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

    /** Accent index: 0 = violet (default), 1..5 = blue/teal/rose/amber/green. */
    public static int accent(Context c) {
        return s(c).getInt("accent", 0);
    }

    public static void setAccent(Context c, int index) {
        s(c).edit().putInt("accent", index).commit();
    }

    /** Speed dial: key '2'..'9' -> number ("" = unset). */
    public static String speedDial(Context c, char key) {
        return s(c).getString("sd_" + key, "");
    }

    public static void setSpeedDial(Context c, char key, String number) {
        SharedPreferences.Editor e = s(c).edit();
        if (number == null || number.length() == 0) {
            e.remove("sd_" + key);
        } else {
            e.putString("sd_" + key, number);
        }
        e.commit();
    }

    /** Private per-number note. */
    public static String note(Context c, String number) {
        return s(c).getString("note_" + com.aurora.phone.util.NumberGeo.groupKey(number), "");
    }

    public static void setNote(Context c, String number, String text) {
        SharedPreferences.Editor e = s(c).edit();
        String key = "note_" + com.aurora.phone.util.NumberGeo.groupKey(number);
        if (text == null || text.trim().length() == 0) {
            e.remove(key);
        } else {
            e.putString(key, text.trim());
        }
        e.commit();
    }

    public static void pushRecentSearch(Context c, String q) {
        if (q == null || q.trim().length() == 0) {
            return;
        }
        q = q.trim();
        java.util.List<String> order = recentSearchOrdered(c);
        order.remove(q);
        order.add(0, q);
        while (order.size() > 8) {
            order.remove(order.size() - 1);
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) {
                joined.append('\u0001');
            }
            joined.append(order.get(i));
        }
        SharedPreferences.Editor e = s(c).edit();
        e.putString("recent_order", joined.toString());
        e.commit();
    }

    public static java.util.List<String> recentSearchOrdered(Context c) {
        String raw = s(c).getString("recent_order", "");
        java.util.List<String> list = new java.util.ArrayList<String>();
        if (raw.length() == 0) {
            return list;
        }
        String[] parts = raw.split("\\u0001", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                list.add(parts[i]);
            }
        }
        return list;
    }

    public static void clearRecentSearches(Context c) {
        SharedPreferences.Editor e = s(c).edit();
        e.remove("recent_searches");
        e.remove("recent_order");
        e.commit();
    }

    /** Timestamp of last-seen missed calls (for the missed-call notification). */
    public static long lastMissedSeen(Context c) {
        return s(c).getLong("last_missed_seen", System.currentTimeMillis());
    }

    public static void setLastMissedSeen(Context c, long millis) {
        s(c).edit().putLong("last_missed_seen", millis).commit();
    }
}
