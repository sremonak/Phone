package com.aurora.phone.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.aurora.phone.CallEntry;
import com.aurora.phone.data.CallLogStore;

/** Exports the call history as a CSV file (Downloads on API29+). */
public final class Exporter {

    private Exporter() {
    }

    /** Returns the written file location, or null on failure/missing permission. */
    public static String exportCsv(Context c) throws IOException {
        List<CallEntry> raw = CallLogStore.loadRaw(c);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        StringBuilder csv = new StringBuilder();
        csv.append("date,number,name,type,duration_seconds\n");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        for (int i = 0; i < raw.size(); i++) {
            CallEntry e = raw.get(i);
            csv.append(format.format(new Date(e.date))).append(',');
            csv.append(escape(e.number)).append(',');
            csv.append(escape(e.name)).append(',');
            csv.append(typeLabel(e.type)).append(',');
            csv.append(e.duration).append('\n');
        }
        byte[] bytes = csv.toString().getBytes("UTF-8");
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "aurora-calls-" + stamp + ".csv");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS);
            ContentResolver resolver = c.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return null;
            }
            OutputStream out = resolver.openOutputStream(uri);
            if (out == null) {
                return null;
            }
            try {
                out.write(bytes);
            } finally {
                out.close();
            }
            return "Downloads/aurora-calls-" + stamp + ".csv";
        }

        // API26–28: app-specific external dir (no storage permission required)
        java.io.File dir = c.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = c.getFilesDir();
        }
        java.io.File file = new java.io.File(dir, "aurora-calls-" + stamp + ".csv");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        try {
            fos.write(bytes);
        } finally {
            fos.close();
        }
        return file.getAbsolutePath();
    }

    private static String typeLabel(int type) {
        switch (type) {
            case 1: return "incoming";
            case 2: return "outgoing";
            case 3: return "missed";
            case 4: return "voicemail";
            default: return "unknown";
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
