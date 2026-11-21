package com.aurora.phone;

import java.util.ArrayList;
import java.util.List;

import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.util.RiskAnalyzer;

/** Everything the detail & call screens need to know about one number. */
public class CallerInfo {
    public String number = "";
    public String displayName = "";
    public NumberGeo.Info geo = new NumberGeo.Info();
    public RiskAnalyzer.Risk risk = new RiskAnalyzer.Risk();

    public long firstDate = 0L;
    public long lastDate = 0L;
    public int totalCalls = 0;
    public int missedCalls = 0;
    public long totalDuration = 0L;
    public int shortCalls = 0;

    public boolean blocked = false;
    public boolean inContacts = false;
    public long contactId = -1L;
    public boolean starred = false;

    public List<CallEntry> history = new ArrayList<CallEntry>();

    public boolean hasHistory() {
        return !history.isEmpty();
    }
}
