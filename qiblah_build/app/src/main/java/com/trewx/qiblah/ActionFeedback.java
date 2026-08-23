package com.trewx.qiblah;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/** Reusable feedback for buttons that start work which takes time. */
public final class ActionFeedback {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String CHANNEL = "action_feedback";
    private static final int WAIT_NOTIFICATION_ID = 9321;

    private ActionFeedback() { }

    public static void notifyWaiting(Context context, String title, String message) {
        if (context == null) return;
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = nm.getNotificationChannel(CHANNEL);
                if (ch == null) {
                    ch = new NotificationChannel(CHANNEL, "Action confirmations", NotificationManager.IMPORTANCE_DEFAULT);
                    ch.setDescription("Immediate confirmations when an action has started and needs time to complete");
                    nm.createNotificationChannel(ch);
                }
            }
            Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(context, CHANNEL)
                    : new Notification.Builder(context);
            b.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new Notification.BigTextStyle().bigText(message))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setPriority(Notification.PRIORITY_DEFAULT);
            nm.notify(WAIT_NOTIFICATION_ID, b.build());
        } catch (Throwable ignored) { }
    }

    public static void waiting(Activity activity, Button button, TextView status,
                               String message, String waitingLabel,
                               String restoreLabel, int seconds) {
        if (activity == null) return;
        notifyWaiting(activity, "Action started", message);
        if (status != null) status.setText(message);
        if (button == null) return;

        button.setEnabled(false);
        button.setAlpha(.78f);
        final int total = Math.max(1, seconds);
        final int[] left = {total};
        final Runnable[] ticker = new Runnable[1];
        ticker[0] = new Runnable() {
            @Override public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (left[0] <= 0) {
                    button.setText(restoreLabel);
                    button.setEnabled(true);
                    button.setAlpha(1f);
                    return;
                }
                button.setText(waitingLabel + " • " + left[0] + "s");
                left[0]--;
                MAIN.postDelayed(this, 1000L);
            }
        };
        MAIN.post(ticker[0]);
    }

    public static void waiting(Activity activity, Button button, TextView status,
                               String message, String waitingLabel,
                               String restoreLabel, long minimumWaitMs) {
        if (activity == null) return;
        notifyWaiting(activity, "Action started", message);
        if (status != null) status.setText(message);
        if (button == null) return;

        button.setEnabled(false);
        button.setAlpha(.78f);
        button.setText(waitingLabel);
        MAIN.postDelayed(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                button.setText(restoreLabel);
                button.setEnabled(true);
                button.setAlpha(1f);
            }
        }, Math.max(800L, minimumWaitMs));
    }
}