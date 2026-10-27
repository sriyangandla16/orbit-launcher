package com.orbit.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * The Home-button trick.
 *
 * Fire OS always opens Amazon's home screen when you press Home and doesn't let you pick
 * another launcher. This accessibility service is told whenever a new screen opens.
 * When that screen is Amazon's home screen, it immediately opens our launcher on top.
 */
public class HomeWatcherService extends AccessibilityService {

    static final String TAG = "OrbitHome";
    private static final long DEBOUNCE_MS = 700;

    private long lastRedirect;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !AppRepository.AMAZON_LAUNCHER.contentEquals(pkg)) return;

        String screen = String.valueOf(event.getClassName());
        Log.i(TAG, "Amazon screen opened: " + screen);
        Prefs.logScreen(this, screen);

        if (!Prefs.takeover(this)) return;
        if (System.currentTimeMillis() < Prefs.pausedUntil(this)) return;   // "Amazon Home" button was used

        boolean isMainHome = screen.contains("HomeActivity");
        if (!isMainHome && !Prefs.aggressive(this)) return;

        long now = SystemClock.uptimeMillis();
        if (now - lastRedirect < DEBOUNCE_MS) return;
        lastRedirect = now;

        Log.i(TAG, "Redirecting to Orbit");
        Intent i = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(i);
        } catch (Exception e) {
            Log.w(TAG, "Could not open launcher", e);
        }
    }

    @Override
    public void onInterrupt() {
    }

    /** True if this service is switched on in the system's accessibility settings. */
    static boolean isEnabled(Context c) {
        String flat = new ComponentName(c, HomeWatcherService.class).flattenToString();
        String shortFlat = new ComponentName(c, HomeWatcherService.class).flattenToShortString();
        String enabled = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        for (String s : enabled.split(":")) {
            if (s.equalsIgnoreCase(flat) || s.equalsIgnoreCase(shortFlat)) return true;
        }
        return false;
    }
}
