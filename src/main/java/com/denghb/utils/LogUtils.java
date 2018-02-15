package com.denghb.utils;

public class LogUtils {


    public static void info(Class clazz, String format, Object... arguments) {
        outLog(clazz, "INFO", format, arguments);
    }

    public static void info(Class clazz, String msg, Throwable t) {
        outLog(clazz, "INFO", msg, t);
    }

    public static void warn(Class clazz, String format, Object... arguments) {
        outLog(clazz, "WARN", format, arguments);
    }

    public static void warn(Class clazz, String msg, Throwable t) {
        outLog(clazz, "WARN", msg, t);
    }

    public static boolean isDebugEnabled() {
        return true;
    }

    public static void debug(Class clazz, String format, Object... arguments) {
        outLog(clazz, "DEBUG", format, arguments);
    }

    public static void debug(Class clazz, String msg, Throwable t) {
        outLog(clazz, "DEBUG", msg, t);
    }

    public static void error(Class clazz, String format, Object... arguments) {
        outLog(clazz, "ERROR", format, arguments);
    }

    public static void error(Class clazz, String msg, Throwable t) {
        outLog(clazz, "ERROR", msg, t);
    }


    private static void outLog(Class clazz, String level, String format, Object... arguments) {
        String log = clazz.getName() + "\t[" + level + "]\t";
        log += format;

        for (Object object : arguments) {
            log = log.replaceFirst("\\{\\}", String.valueOf(object));
        }

        System.out.println(log);
    }

    private static void outLog(Class clazz, String level, String msg, Throwable t) {

        String log = clazz.getName() + "\t[" + level + "]\t";

        System.out.println(log + msg);
        t.printStackTrace();
    }
}
