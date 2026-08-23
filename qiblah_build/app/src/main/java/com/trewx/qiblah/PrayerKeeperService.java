package com.trewx.qiblah;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

public class PrayerKeeperService extends Service {
    private static final String CHANNEL_ID = "prayer_keeper_service_v1";
    private static final int NOTIFICATION_ID = 7088;
    private static final int RESTART_REQUEST = 69003;

    public static void start(Context context) {
        if (context == null) return;
        Intent i = new Intent(context.getApplicationContext(), PrayerKeeperService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getApplicationContext().startForegroundService(i);
            } else {
                context.getApplicationContext().startService(i);
            }
        } catch (Throwable ignored) { }
    }

    public static void stop(Context context) {
        if (context == null) return;
        cancelRestart(context.getApplicationContext());
        try {
            context.getApplicationContext().stopService(
                    new Intent(context.getApplicationContext(), PrayerKeeperService.class));
        } catch (Throwable ignored) { }
    }

    @Override public void onCreate() {
        super.onCreate();
        startInForeground();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startInForeground();
        return START_STICKY;
    }

    private void startInForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Prayer reminder service",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Keeps Qibla Compass prayer reminders active when the app is closed");
                channel.setSound(null, null);
                channel.enableVibration(false);
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }
        }

        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra("open_prayers", true);
        PendingIntent openPi = PendingIntent.getActivity(this, 7089, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Prayer reminders active")
                .setContentText("Qibla Compass can remind you even when the app is closed")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(Notification.PRIORITY_LOW)
                .setColor(Color.rgb(255, 116, 23))
                .setContentIntent(openPi);

        try { startForeground(NOTIFICATION_ID, b.build()); } catch (Throwable ignored) { }
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (PrayerScheduler.isEnabled(this)) scheduleRestart(this, 2000L);
    }

    @Override public void onDestroy() {
        if (PrayerScheduler.isEnabled(this)) scheduleRestart(this, 2500L);
        super.onDestroy();
    }

    private static void scheduleRestart(Context context, long delay) {
        try {
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarm == null) return;
            Intent restart = new Intent(context, PrayerKeeperService.class);
            PendingIntent pi;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pi = PendingIntent.getForegroundService(context, RESTART_REQUEST, restart,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pi = PendingIntent.getService(context, RESTART_REQUEST, restart,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }
            long when = System.currentTimeMillis() + Math.max(1500L, delay);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                } else {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                }
            } else {
                alarm.setExact(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (Throwable ignored) { }
    }

    private static void cancelRestart(Context context) {
        try {
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarm == null) return;
            Intent restart = new Intent(context, PrayerKeeperService.class);
            PendingIntent pi;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pi = PendingIntent.getForegroundService(context, RESTART_REQUEST, restart,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pi = PendingIntent.getService(context, RESTART_REQUEST, restart,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            }
            if (pi != null) alarm.cancel(pi);
        } catch (Throwable ignored) { }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
