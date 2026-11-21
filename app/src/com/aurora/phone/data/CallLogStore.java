package com.aurora.phone.data;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import com.aurora.phone.CallEntry;
import com.aurora.phone.RecentItem;
import com.aurora.phone.util.NumberGeo;

/** Reads the system call log, or returns null when access is missing. */
public final class CallLogStore {

    private CallLogStore() {
    }

    public static boolean canRead(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** Raw call log, newest first. Null when permission is missing. */
    public static List<CallEntry> loadRaw(Context c) {
        if (!canRead(c)) {
            return null;
        }
        List<CallEntry> list = new ArrayList<CallEntry>();
        String[] projection = new String[] {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };
        Cursor cursor = null;
        try {
            cursor = c.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection,
                    null, null, CallLog.Calls.DATE + " DESC LIMIT 500");
            if (cursor == null) {
                return list;
            }
            int nIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int tIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);
            while (cursor.moveToNext()) {
                CallEntry entry = new CallEntry();
                entry.number = nIdx >= 0 ? cursor.getString(nIdx) : null;
                if (entry.number == null) {
                    entry.number = "";
                }
                entry.name = nameIdx >= 0 ? cursor.getString(nameIdx) : null;
                if (entry.name == null) {
                    entry.name = "";
                }
                entry.type = tIdx >= 0 ? cursor.getInt(tIdx) : 3;
                entry.date = dIdx >= 0 ? cursor.getLong(dIdx) : 0L;
                entry.duration = durIdx >= 0 ? cursor.getLong(durIdx) : 0L;
                if (entry.number.length() > 0) {
                    list.add(entry);
                }
            }
        } catch (SecurityException ex) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    /** Groups raw entries per number (latest on top). Null when unreadable. */
    public static List<RecentItem> loadGrouped(Context c) {
        List<CallEntry> raw = loadRaw(c);
        if (raw == null) {
            return null;
        }
        return group(raw);
    }

    public static List<RecentItem> group(List<CallEntry> raw) {
        LinkedHashMap<String, RecentItem> map = new LinkedHashMap<String, RecentItem>();
        for (int i = 0; i < raw.size(); i++) {
            CallEntry entry = raw.get(i);
            String key = NumberGeo.groupKey(entry.number);
            RecentItem item = map.get(key);
            if (item == null) {
                item = new RecentItem(entry);
                map.put(key, item);
            } else {
                item.count++;
                if (entry.type == 3) {
                    item.missed++;
                }
            }
        }
        List<RecentItem> list = new ArrayList<RecentItem>(map.values());
        Collections.sort(list, new Comparator<RecentItem>() {
            @Override
            public int compare(RecentItem a, RecentItem b) {
                if (a.date > b.date) return -1;
                if (a.date < b.date) return 1;
                return 0;
            }
        });
        return list;
    }
}
