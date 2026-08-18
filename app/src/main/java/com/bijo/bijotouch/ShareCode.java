package com.bijo.bijotouch;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a timetable into a compact text code that a classmate can paste, and
 * back again. One person in a section builds the week once; everyone else
 * imports it in seconds instead of typing ~30 slots by hand.
 *
 * The code is just the slot list as minified JSON, Base64'd, behind an "NCT1-"
 * tag so a stray paste is easy to recognise and version. Events and one-off
 * cancellations stay personal and are never shared.
 */
final class ShareCode {

    static final String PREFIX = "NCT1-";

    private ShareCode() {
    }

    static String encode(List<Slot> slots) {
        try {
            JSONArray arr = new JSONArray();
            for (Slot s : slots) {
                JSONObject o = new JSONObject();
                o.put("d", s.day);
                o.put("s", s.startMin);
                o.put("e", s.endMin);
                o.put("t", s.type);
                o.put("g", s.group);
                o.put("c", s.course);
                o.put("r", s.room);
                arr.put(o);
            }
            JSONObject root = new JSONObject();
            root.put("v", 1);
            root.put("slots", arr);
            byte[] bytes = root.toString().getBytes("UTF-8");
            return PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Decode a pasted code into fresh slots with new local ids. Throws with a
     * human-readable message if the text isn't a valid NextClass code, so the
     * caller can show it in a dialog.
     */
    static List<Slot> decode(Store store, String raw) throws Exception {
        String code = raw == null ? "" : raw.trim().replaceAll("\\s", "");
        if (!code.startsWith(PREFIX)) {
            throw new Exception("That doesn't look like a NextClass code. It should start with " + PREFIX);
        }
        byte[] bytes;
        try {
            bytes = Base64.decode(code.substring(PREFIX.length()), Base64.NO_WRAP);
        } catch (IllegalArgumentException bad) {
            throw new Exception("This code looks damaged - ask for it to be sent again.");
        }

        JSONObject root = new JSONObject(new String(bytes, "UTF-8"));
        JSONArray arr = root.optJSONArray("slots");
        if (arr == null || arr.length() == 0) {
            throw new Exception("This code has no classes in it.");
        }

        List<Slot> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            out.add(new Slot(
                    store.newId(),
                    o.getInt("d"), o.getInt("s"), o.getInt("e"),
                    o.optString("t", "Lecture"), o.optString("g", "G:All"),
                    o.optString("c", "CLASS"), o.optString("r", "—")));
        }
        return out;
    }
}
