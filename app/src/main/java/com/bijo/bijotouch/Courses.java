package com.bijo.bijotouch;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Course codes drawn from the timetable, and a stable colour for each. */
final class Courses {

    // A calm, distinct palette; a course maps to one of these by its name.
    private static final int[] PALETTE = {
            0xFFD6336C, 0xFF1971C2, 0xFF2F9E44, 0xFF7048E8,
            0xFFE8590C, 0xFF0CA678, 0xFF9C36B5, 0xFF1098AD,
    };

    private Courses() {
    }

    /** Distinct course codes in the timetable, sorted, for the event picker. */
    static List<String> codes(Context ctx) {
        Set<String> set = new LinkedHashSet<>();
        for (Slot s : Store.get(ctx).slots) {
            set.add(s.course);
        }
        List<String> out = new ArrayList<>(set);
        Collections.sort(out);
        return out;
    }

    /** A consistent colour for a course code (grey for the catch-all "General"). */
    static int color(String course) {
        if (course == null || course.equals(Event.GENERAL)) {
            return 0xFF495057;
        }
        int h = 0;
        for (int i = 0; i < course.length(); i++) {
            h = h * 31 + course.charAt(i);
        }
        return PALETTE[Math.abs(h) % PALETTE.length];
    }
}
