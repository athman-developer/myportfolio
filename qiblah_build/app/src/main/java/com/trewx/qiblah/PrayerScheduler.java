package com.trewx.qiblah;

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

public final class PrayerScheduler {
    static final String PREFS = "prayer_reminders";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_LAT = "lat";
    static final String KEY_LON = "lon";
    static final String KEY_LAST_LOCATION_UPDATE = "last_location_update";

    private static final int DAYS_AHEAD = 10;
    private static final int MAINTENANCE_REQUEST = 69001;
    private static final int TEST_REQUEST = 69002;
    private static final String ACTION_PRAYER = "com.trewx.qiblah.PRAYER_ALARM";
    private static final String ACTION_MAINTENANCE = "com.trewx.qiblah.PRAYER_MAINTENANCE";

    private PrayerScheduler() { }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

    public static void enable(Context context, double lat, double lon) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, true)
                .putLong(KEY_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LON, Double.doubleToRawLongBits(lon))
                .putLong(KEY_LAST_LOCATION_UPDATE, System.currentTimeMillis())
                .apply();
        PrayerKeeperService.start(context);
        scheduleFromSaved(context);
    }

    public static void disable(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, false)
                .apply();
        cancelUpcoming(context);
        cancelMaintenance(context);
        PrayerKeeperService.stop(context);
    }

    public static void onLocationUpdate(Context context, Location location) {
        if (location == null || !isEnabled(context)) return;
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        double oldLat = Double.longBitsToDouble(
                p.getLong(KEY_LAT, Double.doubleToRawLongBits(location.getLatitude())));
        double oldLon = Double.longBitsToDouble(
                p.getLong(KEY_LON, Double.doubleToRawLongBits(location.getLongitude())));
        long last = p.getLong(KEY_LAST_LOCATION_UPDATE, 0L);
        float[] distance = new float[1];
        Location.distanceBetween(oldLat, oldLon, location.getLatitude(), location.getLongitude(), distance);
        boolean meaningfulMove = distance[0] > 10000f;
        boolean stale = System.currentTimeMillis() - last > 6L * 60L * 60L * 1000L;
        if (!meaningfulMove && !stale) return;
        p.edit()
                .putLong(KEY_LAT, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(KEY_LON, Double.doubleToRawLongBits(location.getLongitude()))
                .putLong(KEY_LAST_LOCATION_UPDATE, System.currentTimeMillis())
                .apply();
        scheduleFromSaved(context);
    }

    public static double[] savedLocation(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.contains(KEY_LAT) || !p.contains(KEY_LON)) return null;
        return new double[]{
                Double.longBitsToDouble(p.getLong(KEY_LAT, 0L)),
                Double.longBitsToDouble(p.getLong(KEY_LON, 0L))
        };
    }

    public static void scheduleFromSaved(Context context) {
        if (!isEnabled(context)) return;
        double[] ll = savedLocation(context);
        if (ll == null) return;
        scheduleUpcoming(context, ll[0], ll[1]);
        scheduleDailyMaintenance(context);
        PrayerKeeperService.start(context);
    }

    public static void scheduleUpcoming(Context context, double lat, double lon) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;

        TimeZone tz = TimeZone.getDefault();
        Calendar today = Calendar.getInstance(tz);
        today.set(Calendar.HOUR_OF_DAY, 12);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (int d = 0; d < DAYS_AHEAD; d++) {
            Calendar date = (Calendar) today.clone();
            date.add(Calendar.DAY_OF_MONTH, d);
            PrayerTimes.DayTimes times = PrayerTimes.calculate(date.getTime(), lat, lon, tz);
            int index = 0;
            for (Map.Entry<String, Calendar> entry : times.obligatory().entrySet()) {
                scheduleOne(context, alarm, entry.getKey(), entry.getValue(), index++);
            }
        }
    }

    private static void scheduleOne(Context context, AlarmManager alarm, String prayer,
                                    Calendar when, int prayerIndex) {
        long trigger = when.getTimeInMillis();
        if (trigger <= System.currentTimeMillis() + 5000L) return;

        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.setAction(ACTION_PRAYER);
        intent.putExtra("prayer", prayer);
        intent.putExtra("time", trigger);
        int requestCode = requestCode(when, prayerIndex);
        PendingIntent operation = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent showIntent = prayerScreenIntent(context, requestCode + 200000);
        scheduleWakeAlarm(context, alarm, trigger, operation, showIntent);
    }

    private static void scheduleWakeAlarm(Context context, AlarmManager alarm, long trigger,
                                          PendingIntent operation, PendingIntent showIntent) {
        try {
            boolean exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    || alarm.canScheduleExactAlarms();

            if (exactAllowed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(trigger, showIntent), operation);
                } else {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, trigger, operation);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation);
            } else {
                alarm.set(AlarmManager.RTC_WAKEUP, trigger, operation);
            }
        } catch (SecurityException e) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation);
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, trigger, operation);
                }
            } catch (Throwable ignored) { }
        } catch (Throwable ignored) { }
    }

    private static Bundle pendingIntentCreatorOptions() {
        if (Build.VERSION.SDK_INT < 34) return null;
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            return options.toBundle();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static PendingIntent prayerScreenIntent(Context context, int requestCode) {
        Intent show = new Intent(context, PrayerCallActivity.class);
        show.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        show.putExtra("prayer", "Prayer");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Bundle options = pendingIntentCreatorOptions();
        if (options != null) {
            return PendingIntent.getActivity(context, requestCode, show, flags, options);
        }
        return PendingIntent.getActivity(context, requestCode, show, flags);
    }

    public static void scheduleTest(Context context, long delayMillis) {
        PrayerKeeperService.start(context);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        long actualDelay = Math.max(15000L, delayMillis);
        long trigger = System.currentTimeMillis() + actualDelay;
        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.setAction(ACTION_PRAYER + ".TEST");
        intent.putExtra("prayer", "Background test");
        intent.putExtra("time", trigger);
        PendingIntent operation = PendingIntent.getBroadcast(context, TEST_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        scheduleWakeAlarm(context, alarm, trigger, operation,
                prayerScreenIntent(context, TEST_REQUEST + 200000));

        long seconds = Math.max(1L, Math.round(actualDelay / 1000.0));
        ActionFeedback.notifyWaiting(context,
                "Background reminder test scheduled",
                "Button received. Please wait " + seconds + " seconds. You can close Qibla Compass or lock the screen now.");
    }

    public static void scheduleDailyMaintenance(Context context) {
        if (!isEnabled(context)) return;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;

        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 5);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(context, PrayerRescheduleReceiver.class);
        intent.setAction(ACTION_MAINTENANCE);
        PendingIntent pi = PendingIntent.getBroadcast(context, MAINTENANCE_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long trigger = next.getTimeInMillis();
        try {
            boolean exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    || alarm.canScheduleExactAlarms();
            if (exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else if (exactAllowed) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                alarm.set(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        } catch (Throwable ignored) {
            try { alarm.set(AlarmManager.RTC_WAKEUP, trigger, pi); } catch (Throwable ignoredAgain) { }
        }
    }

    private static int requestCode(Calendar when, int prayerIndex) {
        int year = when.get(Calendar.YEAR) % 100;
        int day = when.get(Calendar.DAY_OF_YEAR);
        return year * 10000 + day * 10 + prayerIndex;
    }

    private static void cancelMaintenance(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        Intent intent = new Intent(context, PrayerRescheduleReceiver.class);
        intent.setAction(ACTION_MAINTENANCE);
        PendingIntent pi = PendingIntent.getBroadcast(context, MAINTENANCE_REQUEST, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) alarm.cancel(pi);
    }

    private static void cancelUpcoming(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        TimeZone tz = TimeZone.getDefault();
        Calendar base = Calendar.getInstance(tz);
        for (int d = -1; d < DAYS_AHEAD + 2; d++) {
            Calendar date = (Calendar) base.clone();
            date.add(Calendar.DAY_OF_MONTH, d);
            for (int i = 0; i < 5; i++) {
                Intent intent = new Intent(context, PrayerAlarmReceiver.class);
                intent.setAction(ACTION_PRAYER);
                PendingIntent pi = PendingIntent.getBroadcast(context, requestCode(date, i), intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) alarm.cancel(pi);
            }
        }
    }
}
