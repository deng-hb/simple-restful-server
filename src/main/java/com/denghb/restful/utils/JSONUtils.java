package com.denghb.restful.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class JSONUtils {

    private static GsonBuilder gb = new GsonBuilder();
    private static Gson gson = gb.serializeNulls().create();

    public static String toJson(Object object) {

        return gson.toJson(object);
    }

    public static <T> T fromJson(Class<T> clazz, String json) {

        return gson.fromJson(json, clazz);
    }


    public static <T> T fromMap(Class<T> clazz, Map map) {
        String json = toJson(map);
        return fromJson(clazz, json);
    }


}
