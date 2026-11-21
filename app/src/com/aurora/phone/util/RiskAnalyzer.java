package com.aurora.phone.util;

/**
 * Offline risk scoring for a number. Purely heuristic — no network,
 * no claims about crowdsourced reports.
 */
public final class RiskAnalyzer {

    public static final int LEVEL_SAFE = 0;
    public static final int LEVEL_WATCH = 1;
    public static final int LEVEL_SPAM = 2;
    public static final int LEVEL_SCAM = 3;

    public static class Risk {
        public int level = LEVEL_SAFE;
        public String title = "Looks safe";
        public String desc = "No issues found in offline analysis.";
    }

    private RiskAnalyzer() {
    }

    /**
     * @param info        geo analysis of the number
     * @param totalCalls  how often this number called you
     * @param shortCalls  how many of those were very short (&lt; 8s)
     * @param blocked     whether the user blocked it
     * @param inContacts  whether the number is saved in contacts
     */
    public static Risk analyze(NumberGeo.Info info, int totalCalls, int shortCalls,
                               boolean blocked, boolean inContacts) {
        Risk risk = new Risk();

        if (blocked) {
            risk.level = LEVEL_SCAM;
            risk.title = "Blocked by you";
            risk.desc = "This number is on your Aurora block list.";
            return risk;
        }
        if (info.isPremium()) {
            risk.level = LEVEL_SPAM;
            risk.title = "Premium-rate number";
            risk.desc = "Calls to this number may incur extra charges. Double-check before dialling.";
            return risk;
        }
        if (info.isShort()) {
            risk.level = LEVEL_SAFE;
            risk.title = "Service short code";
            risk.desc = "Short codes are used by banks, alerts and operators — usually free or standard rate.";
            return risk;
        }
        if (inContacts) {
            risk.level = LEVEL_SAFE;
            risk.title = "Saved in your contacts";
            risk.desc = "You have this number saved, so it is trusted by you.";
            return risk;
        }
        if (totalCalls >= 4 && shortCalls >= 3) {
            risk.level = LEVEL_SPAM;
            risk.title = "Repeated short calls";
            risk.desc = totalCalls + " calls lately, most under 8 seconds — a pattern often left by robocallers.";
            return risk;
        }
        if (info.isTollFree()) {
            risk.level = LEVEL_SAFE;
            risk.title = "Toll-free number";
            risk.desc = "Toll-free numbers are normally free to call and often belong to businesses.";
            return risk;
        }
        if (info.isInternational() && !"US".equals(info.iso) && !"GB".equals(info.iso)) {
            risk.level = LEVEL_WATCH;
            risk.title = "International number";
            risk.desc = "Verify the identity of unexpected international callers before sharing any information.";
            return risk;
        }
        if (totalCalls >= 2 && shortCalls >= 2) {
            risk.level = LEVEL_WATCH;
            risk.title = "Short repeated calls";
            risk.desc = "A few short calls in a row — worth being careful if you do not recognise the number.";
            return risk;
        }
        risk.level = LEVEL_SAFE;
        risk.title = "Looks safe";
        risk.desc = "No issues found in offline analysis for this number.";
        return risk;
    }
}
