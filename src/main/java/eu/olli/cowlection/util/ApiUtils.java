package eu.olli.cowlection.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.util.UUIDTypeAdapter;
import eu.olli.cowlection.Cowlection;
import eu.olli.cowlection.command.exception.ThrowingConsumer;
import eu.olli.cowlection.config.MooConfig;
import eu.olli.cowlection.data.Friend;
import eu.olli.cowlection.data.HySkyBlockStats;
import eu.olli.cowlection.data.HyStalkingData;
import eu.olli.cowlection.data.SlothStalkingData;
import org.apache.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiUtils {
    public static final String UUID_NOT_FOUND = "UUID-NOT-FOUND";
    private static final String NAME_TO_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String UUID_TO_NAME_URL = "https://api.mojang.com/user/profiles/%s/names";
    private static final String STALKING_URL_OFFICIAL = "https://api.hypixel.net/status?key=%s&uuid=%s";
    private static final String SKYBLOCK_STATS_URL_OFFICIAL = "https://api.hypixel.net/skyblock/profiles?key=%s&uuid=%s";
    private static final String STALKING_URL_UNOFFICIAL = "https://api.slothpixel.me/api/players/%s";
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private ApiUtils() {
    }

    public static void fetchFriendData(String name, ThrowingConsumer<Friend> action) {
        pool.execute(() -> action.accept(getFriend(name)));
    }

    private static Friend getFriend(String name) {
        try (BufferedReader reader = makeApiCall(NAME_TO_UUID_URL + name)) {
            if (reader == null) {
                return Friend.FRIEND_NOT_FOUND;
            } else {
                return GsonUtils.fromJson(reader, Friend.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void fetchCurrentName(Friend friend, ThrowingConsumer<String> action) {
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

    public static void fetchPlayerStatus(Friend friend, ThrowingConsumer<HyStalkingData> action) {
        pool.execute(() -> action.accept(stalkPlayer(friend)));
    }

    private static HyStalkingData stalkPlayer(Friend friend) {
        try (BufferedReader reader = makeApiCall(String.format(STALKING_URL_OFFICIAL, MooConfig.moo, UUIDTypeAdapter.fromUUID(friend.getUuid())))) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, HyStalkingData.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void fetchSkyBlockStats(Friend friend, ThrowingConsumer<HySkyBlockStats> action) {
        pool.execute(() -> action.accept(stalkSkyBlockStats(friend)));
    }

    private static HySkyBlockStats stalkSkyBlockStats(Friend friend) {
        try (BufferedReader reader = makeApiCall(String.format(SKYBLOCK_STATS_URL_OFFICIAL, MooConfig.moo, UUIDTypeAdapter.fromUUID(friend.getUuid())))) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, HySkyBlockStats.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void fetchPlayerOfflineStatus(Friend stalkedPlayer, ThrowingConsumer<SlothStalkingData> action) {
        pool.execute(() -> action.accept(stalkOfflinePlayer(stalkedPlayer)));
    }

    private static SlothStalkingData stalkOfflinePlayer(Friend stalkedPlayer) {
        try (BufferedReader reader = makeApiCall(String.format(STALKING_URL_UNOFFICIAL, UUIDTypeAdapter.fromUUID(stalkedPlayer.getUuid())))) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, SlothStalkingData.class);
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
        connection.addRequestProperty("User-Agent", "Forge Mod " + Cowlection.MODNAME + "/" + Cowlection.VERSION + " (" + Cowlection.GITURL + ")");

        connection.getResponseCode();
        if (connection.getResponseCode() == HttpStatus.SC_NO_CONTENT) { // http status 204
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
