package com.aurora.phone;

import android.content.Context;
import android.content.SharedPreferences;

/** Central preference store. */
public final class Prefs {
    private static final String NAME = "aurora_prefs";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static SharedPreferences sp;

    private Prefs() {
    }

    public static void init(Context c) {
        if (sp == null) {
            sp = c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        }
    }

    private static SharedPreferences s(Context c) {
        init(c);
        return sp;
    }

    public static String getTheme(Context c) {
        return s(c).getString("theme", THEME_SYSTEM);
    }

    public static void setTheme(Context c, String mode) {
        s(c).edit().putString("theme", mode).commit();
    }

    public static boolean blockSpam(Context c) {
        return s(c).getBoolean("block_spam", true);
    }

    public static void setBlockSpam(Context c, boolean v) {
        s(c).edit().putBoolean("block_spam", v).commit();
    }

    public static boolean showRisk(Context c) {
        return s(c).getBoolean("show_risk", true);
    }

    public static void setShowRisk(Context c, boolean v) {
        s(c).edit().putBoolean("show_risk", v).commit();
    }

    public static boolean haptics(Context c) {
        return s(c).getBoolean("haptics", true);
    }

    public static void setHaptics(Context c, boolean v) {
        s(c).edit().putBoolean("haptics", v).commit();
    }
}
