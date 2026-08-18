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
 * Home-screen widget: the class on right now (or next), room called out large,
 * plus the nearest deadline. Repaints itself on the timetable's own transition
 * points so it flips to the next class ~5 minutes before the current one ends.
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

    /**
     * Wake up at the next class transition (a start, or an end), so the flip is
     * on time - with a 20-minute safety net in case a slot boundary is far off
     * or the day is over.
     */
    private void scheduleTick(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }

        Calendar when = Calendar.getInstance();
        int transition = Timetable.nextTransitionToday(ctx);
        int now = Timetable.nowMinutes();

        boolean atBoundary = transition > now;
        if (atBoundary) {
            when.set(Calendar.HOUR_OF_DAY, transition / 60);
            when.set(Calendar.MINUTE, transition % 60);
            when.set(Calendar.SECOND, 1);
            when.set(Calendar.MILLISECOND, 0);
        } else {
            when.add(Calendar.MINUTE, 20); // nothing left today - just idle-check
        }

        long at = when.getTimeInMillis();
        PendingIntent pi = tickIntent(ctx);

        // A class boundary must fire on the dot, so the widget flips the second
        // the class ends - not whenever the OS feels like batching an inexact
        // alarm. Fall back to inexact only if exact alarms aren't permitted.
        if (atBoundary && canExact(am)) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                return;
            } catch (SecurityException ignored) {
                // permission pulled at runtime - drop to the inexact path
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

    private RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_next);
        Timetable.Status st = Timetable.status(ctx);

        String kicker, room, detail, time;
        if (st.current != null) {
            kicker = "NOW";
            room = st.current.room;
            detail = st.current.course + " · " + st.current.type;
            time = st.current.time();
        } else if (st.next != null && st.nextDay == 0) {
            kicker = "NEXT" + away(st.next);
            room = st.next.room;
            detail = st.next.course + " · " + st.next.type;
            time = st.next.time();
        } else if (st.next != null) {
            kicker = "NEXT · " + Timetable.DAY_NAMES[st.nextDay - 1].toUpperCase();
            room = st.next.room;
            detail = st.next.course + " · " + st.next.type;
            time = st.next.time();
        } else {
            kicker = "DONE";
            room = "—";
            detail = "No more classes";
            time = "Enjoy the break";
        }

        v.setTextViewText(R.id.w_kicker, kicker);
        v.setTextViewText(R.id.w_room, room);
        v.setTextViewText(R.id.w_detail, detail);
        v.setTextViewText(R.id.w_time, time);

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

    private String away(Slot next) {
        int diff = next.startMin - Timetable.nowMinutes();
        if (diff <= 0 || diff > 600) {
            return "";
        }
        int h = diff / 60;
        int m = diff % 60;
        return h > 0 ? " · in " + h + "h " + m + "m" : " · in " + m + "m";
    }
}
