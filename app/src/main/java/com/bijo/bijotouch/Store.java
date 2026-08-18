package com.bijo.bijotouch;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the app knows, persisted as one JSON file in internal storage.
 *
 * A fresh install starts empty: the user brings their own week, either by
 * pasting a share code from a classmate or building it themselves. After that
 * it is fully the user's - add, edit, delete, cancel.
 *
 * Reloads itself when the file changes on disk so the widget's process never
 * shows stale data after an edit made in the activity.
 */
public final class Store {

    private static final String FILE = "nextclass.json";

    private static Store instance;

    private final File file;
    private long loadedStamp = -1;

    public final List<Slot> slots = new ArrayList<>();
    public final List<Event> events = new ArrayList<>();
    /** Keys "slotId|yyyy-mm-dd" for one-off cancellations (teacher absent). */
    public final Set<String> cancellations = new HashSet<>();
    /** False until the user has been through the first-run welcome once. */
    private boolean onboarded = false;
    private long nextId = 1;

    private Store(Context ctx) {
        file = new File(ctx.getApplicationContext().getFilesDir(), FILE);
    }

    public static synchronized Store get(Context ctx) {
        if (instance == null) {
            instance = new Store(ctx);
        }
        instance.reloadIfChanged();
        return instance;
    }

    public long newId() {
        return nextId++;
    }

    // --------------------------------------------------------- onboarding ---

    /** First launch (or a corrupt reset) with nothing to show yet. */
    public boolean needsOnboarding() {
        return !onboarded && slots.isEmpty();
    }

    public void setOnboarded(boolean value) {
        onboarded = value;
        save();
    }

    /** Replace the whole timetable at once (used by share-code import). */
    public void replaceSlots(List<Slot> fresh) {
        slots.clear();
        slots.addAll(fresh);
        onboarded = true;
        save();
    }

    // ----------------------------------------------------------- load/save ---

    private void reloadIfChanged() {
        long stamp = file.exists() ? file.lastModified() : 0;
        if (stamp == loadedStamp && (loadedStamp != -1)) {
            return;
        }
        load();
        loadedStamp = file.exists() ? file.lastModified() : 0;
    }

    private void load() {
        slots.clear();
        events.clear();
        cancellations.clear();
        onboarded = false;
        nextId = 1;

        if (!file.exists()) {
            // Empty, un-onboarded: the welcome screen takes over from here.
            return;
        }

        try {
            JSONObject root = new JSONObject(readFile());
            nextId = root.optLong("nextId", 1);
            onboarded = root.optBoolean("onboarded", false);

            JSONArray s = root.optJSONArray("slots");
            if (s != null) {
                for (int i = 0; i < s.length(); i++) {
                    JSONObject o = s.getJSONObject(i);
                    slots.add(new Slot(
                            o.getLong("id"), o.getInt("day"),
                            o.getInt("start"), o.getInt("end"),
                            o.getString("type"), o.getString("group"),
                            o.getString("course"), o.getString("room")));
                }
            }

            JSONArray e = root.optJSONArray("events");
            if (e != null) {
                for (int i = 0; i < e.length(); i++) {
                    JSONObject o = e.getJSONObject(i);
                    events.add(new Event(
                            o.getLong("id"), o.getString("title"), o.getString("kind"),
                            o.getInt("year"), o.getInt("month"), o.getInt("dom"),
                            o.optString("note", ""), o.optString("course", Event.GENERAL)));
                }
            }

            JSONArray c = root.optJSONArray("cancel");
            if (c != null) {
                for (int i = 0; i < c.length(); i++) {
                    cancellations.add(c.getString(i));
                }
            }
        } catch (Exception ex) {
            // Corrupt file - start clean and let onboarding guide the rebuild.
            slots.clear();
            events.clear();
            cancellations.clear();
            onboarded = false;
        }
    }

    public void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("nextId", nextId);
            root.put("onboarded", onboarded);

            JSONArray s = new JSONArray();
            for (Slot sl : slots) {
                JSONObject o = new JSONObject();
                o.put("id", sl.id);
                o.put("day", sl.day);
                o.put("start", sl.startMin);
                o.put("end", sl.endMin);
                o.put("type", sl.type);
                o.put("group", sl.group);
                o.put("course", sl.course);
                o.put("room", sl.room);
                s.put(o);
            }
            root.put("slots", s);

            JSONArray e = new JSONArray();
            for (Event ev : events) {
                JSONObject o = new JSONObject();
                o.put("id", ev.id);
                o.put("title", ev.title);
                o.put("kind", ev.kind);
                o.put("year", ev.year);
                o.put("month", ev.month);
                o.put("dom", ev.dom);
                o.put("note", ev.note);
                o.put("course", ev.course);
                e.put(o);
            }
            root.put("events", e);

            JSONArray c = new JSONArray();
            for (String key : cancellations) {
                c.put(key);
            }
            root.put("cancel", c);

            writeFile(root.toString());
            loadedStamp = file.lastModified();
        } catch (Exception ignored) {
        }
    }

    private String readFile() throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(file);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        in.close();
        return new String(out.toByteArray(), "UTF-8");
    }

    private void writeFile(String text) throws Exception {
        java.io.FileOutputStream out = new java.io.FileOutputStream(file);
        out.write(text.getBytes("UTF-8"));
        out.close();
    }

    // ----------------------------------------------------------- mutations ---

    public void upsertSlot(Slot s) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).id == s.id) {
                slots.set(i, s);
                save();
                return;
            }
        }
        slots.add(s);
        save();
    }

    public void deleteSlot(long id) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).id == id) {
                slots.remove(i);
                break;
            }
        }
        save();
    }

    public void addEvent(Event e) {
        events.add(e);
        save();
    }

    public void deleteEvent(long id) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id == id) {
                events.remove(i);
                break;
            }
        }
        save();
    }

    // ------------------------------------------------------- cancellations ---

    public static String cancelKey(long slotId, Calendar day) {
        return slotId + "|" + dateKey(day);
    }

    public static String dateKey(Calendar day) {
        return day.get(Calendar.YEAR) + "-"
                + (day.get(Calendar.MONTH) + 1) + "-"
                + day.get(Calendar.DAY_OF_MONTH);
    }

    public boolean isCancelledToday(long slotId) {
        return cancellations.contains(cancelKey(slotId, Calendar.getInstance()));
    }

    public void toggleCancelToday(long slotId) {
        String key = cancelKey(slotId, Calendar.getInstance());
        if (!cancellations.remove(key)) {
            cancellations.add(key);
        }
        save();
    }
}
