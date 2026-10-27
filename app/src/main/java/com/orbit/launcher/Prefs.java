package com.orbit.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Small wrapper around SharedPreferences: favourites, hidden apps and Home-button settings. */
final class Prefs {

    static final int MAX_FAVOURITES = 4;

    private static final String FILE = "orbit";
    private static final String KEY_FAVOURITES = "favourites";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_TAKEOVER = "takeover";
    private static final String KEY_AGGRESSIVE = "aggressive";
    private static final String KEY_PAUSED_UNTIL = "paused_until";
    private static final String KEY_SCREENS = "seen_screens";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Returns null if favourites were never set (first run). */
    static List<String> favourites(Context c) {
        String raw = sp(c).getString(KEY_FAVOURITES, null);
        if (raw == null) return null;
        List<String> out = new ArrayList<>();
        for (String s : raw.split(",")) {
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    static void setFavourites(Context c, List<String> pkgs) {
        sp(c).edit().putString(KEY_FAVOURITES, TextUtils.join(",", pkgs)).apply();
    }

    static Set<String> hidden(Context c) {
        return new HashSet<>(sp(c).getStringSet(KEY_HIDDEN, Collections.<String>emptySet()));
    }

    static void setHidden(Context c, Set<String> pkgs) {
        sp(c).edit().putStringSet(KEY_HIDDEN, new HashSet<>(pkgs)).apply();
    }

    /** Whether the Home button should bring up this launcher instead of Amazon's. */
    static boolean takeover(Context c) {
        return sp(c).getBoolean(KEY_TAKEOVER, true);
    }

    static void setTakeover(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_TAKEOVER, on).apply();
    }

    /** false = only Amazon's main home screen is replaced; true = any Amazon launcher screen. */
    static boolean aggressive(Context c) {
        return sp(c).getBoolean(KEY_AGGRESSIVE, false);
    }

    static void setAggressive(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_AGGRESSIVE, on).apply();
    }

    /** Used by the "Amazon Home" button so you can visit Amazon's screen without being redirected. */
    static long pausedUntil(Context c) {
        return sp(c).getLong(KEY_PAUSED_UNTIL, 0);
    }

    static void setPausedUntil(Context c, long timeMillis) {
        sp(c).edit().putLong(KEY_PAUSED_UNTIL, timeMillis).apply();
    }

    /** Keeps the last few Amazon screen names seen, shown on the setup screen to help debugging. */
    static void logScreen(Context c, String className) {
        String raw = sp(c).getString(KEY_SCREENS, "");
        List<String> list = new ArrayList<>();
        for (String s : raw.split("\n")) {
            if (!s.isEmpty() && !s.equals(className)) list.add(s);
        }
        list.add(0, className);
        while (list.size() > 6) list.remove(list.size() - 1);
        sp(c).edit().putString(KEY_SCREENS, TextUtils.join("\n", list)).apply();
    }

    static String seenScreens(Context c) {
        return sp(c).getString(KEY_SCREENS, "");
    }
}
