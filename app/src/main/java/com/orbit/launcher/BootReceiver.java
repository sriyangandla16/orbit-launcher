package com.orbit.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Opens our home screen once the stick finishes starting up. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!Prefs.takeover(context) || !HomeWatcherService.isEnabled(context)) return;
        try {
            context.startActivity(new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
    }
}
