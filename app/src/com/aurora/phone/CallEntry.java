package com.aurora.phone;

/** One call-log record. */
public class CallEntry {
    public long id = 0L;
    /** Mirrors CallLog.Calls type values (present as literals for stub-jar safety). */
    public static final int TYPE_INCOMING = 1;
    public static final int TYPE_OUTGOING = 2;
    public static final int TYPE_MISSED = 3;
    public static final int TYPE_VOICEMAIL = 4;

    public String number = "";
    public String name = "";
    /** Mirrors CallLog.Calls: INCOMING=1, OUTGOING=2, MISSED=3, VOICEMAIL=4. */
    public int type = 3;
    public long date = 0L;
    public long duration = 0L;

    public CallEntry() {
    }

    public CallEntry(String number, String name, int type, long date, long duration) {
        this.number = number;
        this.name = name;
        this.type = type;
        this.date = date;
        this.duration = duration;
    }
}
