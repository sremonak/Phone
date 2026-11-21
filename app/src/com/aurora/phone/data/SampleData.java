package com.aurora.phone.data;

import android.provider.CallLog;

import java.util.ArrayList;
import java.util.List;

import com.aurora.phone.CallEntry;
import com.aurora.phone.ContactEntry;
import com.aurora.phone.RecentItem;

/**
 * Attractive sample content used when call-log / contacts permissions
 * have not been granted, so the app never looks empty.
 */
public final class SampleData {

    private static final long MIN = 60000L;
    private static final long HOUR = 60L * MIN;
    private static final long DAY = 24L * HOUR;

    private SampleData() {
    }

    public static List<ContactEntry> contacts() {
        List<ContactEntry> list = new ArrayList<ContactEntry>();
        long now = System.currentTimeMillis();
        list.add(new ContactEntry(1, "Rahim Uddin", "01712-345678", true));
        list.add(new ContactEntry(2, "Fatema Akter", "01819-223344", true));
        list.add(new ContactEntry(3, "Tanvir Ahmed", "01911-556677", false));
        list.add(new ContactEntry(4, "Nusrat Jahan", "01613-889900", true));
        list.add(new ContactEntry(5, "Shakib Hasan", "01521-445566", false));
        list.add(new ContactEntry(6, "Mehjabin Chowdhury", "01722-112233", false));
        list.add(new ContactEntry(7, "Arif Rahman", "01312-778899", true));
        list.add(new ContactEntry(8, "Sadia Islam", "01844-334455", false));
        list.add(new ContactEntry(9, "Farhan Kabir", "01988-221100", false));
        list.add(new ContactEntry(10, "Ridoy Hossain", "01677-990011", false));
        list.add(new ContactEntry(11, "Anika Rahman", "01555-667788", true));
        list.add(new ContactEntry(12, "Imran Hossain", "02-9887766", false));
        list.add(new ContactEntry(13, "Zahid Hasan", "031-667788", false));
        list.add(new ContactEntry(14, "Sumaiya Akter", "01733-445599", false));
        list.add(new ContactEntry(15, "John Smith", "+1 415-555-0123", false));
        list.add(new ContactEntry(16, "Emma Wilson", "+44 20 7946 0958", false));
        list.add(new ContactEntry(17, "Ayesha Siddiqua", "01912-334455", false));
        list.add(new ContactEntry(18, "Corporate HR Desk", "02-55661122", false));
        return list;
    }

    public static List<RecentItem> recents() {
        long now = System.currentTimeMillis();
        List<RecentItem> list = new ArrayList<RecentItem>();

        list.add(recent("01712-345678", "Rahim Uddin", CallEntry.TYPE_INCOMING, now - 12 * MIN, 214, 1, 0));
        list.add(recent("01819-223344", "Fatema Akter", CallEntry.TYPE_MISSED, now - 47 * MIN, 0, 2, 2));
        list.add(recent("01911-556677", "Tanvir Ahmed", CallEntry.TYPE_OUTGOING, now - 3 * HOUR, 95, 1, 0));
        list.add(recent("+18005550199", "", CallEntry.TYPE_MISSED, now - 5 * HOUR, 0, 3, 3));
        list.add(recent("01613-889900", "Nusrat Jahan", CallEntry.TYPE_INCOMING, now - 7 * HOUR, 63, 1, 0));
        list.add(recent("16263", "", CallEntry.TYPE_OUTGOING, now - 9 * HOUR, 41, 1, 0));
        list.add(recent("01521-445566", "Shakib Hasan", CallEntry.TYPE_MISSED, now - 1 * DAY - 2 * HOUR, 0, 4, 3));
        list.add(recent("02-9887766", "Imran Hossain", CallEntry.TYPE_OUTGOING, now - 1 * DAY - 6 * HOUR, 320, 1, 0));
        list.add(recent("01722-112233", "Mehjabin Chowdhury", CallEntry.TYPE_INCOMING, now - 1 * DAY - 9 * HOUR, 128, 2, 0));
        list.add(recent("+442079460958", "Emma Wilson", CallEntry.TYPE_OUTGOING, now - 2 * DAY - 3 * HOUR, 240, 1, 0));
        list.add(recent("01844-334455", "Sadia Islam", CallEntry.TYPE_MISSED, now - 2 * DAY - 8 * HOUR, 0, 1, 1));
        list.add(recent("01312-778899", "Arif Rahman", CallEntry.TYPE_INCOMING, now - 3 * DAY - 4 * HOUR, 76, 1, 0));
        list.add(recent("01988-221100", "Farhan Kabir", CallEntry.TYPE_OUTGOING, now - 3 * DAY - 11 * HOUR, 18, 1, 0));
        list.add(recent("031-667788", "Zahid Hasan", CallEntry.TYPE_INCOMING, now - 4 * DAY - 5 * HOUR, 55, 1, 0));
        list.add(recent("+14155550123", "John Smith", CallEntry.TYPE_OUTGOING, now - 5 * DAY - 6 * HOUR, 402, 1, 0));
        list.add(recent("01555-667788", "Anika Rahman", CallEntry.TYPE_MISSED, now - 5 * DAY - 12 * HOUR, 0, 1, 1));
        return list;
    }

    /** History rows shown on the detail screen while in demo mode. */
    public static List<CallEntry> history(String number) {
        List<CallEntry> all = new ArrayList<CallEntry>();
        long now = System.currentTimeMillis();
        List<RecentItem> recents = recents();
        String key = com.aurora.phone.util.NumberGeo.groupKey(number);
        for (int i = 0; i < recents.size(); i++) {
            RecentItem item = recents.get(i);
            if (!com.aurora.phone.util.NumberGeo.groupKey(item.number).equals(key)) {
                continue;
            }
            all.add(new CallEntry(item.number, item.name, item.type, item.date, item.duration));
            // add one older call for flavour
            all.add(new CallEntry(item.number, item.name,
                    item.type == 3 ? 1 : item.type, item.date - 2 * DAY - 3 * HOUR,
                    item.duration > 0 ? item.duration - 11 : 0));
        }
        if (all.isEmpty()) {
            all.add(new CallEntry(number, "", CallEntry.TYPE_MISSED, now - 2 * DAY, 0));
            all.add(new CallEntry(number, "", CallEntry.TYPE_OUTGOING, now - 5 * DAY, 96));
        }
        return all;
    }

    private static RecentItem recent(String number, String name, int type, long date,
                                     long duration, int count, int missed) {
        RecentItem item = new RecentItem();
        item.number = number;
        item.name = name;
        item.type = type;
        item.date = date;
        item.duration = duration;
        item.count = count;
        item.missed = missed;
        return item;
    }
}
