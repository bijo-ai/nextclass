package com.bijo.bijotouch;

import java.util.Locale;

/** Minutes-since-midnight <-> readable time. Keeps AM/PM out of the data. */
final class Ttime {

    private Ttime() {
    }

    /** 560 -> "09:20". No period. */
    static String hhmm(int min) {
        int h24 = min / 60;
        int m = min % 60;
        int h12 = h24 % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        return String.format(Locale.US, "%02d:%02d", h12, m);
    }

    static String period(int min) {
        return (min / 60) < 12 ? "AM" : "PM";
    }

    /** A gap as "1h 5m" / "12m". */
    static String durHuman(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    /**
     * A range the way LPU shows it:
     *   same half of the day -> "09:20 – 10:10 AM"
     *   crossing noon        -> "11:50 AM – 12:40 PM"
     */
    static String range(int start, int end) {
        String sp = period(start);
        String ep = period(end);
        if (sp.equals(ep)) {
            return hhmm(start) + " – " + hhmm(end) + " " + ep;
        }
        return hhmm(start) + " " + sp + " – " + hhmm(end) + " " + ep;
    }
}
