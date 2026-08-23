package com.trewx.qiblah;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/** Reusable feedback for buttons that start work which takes time. */
public final class ActionFeedback {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ActionFeedback() { }

    public static void waiting(Activity activity, Button button, TextView status,
                               String message, String waitingLabel,
                               String restoreLabel, int seconds) {
        if (activity == null) return;
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
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
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
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