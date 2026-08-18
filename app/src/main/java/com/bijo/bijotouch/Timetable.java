package com.bijo.bijotouch;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only logic over the {@link Store}: the day's classes, the nearest events,
 * and the "now / next" the home screen and widget run on.
 */
public final class Timetable {

    public static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };

    /**
     * Minutes before a class's real end at which it is treated as "over". Kept
     * at 0 so the widget rolls to the next class exactly when the current one
     * ends - not early, and not only when the next class starts.
     */
    public static final int EARLY_FLIP = 0;

    private Timetable() {
    }

    public static List<Slot> forDay(Context ctx, int day) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : Store.get(ctx).slots) {
            if (s.day == day) {
                out.add(s);
            }
        }
        Collections.sort(out, new Comparator<Slot>() {
            @Override
            public int compare(Slot a, Slot b) {
                return Integer.compare(a.startMin, b.startMin);
            }
        });
        return out;
    }

    public static int todayIndex() {
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (dow) {
            case Calendar.MONDAY:    return 1;
            case Calendar.TUESDAY:   return 2;
            case Calendar.WEDNESDAY: return 3;
            case Calendar.THURSDAY:  return 4;
            case Calendar.FRIDAY:    return 5;
            default:                 return 0;
        }
    }

    public static int nowMinutes() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    // --------------------------------------------------------------- events ---

    /** Upcoming (today onward) events, soonest first. */
    public static List<Event> upcomingEvents(Context ctx) {
        List<Event> out = new ArrayList<>();
        for (Event e : Store.get(ctx).events) {
            if (e.daysAway() >= 0) {
                out.add(e);
            }
        }
        Collections.sort(out, new Comparator<Event>() {
            @Override
            public int compare(Event a, Event b) {
                return Long.compare(a.millis(), b.millis());
            }
        });
        return out;
    }

    public static Event nextEvent(Context ctx) {
        List<Event> up = upcomingEvents(ctx);
        return up.isEmpty() ? null : up.get(0);
    }

    // --------------------------------------------------------------- status ---

    public static final class Status {
        public Slot current;   // class on right now (minus the early flip), or null
        public Slot next;      // next class, today or a later day
        public int nextDay;    // day index of `next` when it is on a later day, else 0
        public boolean weekend;
    }

    public static Status status(Context ctx) {
        Store st = Store.get(ctx);
        int day = todayIndex();
        int now = nowMinutes();

        Status out = new Status();
        if (day == 0) {
            out.weekend = true;
            fillNextTeachingDay(ctx, st, out, 1);
            return out;
        }

        for (Slot s : forDay(ctx, day)) {
            if (st.isCancelledToday(s.id)) {
                continue; // teacher absent - invisible to now/next
            }
            int effectiveEnd = s.endMin - EARLY_FLIP;
            if (now >= s.startMin && now < effectiveEnd) {
                out.current = s;
            } else if (now < s.startMin && out.next == null) {
                out.next = s;
            }
        }

        if (out.next == null) {
            fillNextTeachingDay(ctx, st, out, day + 1);
        }
        return out;
    }

    private static void fillNextTeachingDay(Context ctx, Store st, Status out, int fromDay) {
        for (int i = 0; i < 7; i++) {
            int d = ((fromDay - 1 + i) % 7) + 1;
            if (d < 1 || d > 5) {
                continue;
            }
            for (Slot s : forDay(ctx, d)) {
                // A future-day cancellation is rare; a plain first class is fine.
                out.next = s;
                out.nextDay = d;
                return;
            }
        }
    }

    // --------------------------------------------------------- widget timing ---

    /**
     * Minutes-since-midnight of the next moment the widget's answer could
     * change today: any class start, or any class's early-flip point. Used to
     * schedule the next refresh precisely. Returns -1 if nothing is left today.
     */
    public static int nextTransitionToday(Context ctx) {
        int day = todayIndex();
        if (day == 0) {
            return -1;
        }
        int now = nowMinutes();
        int best = -1;
        for (Slot s : forDay(ctx, day)) {
            best = earliestAfter(now, best, s.startMin);
            best = earliestAfter(now, best, s.endMin - EARLY_FLIP);
        }
        return best;
    }

    private static int earliestAfter(int now, int best, int candidate) {
        if (candidate <= now) {
            return best;
        }
        if (best == -1 || candidate < best) {
            return candidate;
        }
        return best;
    }
}
