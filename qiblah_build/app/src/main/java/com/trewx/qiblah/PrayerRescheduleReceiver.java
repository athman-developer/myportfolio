package com.trewx.qiblah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerRescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PrayerScheduler.scheduleFromSaved(context);
    }
}
