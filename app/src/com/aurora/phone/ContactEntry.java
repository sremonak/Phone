package com.aurora.phone;

/** One phone contact. */
public class ContactEntry {
    public long id = -1L;
    public String name = "";
    public String number = "";
    public boolean starred = false;

    public ContactEntry() {
    }

    public ContactEntry(long id, String name, String number, boolean starred) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.starred = starred;
    }
}
