package com.bijo.bijotouch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;

/**
 * Home-screen widget. Four looks: ONGOING (green, with a progress bar), NEXT
 * (coral - the upcoming class), DONE (blue - day over, tomorrow previewed), and
 * WEEKEND (violet - a motivational sign-off). It repaints itself on the
 * timetable's own transition points, and every ~10 min while a class is running
 * so the progress bar keeps moving.
 */
public class NextClassWidget extends AppWidgetProvider {

    private static final String ACTION_TICK = "com.bijo.bijotouch.TICK";

    /** Callable from the activities so an edit shows on the widget at once. */
    public static void refresh(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName cn = new ComponentName(ctx, NextClassWidget.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        if (ids == null || ids.length == 0) {
            return;
        }
        RemoteViews v = new NextClassWidget().build(ctx);
        for (int id : ids) {
            mgr.updateAppWidget(id, v);
        }
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        RemoteViews v = build(ctx);
        for (int id : ids) {
            mgr.updateAppWidget(id, v);
        }
        scheduleTick(ctx);
    }

    @Override
    public void onEnabled(Context ctx) {
        scheduleTick(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(tickIntent(ctx));
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_TICK.equals(intent.getAction())) {
            refresh(ctx);
            scheduleTick(ctx);
        }
    }

    // ------------------------------------------------------------- rendering ---

    private RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_next);
        Timetable.Status st = Timetable.status(ctx);

        int bg;
        String kicker, room, detail, time;
        boolean progress = false;

        switch (st.state) {
            case ONGOING:
                bg = R.drawable.widget_bg_ongoing;
                kicker = "● ONGOING";
                room = st.slot.room;
                detail = st.slot.course + " · " + st.slot.type;
                time = st.slot.time();
                progress = true;
                v.setProgressBar(R.id.w_progress, 100, st.progressPct, false);
                break;
            case NEXT:
                bg = R.drawable.widget_bg_next;
                kicker = "NEXT" + ((st.minsToStart > 0 && st.minsToStart <= 600)
                        ? " · in " + Ttime.durHuman(st.minsToStart) : "");
                room = st.slot.room;
                detail = st.slot.course + " · " + st.slot.type;
                time = st.slot.time();
                break;
            case DONE_TODAY:
                bg = R.drawable.widget_bg_done;
                kicker = "DONE FOR TODAY";
                room = "That's a wrap ✌️";
                detail = "See you " + Timetable.DAY_NAMES[st.nextDay - 1];
                time = "First up " + st.slot.room + " · "
                        + Ttime.hhmm(st.slot.startMin) + " " + Ttime.period(st.slot.startMin);
                break;
            case WEEKEND:
                bg = R.drawable.widget_bg_weekend;
                kicker = "WEEKEND";
                room = "Weekend plans?? 🎉";
                detail = Motivation.line();
                time = "";
                break;
            default: // EMPTY
                bg = R.drawable.widget_bg_empty;
                kicker = "NO CLASSES YET";
                room = "Add your week";
                detail = "Open the app → + Add class";
                time = "";
                break;
        }

        v.setInt(R.id.w_root, "setBackgroundResource", bg);
        v.setTextViewText(R.id.w_kicker, kicker);
        v.setTextViewText(R.id.w_room, room);
        v.setTextViewText(R.id.w_detail, detail);
        v.setTextViewText(R.id.w_time, time);
        v.setViewVisibility(R.id.w_time, time.isEmpty() ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.w_progress, progress ? View.VISIBLE : View.GONE);

        // Nearest deadline as a chip, if any.
        Event e = Timetable.nextEvent(ctx);
        if (e != null) {
            v.setTextViewText(R.id.w_event, e.kind + ": " + e.title + " · " + e.awayHuman());
            v.setViewVisibility(R.id.w_event, View.VISIBLE);
        } else {
            v.setViewVisibility(R.id.w_event, View.GONE);
        }

        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.w_root, pi);
        return v;
    }

    // -------------------------------------------------------------- timing ---

    /**
     * Wake at the next class transition; while a class is ongoing, also at least
     * every ~10 min so the progress bar advances. A 20-minute idle fallback
     * covers the times nothing is scheduled.
     */
    private void scheduleTick(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }

        int now = Timetable.nowMinutes();
        int transition = Timetable.nextTransitionToday(ctx);
        boolean ongoing = Timetable.status(ctx).state == Timetable.State.ONGOING;

        int target;
        if (ongoing) {
            int soon = now + 10;
            target = (transition > now) ? Math.min(transition, soon) : soon;
        } else if (transition > now) {
            target = transition;
        } else {
            target = -1;
        }

        Calendar when = Calendar.getInstance();
        boolean exact;
        if (target > now) {
            when.set(Calendar.HOUR_OF_DAY, target / 60);
            when.set(Calendar.MINUTE, target % 60);
            when.set(Calendar.SECOND, 1);
            when.set(Calendar.MILLISECOND, 0);
            exact = target == transition; // a real boundary fires on the dot
        } else {
            when.add(Calendar.MINUTE, 20);
            exact = false;
        }

        long at = when.getTimeInMillis();
        PendingIntent pi = tickIntent(ctx);
        if (exact && canExact(am)) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                return;
            } catch (SecurityException ignored) {
                // fall through to the inexact path
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    private boolean canExact(AlarmManager am) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            return am.canScheduleExactAlarms();
        }
        return true;
    }

    private PendingIntent tickIntent(Context ctx) {
        Intent i = new Intent(ctx, NextClassWidget.class);
        i.setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(ctx, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
