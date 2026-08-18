package com.bijo.bijotouch;

import java.util.Calendar;

/** One-liners for the weekend state, rotating gently day to day. */
final class Motivation {

    private static final String[] LINES = {
            "You earned this. Go recharge. 🔋",
            "Week crushed. Weekend unlocked.",
            "Big plans or big naps? Both count. 😎",
            "Rest is productive too — enjoy it.",
            "Touch some grass. You've done enough. 🌿",
            "Future-you says thanks for this week.",
            "No bells, no rooms. Just vibes.",
    };

    private Motivation() {
    }

    static String line() {
        int i = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % LINES.length;
        return LINES[i];
    }
}
