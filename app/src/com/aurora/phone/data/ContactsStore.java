package com.aurora.phone.data;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aurora.phone.ContactEntry;
import com.aurora.phone.util.NumberGeo;

/** Reads phone contacts, or returns null when access is missing. */
public final class ContactsStore {

    private ContactsStore() {
    }

    public static boolean canRead(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** Sorted by display name. Null when permission is missing. */
    public static List<ContactEntry> load(Context c) {
        if (!canRead(c)) {
            return null;
        }
        List<ContactEntry> list = new ArrayList<ContactEntry>();
        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.Contacts.STARRED
        };
        Cursor cursor = null;
        try {
            cursor = c.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection, null, null, null);
            if (cursor == null) {
                return list;
            }
            int idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
            int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int cIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int starIdx = cursor.getColumnIndex(ContactsContract.Contacts.STARRED);
            while (cursor.moveToNext()) {
                String name = nameIdx >= 0 ? cursor.getString(nameIdx) : null;
                String number = numIdx >= 0 ? cursor.getString(numIdx) : null;
                if (name == null || name.length() == 0 || number == null || number.length() == 0) {
                    continue;
                }
                ContactEntry entry = new ContactEntry();
                entry.id = cIdx >= 0 ? cursor.getLong(cIdx) : -1L;
                entry.name = name;
                entry.number = number;
                entry.starred = starIdx >= 0 && cursor.getInt(starIdx) == 1;
                list.add(entry);
            }
        } catch (SecurityException ex) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Collections.sort(list, new Comparator<ContactEntry>() {
            @Override
            public int compare(ContactEntry a, ContactEntry b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return list;
    }

    /** Fast lookup by normalized number / group key. */
    public static Map<String, ContactEntry> index(List<ContactEntry> contacts) {
        Map<String, ContactEntry> map = new HashMap<String, ContactEntry>();
        if (contacts == null) {
            return map;
        }
        for (int i = 0; i < contacts.size(); i++) {
            ContactEntry entry = contacts.get(i);
            String key = NumberGeo.groupKey(entry.number);
            if (!map.containsKey(key)) {
                map.put(key, entry);
            }
            String digits = NumberGeo.digits(entry.number);
            if (digits.length() > 0 && !map.containsKey(digits)) {
                map.put(digits, entry);
            }
        }
        return map;
    }
}
