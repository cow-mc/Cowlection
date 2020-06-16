package eu.olli.cowmoonication.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.util.UUIDTypeAdapter;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.UUID;

public final class GsonUtils {
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private GsonUtils() {
    }

    public static <T> T fromJson(String json, Type clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(Reader json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }
}
