package com.trewx.qiblah;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class PrayerDashboardView extends ScrollView {
    private static final int ORANGE = Color.rgb(255, 116, 23);
    private static final int INK = Color.rgb(24, 24, 24);
    private static final int MUTED = Color.rgb(105, 105, 105);
    private static final int GREEN = Color.rgb(15, 138, 95);
    private static final int CREAM = Color.rgb(248, 247, 242);
    private static final int REQ_NOTIFICATIONS = 86;

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinkedHashMap<String, TextView> timeViews = new LinkedHashMap<>();
    private TextView locationStatus;
    private TextView nextPrayer;
    private TextView reminderStatus;
    private Button enableButton;
    private Location currentLocation;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateDisplay(false);
            handler.postDelayed(this, 30000L);
        }
    };

    public PrayerDashboardView(Activity activity) {
        super(activity);
        this.activity = activity;
        setFillViewport(true);
        setBackgroundColor(CREAM);
        buildUi();
    }

    private void buildUi() {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(28));
        addView(box, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Prayer reminders", 29, INK, true);
        box.addView(title);
        TextView intro = text("Daily prayer times follow your date and GPS location. Qibla Compass recalculates them automatically as the days change.", 15, MUTED, false);
        intro.setPadding(0, dp(7), 0, dp(15));
        box.addView(intro);

        LinearLayout hero = card();
        box.addView(hero, spaced());
        TextView caller = text("الله", 38, INK, true);
        caller.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(caller);
        TextView callerSub = text("CALL-STYLE PRAYER REMINDER", 12, ORANGE, true);
        callerSub.setLetterSpacing(.12f);
        callerSub.setGravity(Gravity.CENTER_HORIZONTAL);
        callerSub.setPadding(0, dp(4), 0, 0);
        hero.addView(callerSub);
        TextView desc = text("At prayer time the phone can show a full-screen incoming-call-style reminder with الله as the caller, ringtone and vibration.", 14, MUTED, false);
        desc.setGravity(Gravity.CENTER_HORIZONTAL);
        desc.setPadding(dp(3), dp(10), dp(3), 0);
        hero.addView(desc);

        nextPrayer = text("Next prayer: calculating…", 20, INK, true);
        nextPrayer.setPadding(0, dp(15), 0, dp(8));
        box.addView(nextPrayer);

        locationStatus = text("Location: checking…", 13, MUTED, false);
        box.addView(locationStatus);

        LinearLayout timesCard = card();
        LinearLayout.LayoutParams timesLp = spaced();
        timesLp.topMargin = dp(14);
        box.addView(timesCard, timesLp);
        addPrayerRow(timesCard, "Fajr");
        addPrayerRow(timesCard, "Dhuhr");
        addPrayerRow(timesCard, "Asr");
        addPrayerRow(timesCard, "Maghrib");
        addPrayerRow(timesCard, "Isha");

        TextView method = text("Calculation: Muslim World League (Fajr 18°, Isha 17°) • standard/Shafi'i Asr. Local mosque timetables can differ by a few minutes.", 12, MUTED, false);
        method.setPadding(dp(3), dp(10), dp(3), dp(14));
        box.addView(method);

        reminderStatus = text("Prayer calls are OFF", 15, MUTED, true);
        reminderStatus.setPadding(0, dp(4), 0, dp(8));
        box.addView(reminderStatus);

        enableButton = button("ENABLE PRAYER CALLS", INK);
        enableButton.setOnClickListener(v -> toggleReminders());
        box.addView(enableButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        Button permissions = button("CHECK ALARM PERMISSIONS", Color.rgb(82,82,82));
        LinearLayout.LayoutParams permLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        permLp.topMargin = dp(10);
        box.addView(permissions, permLp);
        permissions.setOnClickListener(v -> requestNextCapability());

        Button test = button("TEST CALL SCREEN", ORANGE);
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        testLp.topMargin = dp(10);
        box.addView(test, testLp);
        test.setOnClickListener(v -> {
            Intent i = new Intent(activity, PrayerCallActivity.class);
            i.putExtra("prayer", "Test");
            activity.startActivity(i);
        });

        TextView note = text("For reliable reminders, allow Notifications, Alarms & reminders, and Full-screen notifications when Android offers those permissions. Times are recalculated after restart, time-zone changes and app updates.", 12, MUTED, false);
        note.setPadding(dp(4), dp(13), dp(4), 0);
        box.addView(note);
    }

    private void addPrayerRow(LinearLayout parent, String name) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));
        TextView n = text(name, 17, INK, false);
        TextView t = text("--:--", 19, INK, true);
        t.setGravity(Gravity.END);
        row.addView(n, new LinearLayout.LayoutParams(0, dp(34), 1f));
        row.addView(t, new LinearLayout.LayoutParams(dp(100), dp(34)));
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        timeViews.put(name, t);
    }

    public void refresh() {
        currentLocation = bestLocation();
        if (currentLocation == null) {
            double[] saved = PrayerScheduler.savedLocation(activity);
            if (saved != null) {
                currentLocation = new Location("saved");
                currentLocation.setLatitude(saved[0]);
                currentLocation.setLongitude(saved[1]);
            }
        }
        updateDisplay(true);
    }

    private void updateDisplay(boolean scheduleIfEnabled) {
        if (currentLocation == null) currentLocation = bestLocation();
        if (currentLocation == null) {
            locationStatus.setText("Location unavailable — allow Location, then open Compass or Prayers again.");
            for (TextView t : timeViews.values()) t.setText("--:--");
            nextPrayer.setText("Next prayer: waiting for location");
            updateReminderUi();
            return;
        }

        double lat = currentLocation.getLatitude();
        double lon = currentLocation.getLongitude();
        locationStatus.setText(String.format(Locale.getDefault(), "Location %.3f°, %.3f° • %s", lat, lon, TimeZone.getDefault().getID()));

        TimeZone tz = TimeZone.getDefault();
        PrayerTimes.DayTimes day = PrayerTimes.calculate(new Date(), lat, lon, tz);
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        fmt.setTimeZone(tz);
        for (Map.Entry<String, Calendar> e : day.obligatory().entrySet()) {
            TextView t = timeViews.get(e.getKey());
            if (t != null) t.setText(fmt.format(e.getValue().getTime()));
        }

        Calendar now = Calendar.getInstance(tz);
        String nextName = null;
        Calendar nextAt = null;
        for (Map.Entry<String, Calendar> e : day.obligatory().entrySet()) {
            if (e.getValue().after(now)) {
                nextName = e.getKey();
                nextAt = e.getValue();
                break;
            }
        }
        if (nextAt == null) {
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            PrayerTimes.DayTimes nextDay = PrayerTimes.calculate(tomorrow.getTime(), lat, lon, tz);
            nextName = "Fajr";
            nextAt = nextDay.fajr;
        }
        long remaining = Math.max(0L, nextAt.getTimeInMillis() - now.getTimeInMillis());
        long hours = remaining / 3600000L;
        long minutes = (remaining % 3600000L) / 60000L;
        nextPrayer.setText(String.format(Locale.getDefault(), "Next: %s • %s • %dh %02dm", nextName, fmt.format(nextAt.getTime()), hours, minutes));

        if (scheduleIfEnabled && PrayerScheduler.isEnabled(activity)) {
            PrayerScheduler.onLocationUpdate(activity, currentLocation);
            PrayerScheduler.scheduleFromSaved(activity);
        }
        updateReminderUi();
    }

    private void updateReminderUi() {
        boolean enabled = PrayerScheduler.isEnabled(activity);
        enableButton.setText(enabled ? "TURN PRAYER CALLS OFF" : "ENABLE PRAYER CALLS");
        enableButton.setBackground(round(enabled ? Color.rgb(120, 45, 45) : INK, dp(18)));
        if (!enabled) {
            reminderStatus.setText("Prayer calls are OFF");
            reminderStatus.setTextColor(MUTED);
            return;
        }
        reminderStatus.setText(exactAlarmAvailable() ? "Prayer calls are ON • precise alarms ready" : "Prayer calls are ON • allow precise alarms for exact timing");
        reminderStatus.setTextColor(GREEN);
    }

    private void toggleReminders() {
        if (PrayerScheduler.isEnabled(activity)) {
            PrayerScheduler.disable(activity);
            updateReminderUi();
            return;
        }
        if (currentLocation == null) currentLocation = bestLocation();
        if (currentLocation == null) {
            locationStatus.setText("A location fix is needed before prayer reminders can be enabled.");
            return;
        }
        PrayerScheduler.enable(activity, currentLocation.getLatitude(), currentLocation.getLongitude());
        PrayerAlarmReceiver.createChannel(activity, (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE));
        updateReminderUi();
        requestNextCapability();
    }

    private void requestNextCapability() {
        if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (alarm != null && !alarm.canScheduleExactAlarms()) {
                try {
                    Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(i);
                    return;
                } catch (Throwable ignored) { }
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && !nm.canUseFullScreenIntent()) {
                try {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(i);
                    return;
                } catch (Throwable ignored) { }
            }
        }
        reminderStatus.setText(PrayerScheduler.isEnabled(activity) ? "Prayer calls are ON • permissions ready" : "Alarm permissions are ready");
        reminderStatus.setTextColor(GREEN);
        if (PrayerScheduler.isEnabled(activity)) PrayerScheduler.scheduleFromSaved(activity);
    }

    private boolean exactAlarmAvailable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        return alarm != null && alarm.canScheduleExactAlarms();
    }

    private Location bestLocation() {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null;
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return null;
        Location best = null;
        String[] providers = new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String p : providers) {
            try {
                Location candidate = lm.getLastKnownLocation(p);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) best = candidate;
            } catch (Throwable ignored) { }
        }
        return best;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(round(Color.WHITE, dp(22)));
        return card;
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(6);
        return p;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0f, 1.10f);
        return t;
    }

    private Button button(String label, int color) {
        Button b = new Button(activity);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(round(color, dp(18)));
        return b;
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
