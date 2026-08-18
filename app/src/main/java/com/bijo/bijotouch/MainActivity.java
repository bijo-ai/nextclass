package com.bijo.bijotouch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.content.res.ColorStateList;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.List;

/**
 * Home: now/next hero, upcoming events, editable day-by-day timetable.
 */
public class MainActivity extends Activity {

    private LinearLayout heroBox;
    private LinearLayout eventsBox;
    private LinearLayout dayRow;
    private LinearLayout dayCards;
    private int selectedDay;
    private boolean onboarding;

    private final String[] TYPES = {"Lecture", "Practical", "Tutorial"};
    private final String[] GROUPS = {"G:All", "G:1", "G:0", "G:2", "G:3"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Store.get(this).needsOnboarding()) {
            showOnboarding();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);

        LinearLayout root = Ui.column(this);
        int pad = Ui.dp(this, 18);
        root.setPadding(pad, Ui.dp(this, 24), pad, Ui.dp(this, 40));
        scroll.addView(root);

        // ---- title row ----
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = Ui.column(this);
        LinearLayout.LayoutParams tcl = Ui.lp(0, Ui.WRAP);
        tcl.weight = 1;
        titleCol.setLayoutParams(tcl);
        titleCol.addView(Ui.text(this, "NextClass", 28, Ui.INK, true));
        titleCol.addView(Ui.text(this, "your week", 13, Ui.MUTED, false));
        titleRow.addView(titleCol);

        titleRow.addView(pillButton("Events", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EventsActivity.class));
            }
        }));
        titleRow.addView(menuButton());
        root.addView(titleRow);

        // ---- hero ----
        heroBox = Ui.column(this);
        LinearLayout.LayoutParams hl = Ui.lp(Ui.MATCH, Ui.WRAP);
        hl.topMargin = Ui.dp(this, 16);
        heroBox.setLayoutParams(hl);
        root.addView(heroBox);

        // ---- events strip ----
        eventsBox = Ui.column(this);
        root.addView(eventsBox);

        // ---- timetable header + add ----
        LinearLayout ttRow = new LinearLayout(this);
        ttRow.setOrientation(LinearLayout.HORIZONTAL);
        ttRow.setGravity(Gravity.CENTER_VERTICAL);
        ttRow.setPadding(0, Ui.dp(this, 26), 0, Ui.dp(this, 12));
        TextView tt = Ui.text(this, "Timetable", 20, Ui.INK, true);
        LinearLayout.LayoutParams ttl = Ui.lp(0, Ui.WRAP);
        ttl.weight = 1;
        tt.setLayoutParams(ttl);
        ttRow.addView(tt);
        ttRow.addView(pillButton("+ Add class", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editSlotDialog(null);
            }
        }));
        root.addView(ttRow);

        // ---- day tabs ----
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        dayRow = new LinearLayout(this);
        dayRow.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(dayRow);
        root.addView(tabScroll);

        dayCards = Ui.column(this);
        dayCards.setPadding(0, Ui.dp(this, 14), 0, 0);
        root.addView(dayCards);

        int today = Timetable.todayIndex();
        selectedDay = (today == 0) ? 1 : today;

        setContentView(scroll);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (onboarding) {
            // If they imported a code from the welcome screen elsewhere, catch up.
            if (!Store.get(this).needsOnboarding()) {
                recreate();
            }
            return;
        }
        // Coming back from the Events screen (or after time passes) - re-read.
        refresh();
    }

    private void refresh() {
        buildHero();
        buildEvents();
        buildTabs();
        showDay(selectedDay);
        // Keep the widget in step with edits made here.
        NextClassWidget.refresh(this);
    }

    // ---------------------------------------------------------------- hero ---

    private void buildHero() {
        heroBox.removeAllViews();
        Timetable.Status st = Timetable.status(this);

        int[] g = heroGradient(st.state);
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.roundedGradient(g[0], g[1], 22, this));
        int p = Ui.dp(this, 20);
        card.setPadding(p, p, p, p);
        card.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));

        switch (st.state) {
            case EMPTY:
                card.addView(kicker("NO CLASSES YET"));
                card.addView(headline("Add your week"));
                card.addView(sub("Tap “+ Add class”, or ≡ to paste a share code."));
                heroBox.addView(card);
                return;
            case WEEKEND:
                card.addView(kicker("WEEKEND"));
                card.addView(headline("Weekend plans?? 🎉"));
                card.addView(sub(Motivation.line()));
                heroBox.addView(card);
                return;
            case FREE_NOW:
                card.addView(kicker("TEACHER ON LEAVE"));
                card.addView(headline("Free period 🎉"));
                card.addView(sub(st.cancelledCourse + " is cancelled today"));
                String when;
                if (st.slot != null && st.nextDay == 0 && st.minsToStart > 0) {
                    when = "Free for " + Ttime.durHuman(st.minsToStart)
                            + "  ·  next " + st.slot.room + " (" + st.slot.course + ")";
                } else if (st.slot != null) {
                    when = "Next class " + Timetable.DAY_NAMES[st.nextDay - 1];
                } else {
                    when = "No more classes today 🎉";
                }
                TextView w = sub(when);
                w.setPadding(0, Ui.dp(this, 10), 0, 0);
                card.addView(w);
                heroBox.addView(card);
                return;
            case DONE_TODAY:
                card.addView(kicker("DONE FOR TODAY"));
                card.addView(headline("That’s a wrap ✌️"));
                card.addView(sub("See you " + Timetable.DAY_NAMES[st.nextDay - 1]
                        + " · first up " + st.slot.room + " at "
                        + Ttime.hhmm(st.slot.startMin) + " " + Ttime.period(st.slot.startMin)));
                heroBox.addView(card);
                return;
            default:
                break; // ONGOING / NEXT below
        }

        Slot show = st.slot;
        String kick = (st.state == Timetable.State.ONGOING)
                ? "● ONGOING"
                : "NEXT UP" + ((st.minsToStart > 0 && st.minsToStart <= 600)
                        ? "  ·  in " + Ttime.durHuman(st.minsToStart) : "");
        card.addView(kicker(kick));

        TextView room = Ui.text(this, show.room, 40, 0xFFFFFFFF, true);
        room.setPadding(0, Ui.dp(this, 6), 0, 0);
        card.addView(room);
        card.addView(sub(show.roomHuman()));

        TextView line = Ui.text(this,
                show.course + "  ·  " + show.type + "  ·  " + show.group, 15, 0xFFFFFFFF, true);
        line.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(line);
        card.addView(sub(show.time()));

        if (st.state == Timetable.State.ONGOING) {
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(st.progressPct);
            bar.setProgressTintList(ColorStateList.valueOf(0xFFFFFFFF));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x55FFFFFF));
            LinearLayout.LayoutParams blp = Ui.lp(Ui.MATCH, Ui.dp(this, 6));
            blp.topMargin = Ui.dp(this, 14);
            bar.setLayoutParams(blp);
            card.addView(bar);
        }

        heroBox.addView(card);
    }

    private int[] heroGradient(Timetable.State state) {
        switch (state) {
            case ONGOING:    return new int[]{0xFF17B26A, 0xFF067647};
            case FREE_NOW:   return new int[]{0xFF2DD4BF, 0xFF0891B2};
            case DONE_TODAY: return new int[]{0xFF3B82F6, 0xFF6366F1};
            case WEEKEND:    return new int[]{0xFF7C3AED, 0xFFDB2777};
            case EMPTY:      return new int[]{0xFF6B7280, 0xFF4B5563};
            default:         return new int[]{Ui.ACCENT1, Ui.ACCENT2};
        }
    }

    private TextView kicker(String s) {
        TextView k = Ui.text(this, s, 12, 0xFFFFF3EC, true);
        k.setLetterSpacing(0.08f);
        return k;
    }

    private TextView headline(String s) {
        TextView t = Ui.text(this, s, 26, 0xFFFFFFFF, true);
        t.setPadding(0, Ui.dp(this, 6), 0, 0);
        return t;
    }

    private TextView sub(String s) {
        return Ui.text(this, s, 14, 0xFFFFF3EC, false);
    }

    // -------------------------------------------------------------- events ---

    private void buildEvents() {
        eventsBox.removeAllViews();
        List<Event> up = Timetable.upcomingEvents(this);
        if (up.isEmpty()) {
            return;
        }

        LinearLayout.LayoutParams boxLp = Ui.lp(Ui.MATCH, Ui.WRAP);
        boxLp.topMargin = Ui.dp(this, 12);
        eventsBox.setLayoutParams(boxLp);

        int shown = Math.min(3, up.size());
        for (int i = 0; i < shown; i++) {
            final Event e = up.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.rounded(Ui.CARD, 14, this));
            int rp = Ui.dp(this, 12);
            row.setPadding(rp, rp, rp, rp);
            LinearLayout.LayoutParams rl = Ui.lp(Ui.MATCH, Ui.WRAP);
            rl.topMargin = Ui.dp(this, 8);
            row.setLayoutParams(rl);

            TextView tag = Ui.text(this, e.kind, 11, 0xFFFFFFFF, true);
            tag.setBackground(Ui.rounded(kindColor(e.kind), 8, this));
            int tp = Ui.dp(this, 8);
            tag.setPadding(tp, Ui.dp(this, 4), tp, Ui.dp(this, 4));
            row.addView(tag);

            LinearLayout col = Ui.column(this);
            LinearLayout.LayoutParams cl = Ui.lp(0, Ui.WRAP);
            cl.weight = 1;
            cl.leftMargin = Ui.dp(this, 12);
            col.setLayoutParams(cl);
            col.addView(Ui.text(this, e.title, 15, Ui.INK, true));
            col.addView(Ui.text(this, e.dateHuman() + "  ·  " + e.awayHuman()
                    + (e.note.isEmpty() ? "" : "  ·  " + e.note), 12, Ui.MUTED, false));
            row.addView(col);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, EventsActivity.class));
                }
            });
            eventsBox.addView(row);
        }
    }

    static int kindColor(String kind) {
        switch (kind) {
            case "CA":         return 0xFFD6336C;
            case "Assignment": return 0xFF1971C2;
            case "Quiz":       return 0xFF7048E8;
            case "Exam":       return 0xFFE8590C;
            default:           return 0xFF495057;
        }
    }

    // ---------------------------------------------------------------- tabs ---

    private void buildTabs() {
        dayRow.removeAllViews();
        for (int d = 1; d <= 5; d++) {
            dayRow.addView(buildTab(d));
        }
    }

    private View buildTab(final int day) {
        boolean sel = day == selectedDay;
        TextView tab = Ui.text(this, Timetable.DAY_NAMES[day - 1], 15,
                sel ? 0xFF3A2A20 : Ui.MUTED, sel);
        int px = Ui.dp(this, 18);
        int py = Ui.dp(this, 11);
        tab.setPadding(px, py, px, py);
        tab.setGravity(Gravity.CENTER);
        tab.setBackground(sel
                ? Ui.roundedGradient(Ui.ACCENT1, Ui.ACCENT2, 14, this)
                : Ui.rounded(0xFFFFFFFF, 14, this));
        LinearLayout.LayoutParams lp = Ui.lp(Ui.WRAP, Ui.WRAP);
        lp.rightMargin = Ui.dp(this, 10);
        tab.setLayoutParams(lp);
        tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDay = day;
                buildTabs();
                showDay(day);
            }
        });
        return tab;
    }

    // --------------------------------------------------------------- cards ---

    private void showDay(int day) {
        dayCards.removeAllViews();
        List<Slot> slots = Timetable.forDay(this, day);
        boolean isToday = day == Timetable.todayIndex();
        int now = Timetable.nowMinutes();

        if (slots.isEmpty()) {
            TextView empty = Ui.text(this, "No classes. Tap “+ Add class” to add one.", 15, Ui.MUTED, false);
            empty.setPadding(0, Ui.dp(this, 8), 0, 0);
            dayCards.addView(empty);
            return;
        }

        Store store = Store.get(this);
        for (final Slot s : slots) {
            boolean cancelled = isToday && store.isCancelledToday(s.id);
            int state = 0; // 0 normal, 1 ongoing, 2 done
            if (isToday && !cancelled) {
                if (now >= s.startMin && now < s.endMin) {
                    state = 1;
                } else if (now >= s.endMin) {
                    state = 2;
                }
            }
            dayCards.addView(buildClassCard(s, state, cancelled, isToday));
        }
    }

    private View buildClassCard(final Slot s, int state, boolean cancelled, final boolean isToday) {
        LinearLayout card = Ui.column(this);
        android.graphics.drawable.GradientDrawable bg = Ui.rounded(Ui.CARD, 16, this);
        if (state == 1) {
            bg.setStroke(Ui.dp(this, 2), Ui.GREEN);
        }
        card.setBackground(bg);
        card.setAlpha(cancelled ? 0.5f : (state == 2 ? 0.55f : 1f));

        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP);
        lp.bottomMargin = Ui.dp(this, 12);
        card.setLayoutParams(lp);

        TextView head = Ui.text(this, s.time(), 14, 0xFFFFFFFF, true);
        head.setBackground(Ui.rounded(Ui.HEADER, 16, this));
        int hp = Ui.dp(this, 12);
        head.setPadding(hp, Ui.dp(this, 10), hp, Ui.dp(this, 10));
        head.setGravity(Gravity.CENTER);
        card.addView(head);

        LinearLayout body = Ui.column(this);
        int bp = Ui.dp(this, 14);
        body.setPadding(bp, bp, bp, bp);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView course = Ui.text(this, s.course, 20, Ui.INK, true);
        LinearLayout.LayoutParams cl = Ui.lp(0, Ui.WRAP);
        cl.weight = 1;
        course.setLayoutParams(cl);
        topRow.addView(course);
        topRow.addView(Ui.text(this, s.room, 18, Ui.ACCENT1, true));
        body.addView(topRow);

        TextView meta = Ui.text(this,
                s.type + "  ·  " + s.group + "  ·  " + s.roomHuman(), 13, Ui.MUTED, false);
        meta.setPadding(0, Ui.dp(this, 6), 0, 0);
        body.addView(meta);

        if (cancelled) {
            TextView badge = Ui.text(this, "✕ cancelled today", 12, 0xFFB00020, true);
            badge.setPadding(0, Ui.dp(this, 8), 0, 0);
            body.addView(badge);
        } else if (state == 1) {
            TextView badge = Ui.text(this, "● happening now", 12, Ui.GREEN, true);
            badge.setPadding(0, Ui.dp(this, 8), 0, 0);
            body.addView(badge);
        }

        card.addView(body);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slotActions(s, isToday);
            }
        });
        return card;
    }

    // ------------------------------------------------------------- actions ---

    private void slotActions(final Slot s, boolean isToday) {
        final Store store = Store.get(this);
        final boolean cancelled = isToday && store.isCancelledToday(s.id);

        final List<String> opts = new java.util.ArrayList<>();
        opts.add("Edit");
        if (isToday) {
            opts.add(cancelled ? "Restore (un-cancel today)" : "Cancel today (teacher absent)");
        }
        opts.add("Delete");

        new AlertDialog.Builder(this)
                .setTitle(s.course + " · " + s.time())
                .setItems(opts.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        String choice = opts.get(which);
                        if (choice.equals("Edit")) {
                            editSlotDialog(s);
                        } else if (choice.equals("Delete")) {
                            confirmDelete(s);
                        } else {
                            store.toggleCancelToday(s.id);
                            refresh();
                        }
                    }
                })
                .show();
    }

    private void confirmDelete(final Slot s) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this class?")
                .setMessage(s.course + " · " + s.time() + "\nThis removes it from every week.")
                .setPositiveButton("Delete", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Store.get(MainActivity.this).deleteSlot(s.id);
                        refresh();
                    }
                })
                .setNegativeButton("Keep", null)
                .show();
    }

    // ----------------------------------------------------------- edit form ---

    private final int[] editStart = new int[1];
    private final int[] editEnd = new int[1];

    private void editSlotDialog(final Slot existing) {
        final boolean isNew = existing == null;

        LinearLayout form = Ui.column(this);
        int p = Ui.dp(this, 20);
        form.setPadding(p, Ui.dp(this, 8), p, 0);

        // day
        final int[] day = {isNew ? selectedDay : existing.day};
        form.addView(label("Day"));
        final TextView dayPick = pickerField(Timetable.DAY_NAMES[day[0] - 1]);
        dayPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setItems(Timetable.DAY_NAMES, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                day[0] = which + 1;
                                dayPick.setText(Timetable.DAY_NAMES[which]);
                            }
                        }).show();
            }
        });
        form.addView(dayPick);

        // times
        editStart[0] = isNew ? 560 : existing.startMin;
        editEnd[0] = isNew ? 610 : existing.endMin;
        form.addView(label("Start – End"));
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        final TextView startField = pickerField(Ttime.hhmm(editStart[0]) + " " + Ttime.period(editStart[0]));
        final TextView endField = pickerField(Ttime.hhmm(editEnd[0]) + " " + Ttime.period(editEnd[0]));
        LinearLayout.LayoutParams half = Ui.lp(0, Ui.WRAP);
        half.weight = 1;
        startField.setLayoutParams(half);
        LinearLayout.LayoutParams half2 = Ui.lp(0, Ui.WRAP);
        half2.weight = 1;
        half2.leftMargin = Ui.dp(this, 10);
        endField.setLayoutParams(half2);
        startField.setOnClickListener(timePick(editStart, startField));
        endField.setOnClickListener(timePick(editEnd, endField));
        timeRow.addView(startField);
        timeRow.addView(endField);
        form.addView(timeRow);

        // type
        final int[] typeIdx = {indexOf(TYPES, isNew ? "Lecture" : existing.type)};
        form.addView(label("Type"));
        final TextView typePick = pickerField(TYPES[typeIdx[0]]);
        typePick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setItems(TYPES, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                typeIdx[0] = which;
                                typePick.setText(TYPES[which]);
                            }
                        }).show();
            }
        });
        form.addView(typePick);

        // group
        final int[] groupIdx = {Math.max(0, indexOf(GROUPS, isNew ? "G:All" : existing.group))};
        form.addView(label("Group"));
        final TextView groupPick = pickerField(GROUPS[groupIdx[0]]);
        groupPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setItems(GROUPS, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                groupIdx[0] = which;
                                groupPick.setText(GROUPS[which]);
                            }
                        }).show();
            }
        });
        form.addView(groupPick);

        // course + room
        form.addView(label("Course code"));
        final EditText courseIn = input(isNew ? "" : existing.course, InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        form.addView(courseIn);
        form.addView(label("Room (e.g. 28-506)"));
        final EditText roomIn = input(isNew ? "" : existing.room, InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        form.addView(roomIn);

        ScrollView sv = new ScrollView(this);
        sv.addView(form);

        new AlertDialog.Builder(this)
                .setTitle(isNew ? "Add class" : "Edit class")
                .setView(sv)
                .setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        String course = courseIn.getText().toString().trim().toUpperCase();
                        String room = roomIn.getText().toString().trim().toUpperCase();
                        if (course.isEmpty()) {
                            course = "CLASS";
                        }
                        if (room.isEmpty()) {
                            room = "—";
                        }
                        if (editEnd[0] <= editStart[0]) {
                            editEnd[0] = editStart[0] + 50;
                        }
                        Store store = Store.get(MainActivity.this);
                        long id = isNew ? store.newId() : existing.id;
                        Slot slot = new Slot(id, day[0], editStart[0], editEnd[0],
                                TYPES[typeIdx[0]], GROUPS[groupIdx[0]], course, room);
                        store.upsertSlot(slot);
                        selectedDay = day[0];
                        refresh();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private View.OnClickListener timePick(final int[] holder, final TextView field) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int h = holder[0] / 60;
                int m = holder[0] % 60;
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        holder[0] = hour * 60 + minute;
                        field.setText(Ttime.hhmm(holder[0]) + " " + Ttime.period(holder[0]));
                    }
                }, h, m, false).show();
            }
        };
    }

    // ------------------------------------------------------- small builders ---

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

    private EditText input(String value, int type) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setInputType(type);
        e.setTextColor(Ui.INK);
        e.setSingleLine(true);
        return e;
    }

    private int indexOf(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(v)) {
                return i;
            }
        }
        return 0;
    }

    private View pillButton(String text, View.OnClickListener click) {
        TextView t = Ui.text(this, text, 14, 0xFF3A2A20, true);
        t.setBackground(Ui.roundedGradient(Ui.ACCENT1, Ui.ACCENT2, 14, this));
        int px = Ui.dp(this, 16);
        int py = Ui.dp(this, 10);
        t.setPadding(px, py, px, py);
        t.setOnClickListener(click);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.WRAP, Ui.WRAP);
        lp.leftMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        return t;
    }

    private View menuButton() {
        TextView t = Ui.text(this, "≡", 22, Ui.INK, true);
        t.setBackground(Ui.rounded(0xFFFFFFFF, 14, this));
        int px = Ui.dp(this, 14);
        int py = Ui.dp(this, 6);
        t.setPadding(px, py, px, py);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });
        LinearLayout.LayoutParams lp = Ui.lp(Ui.WRAP, Ui.WRAP);
        lp.leftMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        return t;
    }

    // ------------------------------------------------------------ onboarding ---

    private void showOnboarding() {
        onboarding = true;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);

        LinearLayout root = Ui.column(this);
        root.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Ui.dp(this, 26);
        root.setPadding(pad, Ui.dp(this, 40), pad, Ui.dp(this, 40));

        LinearLayout hero = Ui.column(this);
        hero.setBackground(Ui.roundedGradient(Ui.ACCENT1, Ui.ACCENT2, 22, this));
        int hp = Ui.dp(this, 24);
        hero.setPadding(hp, hp, hp, hp);
        hero.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        TextView welcome = Ui.text(this, "Welcome to", 13, 0xFFFFF3EC, true);
        welcome.setLetterSpacing(0.08f);
        hero.addView(welcome);
        hero.addView(Ui.text(this, "NextClass", 34, 0xFFFFFFFF, true));
        hero.addView(Ui.text(this,
                "Your timetable and next-class room, right on your home screen. Fully offline.",
                14, 0xFFFFF3EC, false));
        root.addView(hero);

        TextView q = Ui.text(this, "Get your week in", 18, Ui.INK, true);
        q.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 4));
        root.addView(q);

        root.addView(onboardCard("Paste a share code",
                "Fastest — a classmate sends a code, you paste it", true,
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onboardPasteDialog();
            }
        }));
        root.addView(onboardCard("Start with an empty timetable",
                "Add your classes yourself with “+ Add class”", false,
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Store.get(MainActivity.this).setOnboarded(true);
                recreate();
            }
        }));

        TextView foot = Ui.text(this,
                "Nothing leaves your phone. Made by Bijo.", 12, Ui.MUTED, false);
        foot.setPadding(0, Ui.dp(this, 24), 0, 0);
        foot.setGravity(Gravity.CENTER);
        foot.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        root.addView(foot);

        scroll.addView(root);
        setContentView(scroll);
    }

    private View onboardCard(String title, String sub, boolean primary, View.OnClickListener click) {
        LinearLayout card = Ui.column(this);
        card.setBackground(primary
                ? Ui.roundedGradient(Ui.ACCENT1, Ui.ACCENT2, 16, this)
                : Ui.rounded(Ui.CARD, 16, this));
        int p = Ui.dp(this, 18);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP);
        lp.topMargin = Ui.dp(this, 10);
        card.setLayoutParams(lp);
        int titleColor = primary ? 0xFF3A2A20 : Ui.INK;
        int subColor = primary ? 0xFF5A3E2E : Ui.MUTED;
        card.addView(Ui.text(this, title, 17, titleColor, true));
        TextView s = Ui.text(this, sub, 13, subColor, false);
        s.setPadding(0, Ui.dp(this, 3), 0, 0);
        card.addView(s);
        card.setOnClickListener(click);
        return card;
    }

    private void onboardPasteDialog() {
        final EditText box = new EditText(this);
        box.setHint("Paste the NCT1-… code here");
        box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        box.setTextColor(Ui.INK);
        int p = Ui.dp(this, 12);
        box.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("Paste a share code")
                .setView(box)
                .setPositiveButton("Import", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Store store = Store.get(MainActivity.this);
                        try {
                            List<Slot> fresh = ShareCode.decode(store, box.getText().toString());
                            store.replaceSlots(fresh);
                            NextClassWidget.refresh(MainActivity.this);
                            recreate();
                        } catch (Exception ex) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Couldn't import")
                                    .setMessage(ex.getMessage())
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
