package com.denghb.restful.utils;

import com.denghb.json.JSON;

import java.util.Map;

public class JSONUtils {


    public static String toJson(Object object) {

        return JSON.toJSON(object);
    }

    public static <T> T fromJson(Class<T> clazz, String json) {

        return JSON.parseJSON(clazz, json);
    }


    public static <T> T fromMap(Class<T> clazz, Map map) {
        return JSON.map2Object(clazz, map);
    }


}
