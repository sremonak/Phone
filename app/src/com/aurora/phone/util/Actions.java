package com.aurora.phone.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.Toast;

/** Small intent helpers shared by the screens. */
public final class Actions {

    private Actions() {
    }

    /** Attempts a real phone call. Returns false when it could not start. */
    public static boolean dial(Activity activity, String number) {
        String normalized = NumberGeo.normalize(number);
        if (normalized.length() == 0) {
            return false;
        }
        if (activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + normalized));
            activity.startActivity(intent);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean hasDialPermission(Activity activity) {
        return activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestDialPermission(Activity activity, int requestCode) {
        activity.requestPermissions(new String[] { android.Manifest.permission.CALL_PHONE },
                requestCode);
    }

    public static void sms(Activity activity, String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("smsto:" + NumberGeo.normalize(number)));
            intent.putExtra("sms_body", "");
            activity.startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(activity, "No messaging app found", Toast.LENGTH_SHORT).show();
        }
    }

    public static void addToContacts(Activity activity, String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE,
                    NumberGeo.normalize(number));
            activity.startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(activity, "Could not open contacts", Toast.LENGTH_SHORT).show();
        }
    }

    public static void copy(Activity context, String number) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("number", number));
            Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show();
        }
    }

    public static void share(Activity context, String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(Intent.createChooser(intent, "Share"));
        } catch (Exception ex) {
            Toast.makeText(context, "Nothing to share with", Toast.LENGTH_SHORT).show();
        }
    }
}
