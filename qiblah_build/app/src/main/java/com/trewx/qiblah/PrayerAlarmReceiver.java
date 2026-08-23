package com.trewx.qiblah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "prayer_call_reminders_v2";

    @Override public void onReceive(Context context, Intent intent) {
        final String prayerValue = intent != null ? intent.getStringExtra("prayer") : null;
        final String prayer = (prayerValue == null || prayerValue.trim().isEmpty()) ? "Prayer" : prayerValue;
        final Context app = context.getApplicationContext();
        final PendingResult pendingResult = goAsync();

        new Thread(() -> {
            PowerManager.WakeLock wakeLock = null;
            try {
                PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "QiblaCompass:PrayerReminder");
                    wakeLock.setReferenceCounted(false);
                    wakeLock.acquire(15000L);
                }
                showPrayerCall(app, prayer);
                // Refresh the rolling prayer schedule without needing the app to be opened.
                PrayerScheduler.scheduleFromSaved(app);
            } catch (Throwable ignored) {
            } finally {
                try {
                    if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
                } catch (Throwable ignored) { }
                pendingResult.finish();
            }
        }, "Qibla-PrayerAlarm").start();
    }

    public static void showPrayerCall(Context context, String prayer) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        createChannel(context, nm);

        int notificationId = 7100 + Math.abs(prayer.hashCode() % 700);
        Intent callIntent = new Intent(context, PrayerCallActivity.class);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        callIntent.putExtra("prayer", prayer);
        callIntent.putExtra("notification_id", notificationId);
        PendingIntent callPi = PendingIntent.getActivity(context, notificationId, callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dismissIntent = new Intent(context, PrayerActionReceiver.class);
        dismissIntent.setAction("com.trewx.qiblah.DISMISS_PRAYER_CALL");
        dismissIntent.putExtra("notification_id", notificationId);
        PendingIntent dismissPi = PendingIntent.getBroadcast(context, notificationId + 1000, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        b.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("الله")
                .setContentText(prayer + " prayer time")
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(Color.rgb(255, 116, 23))
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(callPi)
                .setFullScreenIntent(callPi, true)
                .setTimeoutAfter(90000L);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Person caller = new Person.Builder().setName("الله").setImportant(true).build();
            b.setStyle(Notification.CallStyle.forIncomingCall(caller, dismissPi, callPi));
        } else {
            b.addAction(new Notification.Action.Builder(0, "DISMISS", dismissPi).build());
            b.addAction(new Notification.Action.Builder(0, "ANSWER", callPi).build());
        }

        try { nm.notify(notificationId, b.build()); } catch (SecurityException ignored) { }
    }

    public static void createChannel(Context context, NotificationManager nm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
        if (existing != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Prayer call reminders",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Prayer reminders that ring and appear even when Qibla Compass is closed");
        channel.enableLights(true);
        channel.setLightColor(Color.rgb(255, 116, 23));
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 350, 500, 350, 900});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setBypassDnd(false);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(sound, attrs);
        nm.createNotificationChannel(channel);
    }
}
