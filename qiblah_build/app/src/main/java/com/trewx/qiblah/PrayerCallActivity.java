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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int notificationId = -1;
    private String prayerName = "Prayer";
    private String currentStyle;

    private TextView sliderHandle;
    private FrameLayout sliderTrack;
    private View pulseTarget;
    private boolean dragging = false;
    private float downRawX;
    private float startTranslation;

    private final Runnable hintPulse = new Runnable() {
        @Override public void run() {
            if (isFinishing()) return;
            if (sliderHandle != null && !dragging) {
                sliderHandle.animate().cancel();
                sliderHandle.animate().translationX(dp(12)).scaleX(1.035f).scaleY(1.035f)
                        .setDuration(340).withEndAction(() -> {
                            if (sliderHandle != null && !dragging && !isFinishing()) {
                                sliderHandle.animate().translationX(0f).scaleX(1f).scaleY(1f).setDuration(250).start();
                            }
                        }).start();
            } else if (pulseTarget != null) {
                pulseTarget.animate().cancel();
                pulseTarget.animate().scaleX(1.08f).scaleY(1.08f).setDuration(340)
                        .withEndAction(() -> {
                            if (pulseTarget != null && !isFinishing()) pulseTarget.animate().scaleX(1f).scaleY(1f).setDuration(250).start();
                        }).start();
            }
            handler.postDelayed(this, 950L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Keep these legacy flags as a best-effort fallback on OEM lock screens/AOD, even on
        // newer Android versions. setShowWhenLocked/setTurnScreenOn below are the modern path.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(0);

        String p = getIntent().getStringExtra("prayer");
        if (p != null && !p.trim().isEmpty()) prayerName = p;
        notificationId = getIntent().getIntExtra("notification_id", -1);
        currentStyle = CallStylePrefs.getStyle(this);
        buildUiForStyle();
        handler.postDelayed(hintPulse, 520L);
        handler.postDelayed(this::dismissCall, 90000L);
    }

    @Override protected void onResume() {
        super.onResume();
        String latest = CallStylePrefs.getStyle(this);
        if (currentStyle != null && !currentStyle.equals(latest)) {
            currentStyle = latest;
            buildUiForStyle();
            handler.removeCallbacks(hintPulse);
            handler.postDelayed(hintPulse, 420L);
        }
    }

    private void buildUiForStyle() {
        sliderHandle = null;
        sliderTrack = null;
        pulseTarget = null;
        CallTheme theme = CallStylePrefs.theme(currentStyle);
        getWindow().setStatusBarColor(theme.background);
        getWindow().setNavigationBarColor(theme.background);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(theme.background);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(26), dp(20), dp(26), dp(26));
        root.addView(body, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View topSpacer = new View(this);
        body.addView(topSpacer, new LinearLayout.LayoutParams(1, 0, .17f));

        if (theme.avatar) {
            TextView avatar = label("الله", 46, theme.text, true);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(round(theme.accentSoft, dp(72)));
            LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(138), dp(138));
            body.addView(avatar, avatarLp);
        }

        TextView caller = label("الله", theme.allahSp, theme.text, false);
        caller.setGravity(Gravity.CENTER);
        caller.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        LinearLayout.LayoutParams callerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (theme.avatar) callerLp.topMargin = dp(16);
        body.addView(caller, callerLp);

        TextView prayerLabel = label(arabicPrayer(prayerName) + "   •   " + prayerName + " prayer", 18, theme.subtext, false);
        prayerLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams prayerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        prayerLp.topMargin = dp(6);
        body.addView(prayerLabel, prayerLp);

        if (theme.subtitle != null && !theme.subtitle.isEmpty()) {
            TextView sub = label(theme.subtitle, 12, theme.subtext, false);
            sub.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = dp(7);
            body.addView(sub, slp);
        }

        View middle = new View(this);
        body.addView(middle, new LinearLayout.LayoutParams(1, 0, theme.circularControls ? .37f : .43f));

        TextView reminder = label(theme.reminderLabel, 15, theme.text, true);
        reminder.setGravity(Gravity.CENTER);
        reminder.setBackground(round(theme.pill, dp(30)));
        body.addView(reminder, new LinearLayout.LayoutParams(dp(230), dp(56)));

        View lower = new View(this);
        body.addView(lower, new LinearLayout.LayoutParams(1, 0, .11f));

        if (theme.circularControls) buildCircleControls(body, theme);
        else buildSliderControls(body, theme);

        TextView style = label("STYLE", 11, theme.subtext, true);
        style.setGravity(Gravity.CENTER);
        style.setPadding(dp(15), dp(9), dp(15), dp(9));
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        st.topMargin = dp(12);
        body.addView(style, st);
        style.setOnClickListener(v -> startActivity(new Intent(this, CallStyleSettingsActivity.class)));
    }

    private void buildSliderControls(LinearLayout body, CallTheme theme) {
        sliderTrack = new FrameLayout(this);
        sliderTrack.setBackground(round(theme.track, dp(52)));
        body.addView(sliderTrack, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));

        TextView decline = label("Decline", 17, theme.text, false);
        decline.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        decline.setPadding(dp(29), 0, 0, 0);
        sliderTrack.addView(decline, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView answer = label("Answer", 17, theme.text, false);
        answer.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        answer.setPadding(0, 0, dp(29), 0);
        sliderTrack.addView(answer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        sliderHandle = label("☎", 34, theme.answer, true);
        sliderHandle.setGravity(Gravity.CENTER);
        sliderHandle.setBackground(round(theme.handle, dp(50)));
        sliderHandle.setElevation(dp(6));
        sliderTrack.addView(sliderHandle, new FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER));
        sliderHandle.setOnTouchListener(this::handleSliderTouch);

        TextView hint = label("Slide right to answer  •  left to decline", 12, theme.subtext, false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(12);
        body.addView(hint, hp);
    }

    private void buildCircleControls(LinearLayout body, CallTheme theme) {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        body.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(126)));

        TextView decline = circleButton("✕", "Decline", theme.decline, theme.text);
        TextView answer = circleButton("☎", "Answer", theme.answer, theme.text);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(120), 1f);
        controls.addView(decline, cp);
        controls.addView(answer, cp);
        decline.setOnClickListener(v -> dismissCall());
        answer.setOnClickListener(v -> answerCall());
        pulseTarget = answer;
    }

    private TextView circleButton(String icon, String caption, int color, int textColor) {
        TextView v = label(icon + "\n" + caption, 18, textColor, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(round(color, dp(62)));
        LinearLayout.LayoutParams inner = new LinearLayout.LayoutParams(dp(112), dp(112));
        v.setLayoutParams(inner);
        return v;
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
                if (x >= max * .56f) {
                    sliderHandle.animate().translationX(max).setDuration(120).withEndAction(this::answerCall).start();
                } else if (x <= -max * .56f) {
                    sliderHandle.animate().translationX(-max).setDuration(120).withEndAction(this::dismissCall).start();
                } else {
                    sliderHandle.animate().translationX(0f).setDuration(220).start();
                    handler.postDelayed(hintPulse, 650L);
                }
                return true;
            default: return false;
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
        if (pulseTarget != null) pulseTarget.animate().cancel();
        super.onDestroy();
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    private String arabicPrayer(String prayer) {
        if (prayer == null) return "الصلاة";
        switch (prayer.toLowerCase()) {
            case "fajr": return "الفجر";
            case "dhuhr": return "الظهر";
            case "asr": return "العصر";
            case "maghrib": return "المغرب";
            case "isha": return "العشاء";
            default: return "الصلاة";
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class CallTheme {
        final int background, text, subtext, answer, decline, track, handle, pill, accentSoft, allahSp;
        final boolean circularControls, avatar;
        final String subtitle, reminderLabel;
        CallTheme(int bg, int text, int sub, int answer, int decline, int track, int handle, int pill,
                  int soft, int allahSp, boolean circles, boolean avatar, String subtitle, String reminderLabel) {
            this.background=bg;this.text=text;this.subtext=sub;this.answer=answer;this.decline=decline;
            this.track=track;this.handle=handle;this.pill=pill;this.accentSoft=soft;this.allahSp=allahSp;
            this.circularControls=circles;this.avatar=avatar;this.subtitle=subtitle;this.reminderLabel=reminderLabel;
        }
    }

    private static final class CallStylePrefs {
        static String getStyle(Context c) { return c.getSharedPreferences("call_styles", MODE_PRIVATE).getString("style", "islamic"); }
        static CallTheme theme(String s) {
            if (s == null) s="islamic";
            switch (s) {
                case "iphone": return new CallTheme(Color.rgb(8,8,10), Color.WHITE, Color.rgb(190,190,195), Color.rgb(47,204,113), Color.rgb(255,69,58), Color.rgb(40,40,44), Color.WHITE, Color.rgb(28,28,31), Color.rgb(28,28,31), 100, true, false, "Incoming prayer reminder", "Prayer reminder");
                case "whatsapp": return new CallTheme(Color.rgb(11,29,27), Color.WHITE, Color.rgb(190,211,205), Color.rgb(37,211,102), Color.rgb(230,70,70), Color.rgb(31,63,58), Color.WHITE, Color.rgb(24,52,48), Color.rgb(28,71,64), 96, true, true, "Incoming prayer reminder", "WhatsApp-inspired prayer call");
                case "pixel": return new CallTheme(Color.rgb(26,28,40), Color.rgb(235,236,244), Color.rgb(193,194,205), Color.rgb(84,184,116), Color.rgb(220,72,72), Color.rgb(54,58,70), Color.WHITE, Color.rgb(62,66,78), Color.rgb(62,66,78), 96, false, false, "Incoming prayer reminder", "Prayer reminder");
                case "samsung": return new CallTheme(Color.rgb(24,31,50), Color.WHITE, Color.rgb(190,201,222), Color.rgb(63,201,104), Color.rgb(235,75,88), Color.rgb(42,54,82), Color.WHITE, Color.rgb(48,62,91), Color.rgb(50,67,99), 100, true, false, "Incoming prayer reminder", "Prayer reminder");
                case "minimal": return new CallTheme(Color.BLACK, Color.WHITE, Color.rgb(175,175,175), Color.rgb(38,190,100), Color.rgb(214,64,64), Color.rgb(35,35,35), Color.WHITE, Color.rgb(25,25,25), Color.rgb(25,25,25), 112, false, false, "", "Prayer reminder");
                case "orange": return new CallTheme(Color.rgb(22,20,18), Color.WHITE, Color.rgb(205,194,185), Color.rgb(255,116,23), Color.rgb(219,65,55), Color.rgb(63,48,38), Color.WHITE, Color.rgb(73,48,32), Color.rgb(85,52,31), 98, false, false, "Qibla Compass", "Prayer reminder");
                case "islamic":
                default: return new CallTheme(Color.rgb(8,38,34), Color.rgb(250,243,215), Color.rgb(198,210,194), Color.rgb(18,153,105), Color.rgb(194,61,61), Color.rgb(26,65,57), Color.rgb(252,247,226), Color.rgb(20,61,52), Color.rgb(31,80,67), 108, true, false, "ذكر الله • Prayer time", "✦  Prayer reminder  ✦");
            }
        }
    }
}
