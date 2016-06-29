package com.sensirion.libble.log;

import java.util.Locale;

public class Log {
    static final boolean LOG = true;
    static final Locale LOCALE = Locale.CANADA;

    public static void i(String tag, String string) {
        if (LOG) android.util.Log.i(tag, string);
    }

    public static void i(String tag, String string, Object... args) {
        if (LOG) android.util.Log.i(tag, String.format(string, args));
    }

    public static void e(String tag, String string) {
        if (LOG) android.util.Log.e(tag, string);
    }

    public static void e(String tag, String string, Object... args) {
        if (LOG) android.util.Log.e(tag, String.format(string, args));
    }

    public static void v(String tag, String string) {
        if (LOG) android.util.Log.v(tag, string);
    }

    public static void v(String tag, String string, Object... args) {
        if (LOG) android.util.Log.v(tag, String.format(string, args));
    }

    public static void d(String tag, String string) {
        if (LOG) android.util.Log.d(tag, string);
    }

    public static void d(String tag, String string, Object... args) {
        if (LOG) android.util.Log.d(tag, String.format(string, args));
    }

    public static void w(String tag, String string) {
        if (LOG) android.util.Log.w(tag, string);
    }

    public static void w(String tag, String string, Object... args) {
        if (LOG) android.util.Log.w(tag, String.format(string, args));
    }
}