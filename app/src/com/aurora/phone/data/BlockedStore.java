package com.aurora.phone.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import com.aurora.phone.util.NumberGeo;

/**
 * App-level block list (SharedPreferences based).
 * Supports exact numbers and wildcard patterns such as "1900*" or "*1234".
 */
public final class BlockedStore {

    private static final String NAME = "aurora_blocked";
    private static final String PATTERNS_KEY = "patterns";

    private BlockedStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isBlocked(Context c, String number) {
        String key = NumberGeo.groupKey(number);
        if (prefs(c).getBoolean(key, false)) {
            return true;
        }
        for (String pattern : patterns(c)) {
            if (matches(pattern, number)) {
                return true;
            }
        }
        return false;
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

    /** Exact-number entries (ignores the patterns set). */
    public static Set<String> all(Context c) {
        Set<String> keys = new HashSet<String>(prefs(c).getAll().keySet());
        keys.remove(PATTERNS_KEY);
        return keys;
    }

    public static Set<String> patterns(Context c) {
        Set<String> stored = prefs(c).getStringSet(PATTERNS_KEY, null);
        return stored == null ? new HashSet<String>() : new HashSet<String>(stored);
    }

    public static void addPattern(Context c, String pattern) {
        pattern = pattern.trim();
        if (pattern.length() == 0 || !pattern.contains("*")) {
            return;
        }
        Set<String> set = patterns(c);
        if (set.add(pattern)) {
            prefs(c).edit().putStringSet(PATTERNS_KEY, set).commit();
        }
    }

    public static void removePattern(Context c, String pattern) {
        Set<String> set = patterns(c);
        if (set.remove(pattern)) {
            prefs(c).edit().putStringSet(PATTERNS_KEY, set).commit();
        }
    }

    /** Wildcard match: '*' at the end = prefix, at the start = suffix, both = contains. */
    public static boolean matches(String pattern, String number) {
        if (pattern == null || pattern.length() == 0 || number == null) {
            return false;
        }
        if (!pattern.contains("*")) {
            return NumberGeo.groupKey(pattern).equals(NumberGeo.groupKey(number));
        }
        String normalized = NumberGeo.digits(number);
        String[] parts = pattern.split("\\*", -1);
        String prefix = parts.length > 0 ? NumberGeo.digits(parts[0]) : "";
        String suffix = parts.length > 1 ? NumberGeo.digits(parts[parts.length - 1]) : "";
        if (prefix.length() > 0 && !normalized.startsWith(prefix)) {
            return false;
        }
        if (suffix.length() > 0 && !normalized.endsWith(suffix)) {
            return false;
        }
        return true;
    }
}
