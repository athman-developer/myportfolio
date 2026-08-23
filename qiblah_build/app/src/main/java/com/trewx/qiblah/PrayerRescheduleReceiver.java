package com.trewx.qiblah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerRescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                PrayerScheduler.scheduleFromSaved(app);
            } catch (Throwable ignored) {
            } finally {
                pendingResult.finish();
            }
        }, "Qibla-PrayerReschedule").start();
    }
}
