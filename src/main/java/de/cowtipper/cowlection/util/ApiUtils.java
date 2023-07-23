package de.cowtipper.cowlection.util;

import com.google.gson.JsonSyntaxException;
import com.mojang.util.UUIDTypeAdapter;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.chesttracker.data.HyBazaarData;
import de.cowtipper.cowlection.chesttracker.data.HyItemsData;
import de.cowtipper.cowlection.chesttracker.data.LowestBinsCache;
import de.cowtipper.cowlection.command.exception.ThrowingConsumer;
import de.cowtipper.cowlection.config.CredentialStorage;
import de.cowtipper.cowlection.data.Friend;
import de.cowtipper.cowlection.data.HyPlayerData;
import de.cowtipper.cowlection.data.HySkyBlockStats;
import de.cowtipper.cowlection.data.HyStalkingData;
import de.cowtipper.cowlection.error.ApiHttpErrorEvent;
import de.cowtipper.cowlection.error.ApiHttpErrorException;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.apache.http.HttpStatus;
import sun.security.provider.certpath.SunCertPathBuilderException;
import sun.security.validator.ValidatorException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiUtils {
    private static final String NAME_TO_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String ONLINE_STATUS_URL = "https://api.hypixel.net/status?uuid=%s";
    private static final String SKYBLOCK_STATS_URL = "https://api.hypixel.net/skyblock/profiles?uuid=%s";
    private static final String BAZAAR_URL = "https://api.hypixel.net/skyblock/bazaar";
    public static final String LOWEST_BINS = "https://moulberry.codes/lowestbin.json";
    private static final String ITEMS_URL = "https://api.hypixel.net/resources/skyblock/items";
    private static final String PLAYER_URL = "https://api.hypixel.net/player?uuid=%s";
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private ApiUtils() {
    }

    public static void fetchFriendData(String name, ThrowingConsumer<Friend> action) {
        pool.execute(() -> action.accept(getFriend(name)));
    }

    private static Friend getFriend(String name) {
        try (BufferedReader reader = makeApiCall(NAME_TO_UUID_URL + name, false)) {
            if (reader == null) {
                return Friend.FRIEND_NOT_FOUND;
            } else {
                return GsonUtils.fromJson(reader, Friend.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            handleApiException(e);
        }
        return null;
    }

    public static void fetchPlayerStatus(Friend friend, ThrowingConsumer<HyStalkingData> action) {
        pool.execute(() -> action.accept(stalkPlayer(friend)));
    }

    private static HyStalkingData stalkPlayer(Friend friend) {
        try (BufferedReader reader = makeApiCall(String.format(ONLINE_STATUS_URL, UUIDTypeAdapter.fromUUID(friend.getUuid())), true)) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, HyStalkingData.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            handleApiException(e);
        }
        return null;
    }

    public static void fetchSkyBlockStats(Friend friend, ThrowingConsumer<HySkyBlockStats> action) {
        pool.execute(() -> action.accept(stalkSkyBlockStats(friend)));
    }

    private static HySkyBlockStats stalkSkyBlockStats(Friend friend) {
        try (BufferedReader reader = makeApiCall(String.format(SKYBLOCK_STATS_URL, UUIDTypeAdapter.fromUUID(friend.getUuid())), true)) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, HySkyBlockStats.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            handleApiException(e);
        }
        return null;
    }

    public static void fetchBazaarData(ThrowingConsumer<HyBazaarData> action) {
        pool.execute(() -> action.accept(getBazaarData()));
    }

    private static HyBazaarData getBazaarData() {
        try (BufferedReader reader = makeApiCall(BAZAAR_URL, false)) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, HyBazaarData.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            handleApiException(e);
        }
        return null;
    }

    public static void fetchLowestBins(ThrowingConsumer<LowestBinsCache> action) {
        pool.execute(() -> action.accept(getLowestBins()));
    }

    private static LowestBinsCache getLowestBins() {
        try (BufferedReader reader = makeApiCall(LOWEST_BINS, false)) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, LowestBinsCache.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            handleApiException(e);
        }
        return new LowestBinsCache();
    }

    public static void fetchItemsData(ThrowingConsumer<HyItemsData> action) {
        pool.execute(() -> action.accept(getItemsData()));
    }

    private static HyItemsData getItemsData() {
        try (BufferedReader reader = makeApiCall(ITEMS_URL, false)) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, HyItemsData.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            handleApiException(e);
        }
        return null;
    }

    public static void fetchHyPlayerDetails(Friend stalkedPlayer, ThrowingConsumer<HyPlayerData> action) {
        pool.execute(() -> action.accept(stalkHyPlayer(stalkedPlayer)));
    }

    private static HyPlayerData stalkHyPlayer(Friend stalkedPlayer) {
        try (BufferedReader reader = makeApiCall(String.format(PLAYER_URL, UUIDTypeAdapter.fromUUID(stalkedPlayer.getUuid())), true)) {
            if (reader != null) {
                return GsonUtils.fromJson(reader, HyPlayerData.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            handleApiException(e);
        }
        return null;
    }

    private static void handleApiException(Exception e) {
        e.printStackTrace();
        if (e instanceof ApiHttpErrorException) {
            MinecraftForge.EVENT_BUS.post(new ApiHttpErrorEvent(e.getMessage(), ((ApiHttpErrorException) e).getUrl(), ((ApiHttpErrorException) e).wasUsingApiKey()));
        }
    }

    private static BufferedReader makeApiCall(String url, boolean sendApiKey) throws IOException {
        return makeApiCall(url, sendApiKey, CredentialStorage.moo);
    }

    private static BufferedReader makeApiCall(String url, boolean sendApiKey, String key) throws IOException {
        if (sendApiKey && !CredentialStorage.isMooValid) {
            throw new ApiHttpErrorException("Your current Hypixel API key is invalid. Use §4/moo apikey §cto manually set your API key.", url, true);
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (CredentialStorage.sslContext != null && connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(CredentialStorage.sslContext.getSocketFactory());
            }
            if (sendApiKey) {
                connection.addRequestProperty("API-Key", key);
            }
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);
            connection.addRequestProperty("User-Agent", "Forge Mod " + Cowlection.MODNAME + "/" + Cowlection.VERSION + " (" + Cowlection.GITURL + ")");

            connection.getResponseCode();
            if (connection.getResponseCode() == HttpStatus.SC_NO_CONTENT) { // http status 204
                return null;
            } else if (connection.getResponseCode() == HttpStatus.SC_BAD_GATEWAY && url.startsWith("https://api.hypixel.net/")) { // http status 502 (cloudflare)
                throw new ApiHttpErrorException("Couldn't contact Hypixel API (502 Bad Gateway). API might be down, check https://status.hypixel.net for info.", "https://status.hypixel.net", sendApiKey);
            } else if (connection.getResponseCode() == HttpStatus.SC_FORBIDDEN && sendApiKey && url.startsWith("https://api.hypixel.net/")) { // http status 403 Forbidden
                Cowlection.getInstance().getMoo().setMooValidity(false);
                throw new ApiHttpErrorException("Your current Hypixel API key seems to be invalid. Use §4/moo apikey §cto manually set your API key.", url, true);
            } else if (connection.getResponseCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) { // http status 503 Service Unavailable
                throw new ApiHttpErrorException("Couldn't contact the API (503 Service unavailable). API might be down, or you might be blocked by Cloudflare, check if you can reach: " + url, url, sendApiKey);
            } else if (connection.getResponseCode() == HttpStatus.SC_BAD_GATEWAY && url.startsWith("https://moulberry.codes/")) { // http status 502 (cloudflare)
                throw new ApiHttpErrorException("Couldn't contact Moulberry's API (502 Bad Gateway). API might be down, check if " + LOWEST_BINS + " is reachable.", LOWEST_BINS);
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
        } catch (SSLHandshakeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ValidatorException && cause.getCause() instanceof SunCertPathBuilderException) {
                throw new ApiHttpErrorException("" + EnumChatFormatting.DARK_RED + EnumChatFormatting.BOLD + " ! "
                        + EnumChatFormatting.RED + "Java is outdated and doesn't support Let's Encrypt certificates (out of the box). A game restart might fix this issue. If the problem persists, open a ticket on the Cowshed discord server.", Cowlection.INVITE_URL);
            } else {
                // not a newer https related issue, thus rethrow exception:
                throw e;
            }
        }
    }
}
