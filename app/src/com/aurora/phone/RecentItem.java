package com.aurora.phone;

/** Grouped recents row: one entry per number. */
public class RecentItem {
    public String number = "";
    public String name = "";
    public int type = 3;
    public long date = 0L;
    public long duration = 0L;
    public int count = 1;
    public int missed = 0;

    public RecentItem() {
    }

    public RecentItem(CallEntry entry) {
        this.number = entry.number;
        this.name = entry.name;
        this.type = entry.type;
        this.date = entry.date;
        this.duration = entry.duration;
        this.count = 1;
        this.missed = entry.type == 3 ? 1 : 0;
    }
}
