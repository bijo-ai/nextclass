package com.bijo.bijotouch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calendar of the things that matter - CAs, assignments, exams - grouped by course. */
public class EventsActivity extends Activity {

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);

        LinearLayout root = Ui.column(this);
        int pad = Ui.dp(this, 18);
        root.setPadding(pad, Ui.dp(this, 24), pad, Ui.dp(this, 40));
        scroll.addView(root);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.text(this, "Events & Deadlines", 24, Ui.INK, true);
        LinearLayout.LayoutParams tl = Ui.lp(0, Ui.WRAP);
        tl.weight = 1;
        title.setLayoutParams(tl);
        titleRow.addView(title);

        TextView add = Ui.text(this, "+ Add", 14, 0xFF3A2A20, true);
        add.setBackground(Ui.roundedGradient(Ui.ACCENT1, Ui.ACCENT2, 14, this));
        int ap = Ui.dp(this, 16);
        add.setPadding(ap, Ui.dp(this, 10), ap, Ui.dp(this, 10));
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDialog();
            }
        });
        titleRow.addView(add);
        root.addView(titleRow);

        list = Ui.column(this);
        list.setPadding(0, Ui.dp(this, 16), 0, 0);
        root.addView(list);

        setContentView(scroll);
        rebuild();
    }

    private void rebuild() {
        list.removeAllViews();
        NextClassWidget.refresh(this);

        List<Event> up = Timetable.upcomingEvents(this);
        if (up.isEmpty()) {
            TextView empty = Ui.text(this,
                    "Nothing yet. Tap “+ Add” to note a CA, assignment or exam.", 15, Ui.MUTED, false);
            list.addView(empty);
            return;
        }

        // Group by course, soonest-deadline course first (up is date-sorted).
        Map<String, List<Event>> groups = new LinkedHashMap<>();
        for (Event e : up) {
            List<Event> g = groups.get(e.course);
            if (g == null) {
                g = new ArrayList<>();
                groups.put(e.course, g);
            }
            g.add(e);
        }
        for (Map.Entry<String, List<Event>> en : groups.entrySet()) {
            list.addView(groupHeader(en.getKey()));
            for (Event e : en.getValue()) {
                list.addView(eventRow(e));
            }
        }
    }

    private View groupHeader(String course) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rl = Ui.lp(Ui.MATCH, Ui.WRAP);
        rl.topMargin = Ui.dp(this, 18);
        rl.bottomMargin = Ui.dp(this, 8);
        row.setLayoutParams(rl);

        View dot = new View(this);
        dot.setBackground(Ui.rounded(Courses.color(course), 5, this));
        LinearLayout.LayoutParams dl = Ui.lp(Ui.dp(this, 10), Ui.dp(this, 10));
        dl.rightMargin = Ui.dp(this, 8);
        dot.setLayoutParams(dl);
        row.addView(dot);

        TextView t = Ui.text(this, course.toUpperCase(), 13, Ui.INK, true);
        t.setLetterSpacing(0.04f);
        row.addView(t);
        return row;
    }

    private View eventRow(final Event e) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.rounded(Ui.CARD, 16, this));
        int rp = Ui.dp(this, 14);
        row.setPadding(rp, rp, rp, rp);
        LinearLayout.LayoutParams rl = Ui.lp(Ui.MATCH, Ui.WRAP);
        rl.bottomMargin = Ui.dp(this, 10);
        row.setLayoutParams(rl);

        TextView tag = Ui.text(this, e.kind, 11, 0xFFFFFFFF, true);
        tag.setBackground(Ui.rounded(MainActivity.kindColor(e.kind), 8, this));
        int tp = Ui.dp(this, 8);
        tag.setPadding(tp, Ui.dp(this, 5), tp, Ui.dp(this, 5));
        row.addView(tag);

        LinearLayout col = Ui.column(this);
        LinearLayout.LayoutParams cl = Ui.lp(0, Ui.WRAP);
        cl.weight = 1;
        cl.leftMargin = Ui.dp(this, 12);
        col.setLayoutParams(cl);
        col.addView(Ui.text(this, e.title, 16, Ui.INK, true));
        col.addView(Ui.text(this, e.dateHuman() + "  ·  " + e.awayHuman()
                + (e.note.isEmpty() ? "" : "  ·  " + e.note), 12, Ui.MUTED, false));
        row.addView(col);

        TextView del = Ui.text(this, "✕", 18, Ui.MUTED, true);
        int dp = Ui.dp(this, 8);
        del.setPadding(dp, dp, dp, dp);
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Store.get(EventsActivity.this).deleteEvent(e.id);
                rebuild();
            }
        });
        row.addView(del);
        return row;
    }

    private void addDialog() {
        final Calendar picked = Calendar.getInstance();

        LinearLayout form = Ui.column(this);
        int p = Ui.dp(this, 20);
        form.setPadding(p, Ui.dp(this, 8), p, 0);

        form.addView(label("Title"));
        final EditText titleIn = new EditText(this);
        titleIn.setHint("e.g. DSA Unit 2");
        titleIn.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleIn.setSingleLine(true);
        titleIn.setTextColor(Ui.INK);
        form.addView(titleIn);

        // Course: the codes in the timetable, plus a catch-all "General".
        final List<String> cc = Courses.codes(this);
        final String[] courseOpts = new String[cc.size() + 1];
        for (int i = 0; i < cc.size(); i++) {
            courseOpts[i] = cc.get(i);
        }
        courseOpts[cc.size()] = Event.GENERAL;
        final int[] courseIdx = {courseOpts.length == 1 ? 0 : 0}; // first course, or General
        form.addView(label("Course"));
        final TextView coursePick = pickerField(courseOpts[courseIdx[0]]);
        coursePick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(EventsActivity.this)
                        .setItems(courseOpts, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                courseIdx[0] = which;
                                coursePick.setText(courseOpts[which]);
                            }
                        }).show();
            }
        });
        form.addView(coursePick);

        final int[] kindIdx = {0};
        form.addView(label("Type"));
        final TextView kindPick = pickerField(Event.KINDS[0]);
        kindPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(EventsActivity.this)
                        .setItems(Event.KINDS, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                kindIdx[0] = which;
                                kindPick.setText(Event.KINDS[which]);
                            }
                        }).show();
            }
        });
        form.addView(kindPick);

        form.addView(label("Date"));
        final TextView datePick = pickerField(fmt(picked));
        datePick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(EventsActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int y, int mo, int dom) {
                        picked.set(y, mo, dom);
                        datePick.setText(fmt(picked));
                    }
                }, picked.get(Calendar.YEAR), picked.get(Calendar.MONTH),
                        picked.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        form.addView(datePick);

        form.addView(label("Note (optional)"));
        final EditText noteIn = new EditText(this);
        noteIn.setHint("room, syllabus, etc.");
        noteIn.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        noteIn.setSingleLine(true);
        noteIn.setTextColor(Ui.INK);
        form.addView(noteIn);

        ScrollView sv = new ScrollView(this);
        sv.addView(form);

        new AlertDialog.Builder(this)
                .setTitle("Add event")
                .setView(sv)
                .setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        String t = titleIn.getText().toString().trim();
                        if (t.isEmpty()) {
                            t = Event.KINDS[kindIdx[0]];
                        }
                        Store store = Store.get(EventsActivity.this);
                        store.addEvent(new Event(store.newId(), t, Event.KINDS[kindIdx[0]],
                                picked.get(Calendar.YEAR), picked.get(Calendar.MONTH) + 1,
                                picked.get(Calendar.DAY_OF_MONTH),
                                noteIn.getText().toString().trim(), courseOpts[courseIdx[0]]));
                        rebuild();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String fmt(Calendar c) {
        String[] mon = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return c.get(Calendar.DAY_OF_MONTH) + " " + mon[c.get(Calendar.MONTH)]
                + " " + c.get(Calendar.YEAR);
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 12, Ui.MUTED, true);
        t.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4));
        return t;
    }

    private TextView pickerField(String value) {
        TextView t = Ui.text(this, value, 16, Ui.INK, false);
        t.setBackground(Ui.rounded(0xFFEFEFF1, 12, this));
        int p = Ui.dp(this, 12);
        t.setPadding(p, p, p, p);
        t.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        return t;
    }
}
