package eu.olli.cowlection.util;

import com.google.gson.*;
import com.mojang.util.UUIDTypeAdapter;
import eu.olli.cowlection.data.HyPlayerData;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.UUID;

public final class GsonUtils {
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).registerTypeAdapter(HyPlayerData.class, new HyPlayerDataDeserializer()).create();
    private static final Gson gsonPrettyPrinter = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).setPrettyPrinting().create();

    private GsonUtils() {
    }

    public static <T> T fromJson(String json, Type clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(Reader json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static String toJson(Object object) {
        if (object instanceof NBTBase) {
            return gsonPrettyPrinter.toJson(nbtToJson((NBTBase) object));
        } else {
            return gson.toJson(object);
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
            return new JsonPrimitive(((NBTTagString) nbtElement).getString());
        } else if (nbtElement instanceof NBTTagList) {
            NBTTagList nbtList = (NBTTagList) nbtElement;
            JsonArray jsonArray = new JsonArray();
            for (int tagId = 0; tagId < nbtList.tagCount(); tagId++) {
                jsonArray.add(nbtToJson(nbtList.get(tagId)));
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
}
