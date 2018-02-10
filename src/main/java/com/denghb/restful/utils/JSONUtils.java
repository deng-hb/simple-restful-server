package com.denghb.restful.utils;

import com.google.gson.GsonBuilder;

public class JSONUtils {

    private static GsonBuilder gb = new GsonBuilder();

    public static String toJson(Object object) {

        return gb.serializeNulls().create().toJson(object);
    }

    public static <T> T fromJson(Class<T> clazz, String json) {

        return gb.serializeNulls().create().fromJson(json, clazz);
    }
}
