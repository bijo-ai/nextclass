package com.bijo.bijotouch;

import java.util.Calendar;
import java.util.Locale;

/** A dated thing worth remembering: a CA test, an assignment deadline, etc. */
public final class Event {

    public static final String[] KINDS = {"CA", "Assignment", "Quiz", "Exam", "Other"};

    /** Shown in the course picker for events not tied to a subject. */
    public static final String GENERAL = "General";

    public long id;
    public String title;   // "DSA Unit 2"
    public String kind;    // one of KINDS
    public int year;
    public int month;      // 1..12
    public int dom;        // day of month
    public String note;    // optional
    public String course;  // a course code from the timetable, or GENERAL

    public Event(long id, String title, String kind, int year, int month, int dom,
                 String note, String course) {
        this.id = id;
        this.title = title;
        this.kind = kind;
        this.year = year;
        this.month = month;
        this.dom = dom;
        this.note = note;
        this.course = (course == null || course.isEmpty()) ? GENERAL : course;
    }

    /** Midnight of the event day, for sorting and "days away" maths. */
    public long millis() {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, dom, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Whole days from today (0 = today, 1 = tomorrow, negative = past). */
    public int daysAway() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long diff = millis() - today.getTimeInMillis();
        return (int) Math.round(diff / (24.0 * 60 * 60 * 1000));
    }

    public String dateHuman() {
        String[] mon = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return String.format(Locale.US, "%d %s", dom, mon[Math.max(0, Math.min(11, month - 1))]);
    }

    /** "in 3d", "Tomorrow", "Today", "2d ago". */
    public String awayHuman() {
        int d = daysAway();
        if (d == 0) {
            return "Today";
        }
        if (d == 1) {
            return "Tomorrow";
        }
        if (d > 1) {
            return "in " + d + "d";
        }
        return (-d) + "d ago";
    }
}
