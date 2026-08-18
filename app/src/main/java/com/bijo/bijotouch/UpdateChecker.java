package com.bijo.bijotouch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Once-a-day check of the GitHub Releases API for a newer version. Never blocks
 * the UI and never installs anything - it just remembers the latest tag so the
 * home screen can offer a "download the update" banner. GitHub is the only host
 * the app ever contacts, and only for this.
 */
final class UpdateChecker {

    private static final String API =
            "https://api.github.com/repos/bijo-ai/nextclass/releases/latest";
    private static final String PREFS = "nextclass_prefs";
    private static final long DAY = 24L * 60 * 60 * 1000;

    interface Listener {
        void onChanged();
    }

    private UpdateChecker() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String latestVersion(Context ctx) {
        return prefs(ctx).getString("latestVersion", "");
    }

    static String latestUrl(Context ctx) {
        return prefs(ctx).getString("latestUrl", "");
    }

    static String currentVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    /** True when a newer, non-dismissed version is known. */
    static boolean updateAvailable(Context ctx) {
        String latest = latestVersion(ctx);
        if (latest.isEmpty() || latest.equals(prefs(ctx).getString("dismissed", ""))) {
            return false;
        }
        return isNewer(latest, currentVersion(ctx));
    }

    static void dismiss(Context ctx) {
        prefs(ctx).edit().putString("dismissed", latestVersion(ctx)).apply();
    }

    /** Kick off a throttled background check; calls back on the main thread if state changed. */
    static void check(Context ctx, final Listener l) {
        if (System.currentTimeMillis() - prefs(ctx).getLong("lastCheck", 0) < DAY) {
            return;
        }
        final Context app = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection c = null;
                try {
                    c = (HttpURLConnection) new URL(API).openConnection();
                    c.setRequestProperty("User-Agent", "NextClass-App");
                    c.setRequestProperty("Accept", "application/vnd.github+json");
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    if (c.getResponseCode() != 200) {
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        sb.append(line);
                    }
                    r.close();
                    JSONObject o = new JSONObject(sb.toString());
                    String tag = o.optString("tag_name", "").replaceAll("[^0-9.]", "");
                    String url = o.optString("html_url", "");
                    prefs(app).edit()
                            .putString("latestVersion", tag)
                            .putString("latestUrl", url)
                            .putLong("lastCheck", System.currentTimeMillis())
                            .apply();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (l != null) {
                                l.onChanged();
                            }
                        }
                    });
                } catch (Exception ignored) {
                    // offline / API hiccup - try again next day, never bother the user
                } finally {
                    if (c != null) {
                        c.disconnect();
                    }
                }
            }
        }).start();
    }

    /** Numeric version compare: "1.10" > "1.9" > "1.0". */
    static boolean isNewer(String latest, String current) {
        String[] a = latest.split("\\.");
        String[] b = current.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? parse(a[i]) : 0;
            int y = i < b.length ? parse(b[i]) : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
