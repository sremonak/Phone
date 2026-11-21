package com.aurora.phone.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.aurora.phone.MainActivity;
import com.aurora.phone.R;

/** Missed-call notifications (framework only, no support library). */
public final class Notifier {

    private static final String CHANNEL_ID = "aurora_missed";

    private Notifier() {
    }

    public static void showMissed(Context c, int count) {
        if (count <= 0) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        ensureChannel(manager);

        Intent intent = new Intent(c, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pending = PendingIntent.getActivity(c, 1, intent, flags);

        String title = c.getString(R.string.missed_notif_title);
        String text = count == 1
                ? c.getString(R.string.missed_notif_text, count)
                : c.getString(R.string.missed_notif_text_plural, count);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(c, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(c);
        }
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_call_missed)
                .setContentTitle(title)
                .setContentText(text)
                .setNumber(count)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
        try {
            manager.notify(42, notification);
        } catch (SecurityException ignored) {
            // notification permission denied on Android13+
        }
    }

    private static void ensureChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Missed calls", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Missed call alerts");
            manager.createNotificationChannel(channel);
        }
    }
}
