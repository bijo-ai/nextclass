package com.bijo.bijotouch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * The ≡ page: share / import a timetable, and the maker's links. Kept as a
 * plain scrolling screen (no drawer library) so the app stays dependency-free.
 */
public class AboutActivity extends Activity {

    private static final String GITHUB   = "https://github.com/bijo-ai";
    private static final String LINKEDIN = "https://www.linkedin.com/in/bijo-k-varghese-9bba32319";

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

        root.addView(Ui.text(this, "NextClass", 28, Ui.INK, true));
        TextView tag = Ui.text(this,
                "Your timetable & next class — fully offline. Touches no LPU server.",
                13, Ui.MUTED, false);
        tag.setPadding(0, Ui.dp(this, 4), 0, 0);
        root.addView(tag);

        // ---- timetable tools ----
        root.addView(sectionLabel("Your timetable"));
        root.addView(actionCard("Share my timetable",
                "Send a code your classmates can paste in", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareTimetable();
            }
        }));
        root.addView(actionCard("Import a share code",
                "Paste a code to replace your timetable", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importDialog();
            }
        }));

        // ---- maker ----
        root.addView(sectionLabel("About"));
        TextView made = Ui.text(this, "Made by Bijo", 15, Ui.INK, true);
        made.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
        root.addView(made);
        root.addView(Ui.text(this,
                "A student project, open source and free. Say hi 👇", 13, Ui.MUTED, false));
        root.addView(linkCard("GitHub", "github.com/bijo-ai", GITHUB));
        root.addView(linkCard("LinkedIn", "Bijo K Varghese", LINKEDIN));

        TextView ver = Ui.text(this, "v1.0", 12, Ui.MUTED, false);
        ver.setPadding(0, Ui.dp(this, 20), 0, 0);
        root.addView(ver);

        setContentView(scroll);
    }

    // ---------------------------------------------------------- share/import ---

    private void shareTimetable() {
        List<Slot> slots = Store.get(this).slots;
        if (slots.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Nothing to share yet")
                    .setMessage("Add a few classes first, then come back to share your timetable.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        final String code = ShareCode.encode(slots);

        final EditText box = new EditText(this);
        box.setText(code);
        box.setTextColor(Ui.INK);
        box.setTextSize(12);
        box.setKeyListener(null); // read-only but selectable
        int p = Ui.dp(this, 16);
        box.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("Your share code")
                .setMessage("Send this to your section. They open NextClass → ≡ → Import a share code.")
                .setView(box)
                .setPositiveButton("Copy", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("NextClass timetable", code));
                        toast("Code copied");
                    }
                })
                .setNeutralButton("Share…", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType("text/plain");
                        send.putExtra(Intent.EXTRA_TEXT,
                                "My NextClass timetable — paste this in the app (≡ → Import a share code):\n\n" + code);
                        startActivity(Intent.createChooser(send, "Share timetable"));
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void importDialog() {
        final EditText box = new EditText(this);
        box.setHint("Paste the NCT1-… code here");
        box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        box.setTextColor(Ui.INK);
        int p = Ui.dp(this, 12);
        box.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("Import a share code")
                .setMessage("This replaces your current timetable. Your events and cancellations stay.")
                .setView(box)
                .setPositiveButton("Import", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Store store = Store.get(AboutActivity.this);
                        try {
                            List<Slot> fresh = ShareCode.decode(store, box.getText().toString());
                            store.replaceSlots(fresh);
                            NextClassWidget.refresh(AboutActivity.this);
                            toast("Imported " + fresh.size() + " classes");
                            finish(); // back to home, which reloads on resume
                        } catch (Exception ex) {
                            new AlertDialog.Builder(AboutActivity.this)
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

    // ------------------------------------------------------------- builders ---

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("No browser found");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private TextView sectionLabel(String s) {
        TextView t = Ui.text(this, s.toUpperCase(), 12, Ui.MUTED, true);
        t.setLetterSpacing(0.06f);
        t.setPadding(0, Ui.dp(this, 26), 0, Ui.dp(this, 10));
        return t;
    }

    private View actionCard(String title, String sub, View.OnClickListener click) {
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.rounded(Ui.CARD, 16, this));
        int p = Ui.dp(this, 16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP);
        lp.bottomMargin = Ui.dp(this, 10);
        card.setLayoutParams(lp);
        card.addView(Ui.text(this, title, 16, Ui.INK, true));
        TextView s = Ui.text(this, sub, 13, Ui.MUTED, false);
        s.setPadding(0, Ui.dp(this, 3), 0, 0);
        card.addView(s);
        card.setOnClickListener(click);
        return card;
    }

    private View linkCard(String title, String sub, final String url) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(Ui.rounded(Ui.CARD, 16, this));
        int p = Ui.dp(this, 16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP);
        lp.topMargin = Ui.dp(this, 10);
        card.setLayoutParams(lp);

        LinearLayout col = Ui.column(this);
        LinearLayout.LayoutParams cl = Ui.lp(0, Ui.WRAP);
        cl.weight = 1;
        col.setLayoutParams(cl);
        col.addView(Ui.text(this, title, 16, Ui.INK, true));
        col.addView(Ui.text(this, sub, 13, Ui.MUTED, false));
        card.addView(col);
        card.addView(Ui.text(this, "↗", 20, Ui.ACCENT1, true));

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(url);
            }
        });
        return card;
    }
}
