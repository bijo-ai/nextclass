package com.bijo.bijotouch;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only logic over the {@link Store}: the day's classes, the nearest events,
 * and the "what now / what next" the home screen and widget run on.
 *
 * Two neighbouring slots for the same course + room + type are treated as one
 * session (LPU runs many classes as two back-to-back periods), so the widget
 * never previews the subject you're already sitting in.
 */
public final class Timetable {

    public static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    /**
     * Minutes before a session's end at which the widget stops saying "ongoing"
     * and starts previewing the next class - a heads-up window to pack up and
     * move, regardless of how long the class actually is.
     */
    public static final int LEAD_MINUTES = 15;

    private Timetable() {
    }

    /** Raw slots for a day, sorted, straight from the store (references, editable). */
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

    /** Merge contiguous same-course/room/type slots into single sessions (copies). */
    private static List<Slot> merge(List<Slot> sorted) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : sorted) {
            if (!out.isEmpty()) {
                Slot last = out.get(out.size() - 1);
                if (last.course.equals(s.course) && last.room.equals(s.room)
                        && last.type.equals(s.type) && s.startMin <= last.endMin) {
                    if (s.endMin > last.endMin) {
                        last.endMin = s.endMin;
                    }
                    continue;
                }
            }
            out.add(new Slot(s.id, s.day, s.startMin, s.endMin, s.type, s.group, s.course, s.room));
        }
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
            case Calendar.SATURDAY:  return 6;
            case Calendar.SUNDAY:    return 7;
            default:                 return 1;
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

    public enum State { ONGOING, NEXT, FREE_NOW, DONE_TODAY, WEEKEND, EMPTY }

    public static final class Status {
        public State state = State.EMPTY;
        public Slot slot;            // class to show (ongoing or upcoming); null when EMPTY
        public int nextDay;          // day index when `slot` is on a future day, else 0
        public int progressPct;      // 0..100, ONGOING only
        public int minsToStart = -1; // minutes until `slot` starts today, NEXT/FREE_NOW only
        public String cancelledCourse; // the subject that's off, FREE_NOW only
    }

    public static Status status(Context ctx) {
        Store store = Store.get(ctx);
        Status out = new Status();
        if (store.slots.isEmpty()) {
            out.state = State.EMPTY;
            return out;
        }

        int day = todayIndex();
        int now = nowMinutes();

        if (day != 0) {
            List<Slot> active = new ArrayList<>();
            for (Slot s : forDay(ctx, day)) {
                if (!store.isCancelledToday(s.id)) {
                    active.add(s);
                }
            }
            List<Slot> sessions = merge(active);

            Slot current = null;
            for (Slot s : sessions) {
                if (now >= s.startMin && now < s.endMin) {
                    current = s;
                    break;
                }
            }

            if (current != null) {
                if (now < current.endMin - LEAD_MINUTES) {
                    out.state = State.ONGOING;
                    out.slot = current;
                    out.progressPct = pct(current.startMin, current.endMin, now);
                    return out;
                }
                // final stretch - preview the next session if there is one
                for (Slot s : sessions) {
                    if (s.startMin >= current.endMin) {
                        out.state = State.NEXT;
                        out.slot = s;
                        out.minsToStart = s.startMin - now;
                        return out;
                    }
                }
                // last class of the day, still running: stay ongoing to the bell
                out.state = State.ONGOING;
                out.slot = current;
                out.progressPct = pct(current.startMin, current.endMin, now);
                return out;
            }

            // teacher on leave: now sits inside a class you cancelled today
            Slot cancelledNow = null;
            for (Slot s : forDay(ctx, day)) {
                if (store.isCancelledToday(s.id) && now >= s.startMin && now < s.endMin) {
                    cancelledNow = s;
                    break;
                }
            }
            if (cancelledNow != null) {
                out.state = State.FREE_NOW;
                out.cancelledCourse = cancelledNow.course;
                for (Slot s : sessions) {
                    if (s.startMin > now) {
                        out.slot = s;
                        out.minsToStart = s.startMin - now;
                        return out;
                    }
                }
                fillNextTeachingDay(ctx, out, day + 1); // nothing else today
                return out;
            }

            // between/before classes - next one today?
            for (Slot s : sessions) {
                if (s.startMin > now) {
                    out.state = State.NEXT;
                    out.slot = s;
                    out.minsToStart = s.startMin - now;
                    return out;
                }
            }
        }

        // nothing left today: next class tomorrow (done) or 2+ days off (weekend/break)
        fillNextTeachingDay(ctx, out, day + 1);
        if (out.slot == null) {
            out.state = State.EMPTY;
            return out;
        }
        int gap = ((out.nextDay - day) % 7 + 7) % 7;
        if (gap == 0) {
            gap = 7; // only today has classes - a whole week until the next
        }
        out.state = (gap >= 2) ? State.WEEKEND : State.DONE_TODAY;
        return out;
    }

    private static void fillNextTeachingDay(Context ctx, Status out, int fromDay) {
        for (int i = 0; i < 7; i++) {
            int d = ((fromDay - 1 + i) % 7) + 1;
            List<Slot> raw = forDay(ctx, d);
            if (!raw.isEmpty()) {
                out.slot = merge(raw).get(0);
                out.nextDay = d;
                return;
            }
        }
    }

    private static int pct(int start, int end, int now) {
        if (end <= start) {
            return 0;
        }
        int p = (now - start) * 100 / (end - start);
        return Math.max(0, Math.min(100, p));
    }

    // --------------------------------------------------------- widget timing ---

    /**
     * Minutes-since-midnight of the next moment the widget's answer could change
     * today: a session start, its lead point (end - LEAD), or its end. Returns
     * -1 if nothing is left today.
     */
    public static int nextTransitionToday(Context ctx) {
        int day = todayIndex();
        if (day == 0) {
            return -1;
        }
        Store store = Store.get(ctx);
        int now = nowMinutes();
        List<Slot> active = new ArrayList<>();
        int best = -1;
        // Every raw start/end is a boundary (a cancelled slot's edges flip the
        // free state on and off); active sessions add the lead-time flip point.
        for (Slot s : forDay(ctx, day)) {
            best = earliestAfter(now, best, s.startMin);
            best = earliestAfter(now, best, s.endMin);
            if (!store.isCancelledToday(s.id)) {
                active.add(s);
            }
        }
        for (Slot s : merge(active)) {
            best = earliestAfter(now, best, s.endMin - LEAD_MINUTES);
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
