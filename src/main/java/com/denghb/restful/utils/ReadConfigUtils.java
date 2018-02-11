package com.denghb.restful.utils;


import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

/**
 * Created by denghb on 2016/12/16.
 */
public class ReadConfigUtils {


    private static String CONFIG_FILE = "slf4j2elk.properties";

    private static Properties properties = new Properties();

    static {
        // Add props from the resource slf4j2elk.properties
        InputStream in = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
            public InputStream run() {
                ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
                if (threadCL != null) {
                    return threadCL.getResourceAsStream(CONFIG_FILE);
                } else {
                    return ClassLoader.getSystemResourceAsStream(CONFIG_FILE);
                }
            }
        });
        if (null != in) {
            try {
                properties.load(in);
            } catch (java.io.IOException e) {
                // ignored
            } finally {
                try {
                    in.close();
                } catch (java.io.IOException e) {
                    // ignored
                }
            }
        }
    }

    /**
     * 获取属性
     *
     * @param name
     * @return
     */
    public static String getValue(String name) {


        String value = null;

        try {
            value = properties.getProperty(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static void main(String... args) {
        System.out.println(getValue("com.denghb.slf4j2elk.level"));
    }

}
