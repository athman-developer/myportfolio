package com.trewx.qiblah;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PrayerCallActivity extends Activity {
    private static final int CALL_BG = Color.rgb(26, 28, 40);
    private static final int TRACK_BG = Color.rgb(54, 58, 70);
    private static final int TEXT = Color.rgb(235, 236, 244);
    private static final int SUBTEXT = Color.rgb(193, 194, 205);
    private static final int GREEN = Color.rgb(84, 184, 116);
    private static final int PILL = Color.rgb(62, 66, 78);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int notificationId = -1;
    private TextView sliderHandle;
    private FrameLayout sliderTrack;
    private boolean dragging = false;
    private float downRawX;
    private float startTranslation;

    private final Runnable hintPulse = new Runnable() {
        @Override public void run() {
            if (sliderHandle != null && !dragging && !isFinishing()) {
                sliderHandle.animate().cancel();
                sliderHandle.animate()
                        .translationX(dp(11))
                        .scaleX(1.035f)
                        .scaleY(1.035f)
                        .setDuration(360)
                        .withEndAction(() -> {
                            if (sliderHandle != null && !dragging && !isFinishing()) {
                                sliderHandle.animate()
                                        .translationX(0f)
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(260)
                                        .start();
                            }
                        }).start();
            }
            handler.postDelayed(this, 900L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().setStatusBarColor(CALL_BG);
        getWindow().setNavigationBarColor(CALL_BG);
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        String prayer = getIntent().getStringExtra("prayer");
        if (prayer == null || prayer.trim().isEmpty()) prayer = "Prayer";
        notificationId = getIntent().getIntExtra("notification_id", -1);
        buildUi(prayer);
        handler.postDelayed(hintPulse, 520L);
        handler.postDelayed(this::dismissCall, 90000L);
    }

    private void buildUi(String prayer) {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(CALL_BG);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(28), dp(22), dp(28), dp(28));
        root.addView(body, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View topSpacer = new View(this);
        body.addView(topSpacer, new LinearLayout.LayoutParams(1, 0, 0.22f));

        TextView caller = label("الله", 58, TEXT, false);
        caller.setGravity(Gravity.CENTER);
        caller.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        body.addView(caller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView prayerLabel = label(prayer + " prayer", 20, SUBTEXT, false);
        prayerLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams prayerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        prayerLp.topMargin = dp(6);
        body.addView(prayerLabel, prayerLp);

        View middleSpacer = new View(this);
        body.addView(middleSpacer, new LinearLayout.LayoutParams(1, 0, 0.42f));

        TextView reminderPill = label("◯   Prayer reminder", 16, TEXT, true);
        reminderPill.setGravity(Gravity.CENTER);
        reminderPill.setBackground(round(PILL, dp(34)));
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(dp(210), dp(58));
        body.addView(reminderPill, pillLp);

        View lowerSpacer = new View(this);
        body.addView(lowerSpacer, new LinearLayout.LayoutParams(1, 0, 0.13f));

        sliderTrack = new FrameLayout(this);
        sliderTrack.setBackground(round(TRACK_BG, dp(52)));
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96));
        body.addView(sliderTrack, trackLp);

        TextView decline = label("Decline", 17, TEXT, false);
        decline.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        decline.setPadding(dp(30), 0, 0, 0);
        FrameLayout.LayoutParams declineLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        sliderTrack.addView(decline, declineLp);

        TextView answer = label("Answer", 17, TEXT, false);
        answer.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        answer.setPadding(0, 0, dp(30), 0);
        FrameLayout.LayoutParams answerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        sliderTrack.addView(answer, answerLp);

        sliderHandle = label("☎", 34, GREEN, true);
        sliderHandle.setGravity(Gravity.CENTER);
        sliderHandle.setBackground(round(Color.WHITE, dp(50)));
        sliderHandle.setElevation(dp(6));
        FrameLayout.LayoutParams handleLp = new FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER);
        sliderTrack.addView(sliderHandle, handleLp);
        sliderHandle.setOnTouchListener(this::handleSliderTouch);

        TextView hint = label("Slide right to answer  •  left to decline", 12, Color.rgb(156,158,170), false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(14);
        body.addView(hint, hintLp);
    }

    private boolean handleSliderTouch(View view, MotionEvent event) {
        if (sliderTrack == null || sliderHandle == null) return false;
        float max = Math.max(dp(72), (sliderTrack.getWidth() - sliderHandle.getWidth()) / 2f - dp(8));
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = true;
                handler.removeCallbacks(hintPulse);
                sliderHandle.animate().cancel();
                sliderHandle.setScaleX(1f);
                sliderHandle.setScaleY(1f);
                downRawX = event.getRawX();
                startTranslation = sliderHandle.getTranslationX();
                return true;
            case MotionEvent.ACTION_MOVE:
                float next = startTranslation + event.getRawX() - downRawX;
                next = Math.max(-max, Math.min(max, next));
                sliderHandle.setTranslationX(next);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float x = sliderHandle.getTranslationX();
                dragging = false;
                if (x >= max * 0.56f) {
                    sliderHandle.animate().translationX(max).setDuration(120)
                            .withEndAction(this::answerCall).start();
                } else if (x <= -max * 0.56f) {
                    sliderHandle.animate().translationX(-max).setDuration(120)
                            .withEndAction(this::dismissCall).start();
                } else {
                    sliderHandle.animate().translationX(0f).setDuration(220).start();
                    handler.postDelayed(hintPulse, 650L);
                }
                return true;
            default:
                return false;
        }
    }

    private void answerCall() {
        cancelNotification();
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra("open_prayers", true);
        startActivity(open);
        finish();
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void dismissCall() {
        cancelNotification();
        finish();
    }

    private void cancelNotification() {
        if (notificationId < 0) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notificationId);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (sliderHandle != null) sliderHandle.animate().cancel();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
