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
    private static final String STALKING_URL = "https://api.hypixel.net/status?key=%s&uuid=%s";
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).registerTypeAdapter(Friend.class, new Friend.FriendCreator()).create();

    private ApiUtils() {
    }

    public static void fetchFriendData(String name, Consumer<Friend> action) {
        pool.execute(() -> action.accept(getFriend(name)));
    }

    private static Friend getFriend(String name) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(NAME_TO_UUID_URL + name).openConnection();
            connection.setReadTimeout(5000);
            if (connection.getResponseCode() == 204) {
                return Friend.FRIEND_NOT_FOUND;
            } else if (connection.getResponseCode() == 200) {
                return gson.fromJson(new BufferedReader(new InputStreamReader(connection.getInputStream())), Friend.class);
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
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(UUID_TO_NAME_URL, UUIDTypeAdapter.fromUUID(friend.getUuid()))).openConnection();
            connection.setReadTimeout(5000);
            if (connection.getResponseCode() == 204) {
                return UUID_NOT_FOUND;
            } else if (connection.getResponseCode() == 200) {
                JsonArray nameHistoryData = new JsonParser().parse(new BufferedReader(new InputStreamReader(connection.getInputStream()))).getAsJsonArray();
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
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(STALKING_URL, MooConfig.moo, UUIDTypeAdapter.fromUUID(friend.getUuid()))).openConnection();
            connection.setReadTimeout(5000);
            connection.addRequestProperty("User-Agent", "Forge Mod " + Cowmoonication.MODNAME + "/" + Cowmoonication.VERSION + " (https://github.com/cow-mc/Cowmoonication/)");

            connection.getResponseCode();
            if (connection.getResponseCode() == 204) {
                return null;
            } else { // various possible http status code: 200, 403, 422
                BufferedReader reader;
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    reader = new BufferedReader(new InputStreamReader(errorStream));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                }

                return gson.fromJson(reader, HyStalking.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
