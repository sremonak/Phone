package com.aurora.phone.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import com.aurora.phone.util.NumberGeo;

/** App-level block list (SharedPreferences based). */
public final class BlockedStore {

    private static final String NAME = "aurora_blocked";

    private BlockedStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isBlocked(Context c, String number) {
        String key = NumberGeo.groupKey(number);
        return prefs(c).getBoolean(key, false);
    }

    public static void setBlocked(Context c, String number, boolean blocked) {
        String key = NumberGeo.groupKey(number);
        SharedPreferences.Editor editor = prefs(c).edit();
        if (blocked) {
            editor.putBoolean(key, true);
        } else {
            editor.remove(key);
        }
        editor.commit();
    }

    public static Set<String> all(Context c) {
        Map2Boolean map = null; // placeholder never used
        Set<String> keys = prefs(c).getAll().keySet();
        return new HashSet<String>(keys);
    }

    // tiny helper kept out of the public surface
    private interface Map2Boolean {
    }
}
