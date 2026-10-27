package com.orbit.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds every app that can be opened from a TV home screen. */
final class AppRepository {

    /** One app on the home screen. */
    static final class AppEntry {
        String pkg;
        String label;
        Intent launch;
        Drawable banner;   // 16:9 TV artwork, if the app has one
        Drawable icon;     // square icon, used when there is no banner
        int tint;          // card colour behind the icon
    }

    static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";

    private AppRepository() {}

    static List<AppEntry> load(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, AppEntry> byPkg = new LinkedHashMap<>();

        // TV apps use LEANBACK_LAUNCHER; phone-style apps use LAUNCHER. Check TV first so its banner wins.
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
            for (ResolveInfo ri : found) {
                ActivityInfo ai = ri.activityInfo;
                String pkg = ai.packageName;
                if (pkg.equals(c.getPackageName()) || pkg.equals(AMAZON_LAUNCHER) || byPkg.containsKey(pkg)) {
                    continue;
                }
                AppEntry e = new AppEntry();
                e.pkg = pkg;
                e.label = String.valueOf(ri.loadLabel(pm));
                e.launch = new Intent(Intent.ACTION_MAIN)
                        .addCategory(category)
                        .setComponent(new ComponentName(pkg, ai.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                e.banner = loadBanner(pm, new ComponentName(pkg, ai.name));
                if (e.banner == null) {
                    e.icon = ri.loadIcon(pm);
                    e.tint = cardColour(e.icon);
                }
                byPkg.put(pkg, e);
            }
        }

        List<AppEntry> list = new ArrayList<>(byPkg.values());
        Collections.sort(list, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return list;
    }

    private static Drawable loadBanner(PackageManager pm, ComponentName cn) {
        try {
            Drawable d = pm.getActivityBanner(cn);
            if (d != null) return d;
        } catch (Exception ignored) {
        }
        try {
            return pm.getApplicationBanner(cn.getPackageName());
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Average colour of the icon, darkened so white text and the icon stay readable on it. */
    private static int cardColour(Drawable icon) {
        int fallback = Color.rgb(42, 47, 60);
        if (icon == null) return fallback;
        try {
            Bitmap b = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            icon.setBounds(0, 0, 8, 8);
            icon.draw(canvas);
            long r = 0, g = 0, bl = 0;
            int n = 0;
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    int p = b.getPixel(x, y);
                    if (Color.alpha(p) < 128) continue;
                    r += Color.red(p);
                    g += Color.green(p);
                    bl += Color.blue(p);
                    n++;
                }
            }
            b.recycle();
            if (n == 0) return fallback;
            float[] hsv = new float[3];
            Color.RGBToHSV((int) (r / n), (int) (g / n), (int) (bl / n), hsv);
            hsv[1] = Math.min(hsv[1], 0.65f);
            hsv[2] = Math.min(Math.max(hsv[2] * 0.55f, 0.18f), 0.42f);
            return Color.HSVToColor(hsv);
        } catch (Exception e) {
            return fallback;
        }
    }
}
