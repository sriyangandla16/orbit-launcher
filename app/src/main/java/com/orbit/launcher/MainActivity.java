package com.orbit.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.orbit.launcher.AppRepository.AppEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The home screen: clock, favourites row and a grid of every app. */
public class MainActivity extends Activity {

    /** Picked automatically as favourites on first run, if installed. */
    private static final String[] DEFAULT_FAVOURITES = {
            "com.amazon.firetv.youtube",
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.amazon.firebat",
            "com.amazon.avod.thirdpartyclient",
            "in.startv.hotstar",
            "com.spotify.tv.android",
    };

    private static final int GRID_COLUMNS = 5;
    private static final long AMAZON_PAUSE_MS = 30 * 60 * 1000L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService background = Executors.newSingleThreadExecutor();

    private ScrollView scroll;
    private LinearLayout content, favRow, grid, headerButtons;
    private TextView favHint, loading;

    private List<AppEntry> apps = new ArrayList<>();
    private String focusAfterRender;
    private boolean firstRender = true;
    private View firstCard;
    private int favW, favH, gridW, gridH, gap;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reload();   // an app was installed, removed or updated
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scroll = findViewById(R.id.scroll);
        content = findViewById(R.id.content);
        favRow = findViewById(R.id.fav_row);
        grid = findViewById(R.id.grid);
        headerButtons = findViewById(R.id.header_buttons);
        favHint = findViewById(R.id.fav_hint);
        loading = findViewById(R.id.loading);

        computeSizes();
        buildHeader();

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        registerReceiver(packageReceiver, f);

        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back on our screen, so the "Amazon Home" pause is over.
        Prefs.setPausedUntil(this, 0);
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception ignored) {
        }
        background.shutdownNow();
        super.onDestroy();
    }

    /** Back never leaves the home screen; it jumps to the top instead. */
    @Override
    public void onBackPressed() {
        scroll.smoothScrollTo(0, 0);
        if (firstCard != null) firstCard.requestFocus();
    }

    // ---------------------------------------------------------------- layout

    private void computeSizes() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int side = Math.round(width * 0.055f);
        content.setPadding(side, content.getPaddingTop(), side, content.getPaddingBottom());
        gap = dp(22);
        int avail = width - 2 * side;
        favW = (avail - (Prefs.MAX_FAVOURITES - 1) * gap) / Prefs.MAX_FAVOURITES;
        favH = favW * 9 / 16;
        gridW = (avail - (GRID_COLUMNS - 1) * gap) / GRID_COLUMNS;
        gridH = gridW * 9 / 16;
    }

    private void buildHeader() {
        final Intent amazonHome = amazonHomeIntent();
        if (amazonHome != null) {
            addHeaderButton(getString(R.string.amazon_home), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Let Amazon's screen stay open (for Prime Video etc.) until you come back here.
                    Prefs.setPausedUntil(MainActivity.this, System.currentTimeMillis() + AMAZON_PAUSE_MS);
                    safeStart(amazonHome);
                }
            });
        }
        addHeaderButton(getString(R.string.settings), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                safeStart(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
        addHeaderButton(getString(R.string.customise), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SetupActivity.class));
            }
        });
    }

    private void addHeaderButton(String label, View.OnClickListener click) {
        TextView b = UiKit.pill(this, label);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(12);
        headerButtons.addView(b, lp);
    }

    private Intent amazonHomeIntent() {
        PackageManager pm = getPackageManager();
        String pkg = AppRepository.AMAZON_LAUNCHER;
        Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (pm.resolveActivity(home, 0) != null) return home;
        Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
        return launch == null ? null : launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    // ---------------------------------------------------------------- data

    private void reload() {
        try {
            background.execute(new Runnable() {
                @Override
                public void run() {
                    final List<AppEntry> list = AppRepository.load(getApplicationContext());
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            apps = list;
                            render();
                        }
                    });
                }
            });
        } catch (Exception ignored) {
            // executor already shut down
        }
    }

    private void render() {
        loading.setVisibility(View.GONE);

        View focused = getCurrentFocus();
        String keepPkg = focusAfterRender;
        if (keepPkg == null && focused != null && focused.getTag() instanceof String) {
            keepPkg = (String) focused.getTag();
        }
        focusAfterRender = null;
        firstCard = null;

        Map<String, AppEntry> byPkg = new HashMap<>();
        for (AppEntry e : apps) byPkg.put(e.pkg, e);

        List<String> favs = Prefs.favourites(this);
        if (favs == null) {
            favs = new ArrayList<>();
            for (String p : DEFAULT_FAVOURITES) {
                if (byPkg.containsKey(p) && !favs.contains(p) && favs.size() < Prefs.MAX_FAVOURITES) {
                    favs.add(p);
                }
            }
            Prefs.setFavourites(this, favs);
        }
        Set<String> hidden = Prefs.hidden(this);

        // Favourites row
        favRow.removeAllViews();
        int i = 0;
        for (String p : favs) {
            AppEntry e = byPkg.get(p);
            if (e == null) continue;   // uninstalled; keep it saved in case it comes back
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(favW, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.leftMargin = gap;
            favRow.addView(makeTile(e, favW, favH, true), lp);
            i++;
        }
        favRow.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
        favHint.setVisibility(i == 0 ? View.VISIBLE : View.GONE);

        // All-apps grid
        grid.removeAllViews();
        List<AppEntry> rest = new ArrayList<>();
        for (AppEntry e : apps) {
            if (!hidden.contains(e.pkg) && !favs.contains(e.pkg)) rest.add(e);
        }
        LinearLayout row = null;
        for (int n = 0; n < rest.size(); n++) {
            if (n % GRID_COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (n > 0) rlp.topMargin = dp(4);
                grid.addView(row, rlp);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(gridW, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (n % GRID_COLUMNS != 0) lp.leftMargin = gap;
            row.addView(makeTile(rest.get(n), gridW, gridH, false), lp);
        }

        // Put focus back where it was, or on the first tile.
        View target = keepPkg == null ? null : content.findViewWithTag(keepPkg);
        if (target != null) {
            target.requestFocus();
        } else if ((firstRender || getCurrentFocus() == null) && firstCard != null) {
            firstCard.requestFocus();
        }
        firstRender = false;
    }

    /** One app tile: a rounded 16:9 card, with the name fading in below when focused. */
    private View makeTile(final AppEntry e, int w, int h, final boolean isFavourite) {
        final LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setClipChildren(false);
        wrap.setClipToPadding(false);

        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(isFavourite ? 14 : 11));
        bg.setColor(e.banner != null ? 0xFF1E222C : e.tint);
        card.setBackground(bg);
        card.setClipToOutline(true);
        card.setElevation(dp(3));
        card.setAlpha(0.88f);

        ImageView img = new ImageView(this);
        if (e.banner != null) {
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageDrawable(e.banner);
            card.addView(img, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setImageDrawable(e.icon);
            int s = Math.round(h * 0.56f);
            card.addView(img, new FrameLayout.LayoutParams(s, s, Gravity.CENTER));
        }

        final TextView label = new TextView(this);
        label.setText(e.label);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, isFavourite ? 15 : 13);
        label.setTextColor(0xF2FFFFFF);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        label.setAlpha(0f);

        card.setTag(e.pkg);
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                safeStart(e.launch);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showMenu(e);
                return true;
            }
        });
        card.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_UP) {
                    showMenu(e);
                    return true;
                }
                return false;
            }
        });
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                float s = has ? 1.10f : 1f;
                v.animate().scaleX(s).scaleY(s).alpha(has ? 1f : 0.88f)
                        .translationZ(has ? dp(16) : 0).setDuration(150).start();
                wrap.setTranslationZ(has ? 1 : 0);   // draw above neighbouring tiles
                label.animate().alpha(has ? 1f : 0f).translationY(has ? dp(5) : 0)
                        .setDuration(150).start();
                if (has) keepInView(wrap, isFavourite);
            }
        });

        wrap.addView(card, new LinearLayout.LayoutParams(w, h));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(10);
        wrap.addView(label, llp);

        if (firstCard == null) firstCard = card;
        return wrap;
    }

    /** Scrolls so the focused tile sits near the middle of the screen. */
    private void keepInView(final View wrap, final boolean isFavourite) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                if (isFavourite) {
                    scroll.smoothScrollTo(0, 0);
                    return;
                }
                if (wrap.getParent() == null) return;
                Rect r = new Rect();
                wrap.getDrawingRect(r);
                content.offsetDescendantRectToMyCoords(wrap, r);
                int target = r.centerY() - scroll.getHeight() / 2;
                scroll.smoothScrollTo(0, Math.max(0, target));
            }
        });
    }

    // ---------------------------------------------------------------- menu

    /** Hold OK (or press Menu) on a tile. */
    private void showMenu(final AppEntry e) {
        List<String> saved = Prefs.favourites(this);
        final List<String> favs = saved == null ? new ArrayList<String>() : saved;
        final boolean isFav = favs.contains(e.pkg);

        List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        if (isFav) {
            final int idx = favs.indexOf(e.pkg);
            if (idx > 0) {
                labels.add(getString(R.string.move_left));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        Collections.swap(favs, idx, idx - 1);
                        saveFavsAndFocus(favs, e.pkg);
                    }
                });
            }
            if (idx < favs.size() - 1) {
                labels.add(getString(R.string.move_right));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        Collections.swap(favs, idx, idx + 1);
                        saveFavsAndFocus(favs, e.pkg);
                    }
                });
            }
            labels.add(getString(R.string.remove_fav));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    favs.remove(e.pkg);
                    saveFavsAndFocus(favs, e.pkg);
                }
            });
        } else {
            labels.add(getString(R.string.add_fav));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    if (countInstalled(favs) >= Prefs.MAX_FAVOURITES) {
                        Toast.makeText(MainActivity.this, R.string.fav_full, Toast.LENGTH_LONG).show();
                        return;
                    }
                    favs.add(e.pkg);
                    saveFavsAndFocus(favs, e.pkg);
                }
            });
            labels.add(getString(R.string.hide_app));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    Set<String> hidden = Prefs.hidden(MainActivity.this);
                    hidden.add(e.pkg);
                    Prefs.setHidden(MainActivity.this, hidden);
                    Toast.makeText(MainActivity.this, R.string.hidden_toast, Toast.LENGTH_LONG).show();
                    render();
                }
            });
        }
        labels.add(getString(R.string.app_info));
        actions.add(new Runnable() {
            @Override
            public void run() {
                safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", e.pkg, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(e.label)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .show();
    }

    private int countInstalled(List<String> favs) {
        int n = 0;
        for (AppEntry a : apps) if (favs.contains(a.pkg)) n++;
        return n;
    }

    private void saveFavsAndFocus(List<String> favs, String pkg) {
        Prefs.setFavourites(this, favs);
        focusAfterRender = pkg;
        render();
    }

    // ---------------------------------------------------------------- helpers

    private void safeStart(Intent i) {
        try {
            startActivity(i);
        } catch (Exception ex) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(float v) {
        return UiKit.dp(this, v);
    }
}
