package com.aurora.phone.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Time formatting helpers. */
public final class TimeUtils {
    private TimeUtils() {
    }

    /** Compact relative label: "Just now", "5m ago", "Yesterday", "Mon", "4 Mar". */
    public static String relative(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;
        if (diff < 60000L) {
            return "Just now";
        }
        if (diff < 3600000L) {
            return (diff / 60000L) + "m ago";
        }
        Date d = new Date(time);
        Date today = new Date(now);
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String todayKey = dayFmt.format(today);
        String dayKey = dayFmt.format(d);
        if (todayKey.equals(dayKey)) {
            return (diff / 3600000L) + "h ago";
        }
        long oneDay = 86400000L;
        Date yesterday = new Date(now - oneDay);
        if (dayFmt.format(yesterday).equals(dayKey)) {
            return "Yesterday";
        }
        if (diff < 6L * oneDay) {
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(d);
        }
        if (d.getYear() == today.getYear()) {
            return new SimpleDateFormat("d MMM", Locale.getDefault()).format(d);
        }
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(d);
    }

    /** Full date+time label for detail rows. */
    public static String full(long time) {
        Date d = new Date(time);
        long diff = System.currentTimeMillis() - time;
        if (diff < 86400000L) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(d);
        }
        if (diff < 6L * 86400000L) {
            return new SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(d);
        }
        return new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(d);
    }

    /** Duration label: "45s", "2m 30s", "1h 5m". */
    public static String duration(long seconds) {
        if (seconds <= 0) {
            return "—";
        }
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        if (h > 0) {
            return h + "h " + m + "m";
        }
        if (m > 0) {
            if (s > 0) {
                return m + "m " + s + "s";
            }
            return m + "m";
        }
        return s + "s";
    }

    /** Timer label for the call screen: mm:ss or h:mm:ss. */
    public static String timer(long seconds) {
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        String mm = (m < 10 ? "0" : "") + m;
        String ss = (s < 10 ? "0" : "") + s;
        if (h > 0) {
            return h + ":" + mm + ":" + ss;
        }
        return mm + ":" + ss;
    }
}
