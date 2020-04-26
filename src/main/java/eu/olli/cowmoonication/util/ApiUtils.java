package eu.olli.cowmoonication.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.util.UUIDTypeAdapter;
import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.friends.Friend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ApiUtils {
    public static final String UUID_NOT_FOUND = "UUID-NOT-FOUND";
    private static final String NAME_TO_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String UUID_TO_NAME_URL = "https://api.mojang.com/user/profiles/%s/names";
    private static final String STALKING_URL_OFFICIAL = "https://api.hypixel.net/status?key=%s&uuid=%s";
    private static final String STALKING_URL_UNOFFICIAL = "https://api.slothpixel.me/api/players/%s";
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).registerTypeAdapter(Friend.class, new Friend.FriendCreator()).create();

    private ApiUtils() {
    }

    public static void fetchFriendData(String name, Consumer<Friend> action) {
        pool.execute(() -> action.accept(getFriend(name)));
    }

    private static Friend getFriend(String name) {
        try (BufferedReader reader = makeApiCall(NAME_TO_UUID_URL + name)) {
            if (reader == null) {
                return Friend.FRIEND_NOT_FOUND;
            } else {
                return gson.fromJson(reader, Friend.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void fetchCurrentName(Friend friend, Consumer<String> action) {
        pool.execute(() -> action.accept(getCurrentName(friend)));
    }

    private static String getCurrentName(Friend friend) {
        try (BufferedReader reader = makeApiCall(String.format(UUID_TO_NAME_URL, UUIDTypeAdapter.fromUUID(friend.getUuid())))) {
            if (reader == null) {
                return UUID_NOT_FOUND;
            } else {
                JsonArray nameHistoryData = new JsonParser().parse(reader).getAsJsonArray();
                if (nameHistoryData.size() > 0) {
                    return nameHistoryData.get(nameHistoryData.size() - 1).getAsJsonObject().get("name").getAsString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void fetchPlayerStatus(Friend friend, Consumer<HyStalking> action) {
        pool.execute(() -> action.accept(stalkPlayer(friend)));
    }

    private static HyStalking stalkPlayer(Friend friend) {
        try (BufferedReader reader = makeApiCall(String.format(STALKING_URL_OFFICIAL, MooConfig.moo, UUIDTypeAdapter.fromUUID(friend.getUuid())))) {
            if (reader != null) {
                return gson.fromJson(reader, HyStalking.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void fetchPlayerOfflineStatus(Friend stalkedPlayer, Consumer<SlothStalking> action) {
        pool.execute(() -> action.accept(stalkOfflinePlayer(stalkedPlayer)));
    }

    private static SlothStalking stalkOfflinePlayer(Friend stalkedPlayer) {
        try (BufferedReader reader = makeApiCall(String.format(STALKING_URL_UNOFFICIAL, UUIDTypeAdapter.fromUUID(stalkedPlayer.getUuid())))) {
            if (reader != null) {
                return gson.fromJson(reader, SlothStalking.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static BufferedReader makeApiCall(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.addRequestProperty("User-Agent", "Forge Mod " + Cowmoonication.MODNAME + "/" + Cowmoonication.VERSION + " (https://github.com/cow-mc/Cowmoonication/)");

        connection.getResponseCode();
        if (connection.getResponseCode() == 204) {
            return null;
        } else {
            BufferedReader reader;
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                reader = new BufferedReader(new InputStreamReader(errorStream));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }
            return reader;
        }
    }
}
