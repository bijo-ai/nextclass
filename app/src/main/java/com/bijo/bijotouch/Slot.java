package com.bijo.bijotouch;

/**
 * One recurring class. Mutable now that classes can be edited, and carries an
 * id so an edit or a cancellation can name it.
 */
public final class Slot {

    public long id;
    public int day;        // 1 = Monday .. 5 = Friday
    public int startMin;   // 09:20 -> 560
    public int endMin;     // 10:10 -> 610
    public String type;    // Lecture / Practical / Tutorial
    public String group;   // G:1, G:0, G:All
    public String course;  // CSE202
    public String room;    // 37-901

    public Slot(long id, int day, int startMin, int endMin,
                String type, String group, String course, String room) {
        this.id = id;
        this.day = day;
        this.startMin = startMin;
        this.endMin = endMin;
        this.type = type;
        this.group = group;
        this.course = course;
        this.room = room;
    }

    public String time() {
        return Ttime.range(startMin, endMin);
    }

    /** "37-901" -> "Building 37 · Room 901". Leaves anything odd untouched. */
    public String roomHuman() {
        int dash = room.indexOf('-');
        if (dash <= 0 || dash == room.length() - 1) {
            return "Room " + room;
        }
        return "Building " + room.substring(0, dash) + " · Room " + room.substring(dash + 1);
    }
}
