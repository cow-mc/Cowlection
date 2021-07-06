package de.cowtipper.cowlection.util;

import com.google.gson.*;
import com.mojang.util.UUIDTypeAdapter;
import de.cowtipper.cowlection.chesttracker.LowestBinsCache;
import de.cowtipper.cowlection.data.HyPlayerData;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.codec.binary.Base64;

import java.io.Reader;
import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class GsonUtils {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeAdapter(HyPlayerData.class, new HyPlayerDataDeserializer())
            .registerTypeAdapter(LowestBinsCache.class, new LowestBinsDeserializer())
            .create();
    private static final Gson gsonPrettyPrinter = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .setPrettyPrinting().create();

    private GsonUtils() {
    }

    public static <T> T fromJson(String json, Type clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(Reader json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static String toJson(Object object) {
        return toJson(object, false);
    }

    public static String toJson(Object object, boolean sort) {
        if (object instanceof NBTBase) {
            JsonElement jsonElement = nbtToJson((NBTBase) object);
            if (sort && (jsonElement instanceof JsonObject || jsonElement instanceof JsonArray)) {
                jsonElement = sortJsonElement(jsonElement);
            }
            return gsonPrettyPrinter.toJson(jsonElement);
        } else {
            return gson.toJson(object);
        }
    }

    private static JsonElement sortJsonElement(JsonElement jsonElement) {
        if (jsonElement instanceof JsonArray) {
            // sort each element of array
            JsonArray sortedJsonArray = new JsonArray();
            for (JsonElement arrayElement : (JsonArray) jsonElement) {
                sortedJsonArray.add(sortJsonElement(arrayElement));
            }
            return sortedJsonArray;
        } else if (jsonElement instanceof JsonObject) {
            // sort json by key
            TreeMap<String, JsonElement> sortedJsonObject = new TreeMap<>(String::compareToIgnoreCase);
            for (Map.Entry<String, JsonElement> jsonEntry : ((JsonObject) jsonElement).entrySet()) {
                JsonElement sortedJsonElement = jsonEntry.getValue();
                if (sortedJsonElement instanceof JsonObject) {
                    sortedJsonElement = sortJsonElement(sortedJsonElement);
                }
                sortedJsonObject.put(jsonEntry.getKey(), sortedJsonElement);
            }
            // overwrite jsonElement with json sorted by key alphabetically
            JsonObject sortedJsonElement = new JsonObject();
            for (Map.Entry<String, JsonElement> jsonEntrySorted : sortedJsonObject.entrySet()) {
                sortedJsonElement.add(jsonEntrySorted.getKey(), jsonEntrySorted.getValue());
            }
            return sortedJsonElement;
        } else {
            // neither array, nor object: return original element
            return jsonElement;
        }
    }

    private static JsonElement nbtToJson(NBTBase nbtElement) {
        if (nbtElement instanceof NBTBase.NBTPrimitive) {
            NBTBase.NBTPrimitive nbtNumber = (NBTBase.NBTPrimitive) nbtElement;
            switch (nbtNumber.getId()) {
                case Constants.NBT.TAG_BYTE:
                    return new JsonPrimitive(nbtNumber.getByte());
                case Constants.NBT.TAG_SHORT:
                    return new JsonPrimitive(nbtNumber.getShort());
                case Constants.NBT.TAG_INT:
                    return new JsonPrimitive(nbtNumber.getInt());
                case Constants.NBT.TAG_LONG:
                    return new JsonPrimitive(nbtNumber.getLong());
                case Constants.NBT.TAG_FLOAT:
                    return new JsonPrimitive(nbtNumber.getFloat());
                case Constants.NBT.TAG_DOUBLE:
                    return new JsonPrimitive(nbtNumber.getDouble());
                default:
                    return new JsonObject();
            }
        } else if (nbtElement instanceof NBTTagString) {
            String str = ((NBTTagString) nbtElement).getString();
            if (str.length() > 100 && (str.startsWith("eyJ") || str.startsWith("ewo")) && Base64.isBase64(str)) {
                // base64 decode NBTTagStrings starting with {" or {\n
                try {
                    JsonElement base64DecodedJson = new JsonParser().parse(new String(Base64.decodeBase64(str)));
                    if (base64DecodedJson.isJsonObject()) {
                        JsonObject jsonObject = base64DecodedJson.getAsJsonObject();
                        JsonElement timestamp = jsonObject.get("timestamp");
                        if (timestamp != null) {
                            // convert unix timestamp to human-readable dates
                            try {
                                ZonedDateTime utcDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getAsLong()), ZoneOffset.UTC);
                                ZonedDateTime localDateTime = utcDateTime.withZoneSameInstant(ZoneOffset.systemDefault());
                                String zoneOffset = localDateTime.getOffset().toString();

                                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss O");
                                jsonObject.add("timeInUTC", new JsonPrimitive(utcDateTime.format(dateTimeFormatter)));
                                jsonObject.add("timeInLocalZone (UTC" + ("Z".equals(zoneOffset) ? "" : zoneOffset) + ")", new JsonPrimitive(localDateTime.format(dateTimeFormatter)));
                            } catch (DateTimeException | NumberFormatException ignored) {
                            }
                        }
                    }
                    return base64DecodedJson;
                } catch (JsonParseException ignored) {
                    // failed to parse as json; leaving original string unmodified
                }
            }
            return new JsonPrimitive(str);
        } else if (nbtElement instanceof NBTTagList) {
            NBTTagList nbtList = (NBTTagList) nbtElement;
            JsonArray jsonArray = new JsonArray();
            for (int tagId = 0; tagId < nbtList.tagCount(); tagId++) {
                jsonArray.add(nbtToJson(nbtList.get(tagId)));
            }
            return jsonArray;
        } else if (nbtElement instanceof NBTTagIntArray) {
            int[] intArray = ((NBTTagIntArray) nbtElement).getIntArray();
            JsonArray jsonArray = new JsonArray();
            for (int number : intArray) {
                jsonArray.add(new JsonPrimitive(number));
            }
            return jsonArray;
        } else if (nbtElement instanceof NBTTagCompound) {
            NBTTagCompound nbtCompound = (NBTTagCompound) nbtElement;
            JsonObject jsonObject = new JsonObject();
            for (String nbtEntry : nbtCompound.getKeySet()) {
                jsonObject.add(nbtEntry, nbtToJson(nbtCompound.getTag(nbtEntry)));
            }
            return jsonObject;
        }
        return new JsonObject();
    }

    public static class HyPlayerDataDeserializer implements JsonDeserializer<HyPlayerData> {
        @Override
        public HyPlayerData deserialize(JsonElement json, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            if (!json.getAsJsonObject().get("success").getAsBoolean()) {
                // status: failed
                return null;
            }
            JsonElement player = json.getAsJsonObject().get("player");
            HyPlayerData hyPlayerData = gsonPrettyPrinter.fromJson(player, HyPlayerData.class);
            if (hyPlayerData == null) {
                // player hasn't played Hypixel before
                return new HyPlayerData();
            }
            return hyPlayerData;
        }
    }

    public static class LowestBinsDeserializer implements JsonDeserializer<LowestBinsCache> {
        @Override
        public LowestBinsCache deserialize(JsonElement json, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            LowestBinsCache lowestBinsCache = new LowestBinsCache();
            if (!json.isJsonObject()) {
                // invalid JSON
                return lowestBinsCache;
            }
            JsonObject lowestBins = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : lowestBins.entrySet()) {
                try {
                    lowestBinsCache.put(entry.getKey(), entry.getValue().getAsInt());
                } catch (ClassCastException | NumberFormatException ignored) {
                    // somehow not an integer
                }
            }
            return lowestBinsCache;
        }
    }
}
