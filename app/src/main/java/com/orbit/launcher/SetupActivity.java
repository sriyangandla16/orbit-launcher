package com.orbit.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

/** "Customise" screen: Home-button setup, options and a small debug panel. */
public class SetupActivity extends Activity {

    private TextView status, takeoverBtn, modeBtn, hiddenBtn, screens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int side = Math.round(getResources().getDisplayMetrics().widthPixels * 0.07f);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(side, dp(36), side, dp(48));
        col.setClipChildren(false);
        col.setClipToPadding(false);

        col.addView(UiKit.text(this, getString(R.string.setup_title), 30, 255, "sans-serif-light"));

        status = UiKit.text(this, "", 17, 235, "sans-serif-medium");
        addWithTop(col, status, 18);

        String component = new ComponentName(this, HomeWatcherService.class).flattenToString();

        TextView help = UiKit.text(this, getString(R.string.setup_help, component), 14, 170, "sans-serif");
        help.setLineSpacing(0, 1.25f);
        addWithTop(col, help, 10);

        TextView openA11y = UiKit.pill(this, getString(R.string.open_accessibility));
        openA11y.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(SetupActivity.this, R.string.no_accessibility_screen, Toast.LENGTH_LONG).show();
                }
            }
        });
        addButton(col, openA11y, 20);

        col.addView(sectionTitle(R.string.options));

        takeoverBtn = UiKit.pill(this, "");
        takeoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setTakeover(SetupActivity.this, !Prefs.takeover(SetupActivity.this));
                refresh();
            }
        });
        addButton(col, takeoverBtn, 12);

        modeBtn = UiKit.pill(this, "");
        modeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setAggressive(SetupActivity.this, !Prefs.aggressive(SetupActivity.this));
                refresh();
            }
        });
        addButton(col, modeBtn, 12);

        hiddenBtn = UiKit.pill(this, "");
        hiddenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setHidden(SetupActivity.this, new HashSet<String>());
                Toast.makeText(SetupActivity.this, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
        addButton(col, hiddenBtn, 12);

        TextView resetFavs = UiKit.pill(this, getString(R.string.reset_favs));
        resetFavs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setFavourites(SetupActivity.this, new ArrayList<String>());
                Toast.makeText(SetupActivity.this, R.string.favs_cleared, Toast.LENGTH_SHORT).show();
            }
        });
        addButton(col, resetFavs, 12);

        col.addView(sectionTitle(R.string.debug_title));
        screens = UiKit.text(this, "", 13, 150, "monospace");
        screens.setLineSpacing(0, 1.3f);
        addWithTop(col, screens, 8);

        TextView version = UiKit.text(this, getString(R.string.version, versionName()), 13, 110, "sans-serif");
        addWithTop(col, version, 28);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(col, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        openA11y.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean on = HomeWatcherService.isEnabled(this);
        status.setText(on ? R.string.status_on : R.string.status_off);
        status.setTextColor(on ? 0xFF7CE0A0 : 0xFFFFB86B);

        takeoverBtn.setText(Prefs.takeover(this) ? R.string.takeover_on : R.string.takeover_off);
        modeBtn.setText(Prefs.aggressive(this) ? R.string.mode_aggressive : R.string.mode_safe);
        int hidden = Prefs.hidden(this).size();
        hiddenBtn.setText(getString(R.string.unhide_all, hidden));
        hiddenBtn.setVisibility(hidden == 0 ? View.GONE : View.VISIBLE);

        String seen = Prefs.seenScreens(this);
        screens.setText(seen.isEmpty() ? getString(R.string.debug_empty) : seen);
    }

    private TextView sectionTitle(int res) {
        TextView t = UiKit.text(this, getString(res), 16, 180, "sans-serif-medium");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(30);
        t.setLayoutParams(lp);
        return t;
    }

    private void addButton(LinearLayout col, TextView b, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        col.addView(b, lp);
    }

    private void addWithTop(LinearLayout col, View v, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        col.addView(v, lp);
    }

    private String versionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private int dp(float v) {
        return UiKit.dp(this, v);
    }
}
