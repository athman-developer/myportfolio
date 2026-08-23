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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
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
        dragging = false;

        if (CallStylePrefs.IPHONE.equals(currentStyle)) {
            buildCircleTheme(prayerName,
                    new int[]{Color.rgb(18,18,20), Color.rgb(31,31,35)},
                    Color.rgb(244,244,247), Color.rgb(178,178,186),
                    Color.rgb(52,199,89), Color.rgb(255,69,58),
                    "Incoming prayer reminder", "iPhone-inspired", 96, false);
        } else if (CallStylePrefs.WHATSAPP.equals(currentStyle)) {
            buildCircleTheme(prayerName,
                    new int[]{Color.rgb(3,25,20), Color.rgb(7,58,45)},
                    Color.WHITE, Color.rgb(190,213,205),
                    Color.rgb(37,211,102), Color.rgb(226,74,74),
                    "Incoming prayer reminder", "WhatsApp-inspired", 96, false);
        } else if (CallStylePrefs.SAMSUNG.equals(currentStyle)) {
            buildCircleTheme(prayerName,
                    new int[]{Color.rgb(16,21,44), Color.rgb(49,57,96)},
                    Color.rgb(244,246,255), Color.rgb(190,196,220),
                    Color.rgb(70,205,125), Color.rgb(235,83,94),
                    "Prayer reminder", "Samsung One UI-inspired", 98, true);
        } else if (CallStylePrefs.MINIMAL.equals(currentStyle)) {
            buildCircleTheme(prayerName,
                    new int[]{Color.BLACK, Color.rgb(9,9,9)},
                    Color.WHITE, Color.rgb(170,170,170),
                    Color.rgb(74,196,112), Color.rgb(222,72,72),
                    "", "Minimal", 112, false);
        } else if (CallStylePrefs.ISLAMIC.equals(currentStyle)) {
            buildSliderTheme(prayerName,
                    new int[]{Color.rgb(3,38,30), Color.rgb(7,70,53)},
                    Color.rgb(242,232,191), Color.rgb(208,222,213),
                    Color.rgb(200,163,70), Color.rgb(18,71,56),
                    "✦  PRAYER REMINDER  ✦", 108, true);
        } else if (CallStylePrefs.ORANGE.equals(currentStyle)) {
            buildSliderTheme(prayerName,
                    new int[]{Color.rgb(16,16,16), Color.rgb(58,29,10)},
                    Color.WHITE, Color.rgb(204,198,192),
                    Color.rgb(255,116,23), Color.rgb(67,54,47),
                    "QIBLA COMPASS • PRAYER REMINDER", 100, false);
        } else {
            buildSliderTheme(prayerName,
                    new int[]{Color.rgb(26,28,40), Color.rgb(30,32,45)},
                    Color.rgb(235,236,244), Color.rgb(193,194,205),
                    Color.rgb(84,184,116), Color.rgb(54,58,70),
                    "PRAYER REMINDER", 98, false);
        }
    }

    private FrameLayout createRoot(int[] bgColors) {
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, bgColors);
        root.setBackground(bg);
        setContentView(root);
        int barColor = bgColors[0];
        getWindow().setStatusBarColor(barColor);
        getWindow().setNavigationBarColor(barColor);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
        return root;
    }

    private void buildSliderTheme(String prayer, int[] bgColors, int textColor, int subColor,
                                  int accent, int trackColor, String reminderText,
                                  int allahSize, boolean islamic) {
        FrameLayout root = createRoot(bgColors);
        LinearLayout body = body(root);
        addStyleButton(root, textColor);

        View topSpacer = new View(this);
        body.addView(topSpacer, new LinearLayout.LayoutParams(1, 0, islamic ? .12f : .19f));

        if (islamic) {
            TextView ornament = label("✦     ✧     ✦", 17, accent, false);
            ornament.setGravity(Gravity.CENTER);
            body.addView(ornament, matchWrap());
        }

        TextView caller = label("الله", allahSize, islamic ? accent : textColor, false);
        caller.setGravity(Gravity.CENTER);
        caller.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        body.addView(caller, matchWrap());

        TextView prayerLabel = label(islamic ? arabicPrayer(prayer) + "  •  " + prayer : prayer + " prayer", 20, subColor, false);
        prayerLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams prayerLp = matchWrap();
        prayerLp.topMargin = dp(5);
        body.addView(prayerLabel, prayerLp);

        View middleSpacer = new View(this);
        body.addView(middleSpacer, new LinearLayout.LayoutParams(1, 0, islamic ? .28f : .40f));

        TextView reminderPill = label(reminderText, 13, textColor, true);
        reminderPill.setGravity(Gravity.CENTER);
        reminderPill.setBackground(round(trackColor, dp(30)));
        body.addView(reminderPill, new LinearLayout.LayoutParams(dp(islamic ? 250 : 235), dp(54)));

        View lowerSpacer = new View(this);
        body.addView(lowerSpacer, new LinearLayout.LayoutParams(1, 0, .14f));

        sliderTrack = new FrameLayout(this);
        sliderTrack.setBackground(round(trackColor, dp(52)));
        body.addView(sliderTrack, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));

        TextView decline = label("Decline", 17, textColor, false);
        decline.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        decline.setPadding(dp(29), 0, 0, 0);
        sliderTrack.addView(decline, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView answer = label("Answer", 17, textColor, false);
        answer.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        answer.setPadding(0, 0, dp(29), 0);
        sliderTrack.addView(answer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        sliderHandle = label("☎", 34, accent, true);
        sliderHandle.setGravity(Gravity.CENTER);
        sliderHandle.setBackground(round(Color.WHITE, dp(50)));
        sliderHandle.setElevation(dp(7));
        sliderTrack.addView(sliderHandle, new FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER));
        sliderHandle.setOnTouchListener(this::handleSliderTouch);

        TextView hint = label("Slide right to answer  •  left to decline", 12, subColor, false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = matchWrap();
        hintLp.topMargin = dp(13);
        body.addView(hint, hintLp);
    }

    private void buildCircleTheme(String prayer, int[] bgColors, int textColor, int subColor,
                                  int green, int red, String reminderText, String styleLabel,
                                  int allahSize, boolean samsungGlow) {
        FrameLayout root = createRoot(bgColors);
        LinearLayout body = body(root);
        addStyleButton(root, textColor);

        View topSpacer = new View(this);
        body.addView(topSpacer, new LinearLayout.LayoutParams(1, 0, .18f));

        TextView caller = label("الله", allahSize, textColor, false);
        caller.setGravity(Gravity.CENTER);
        body.addView(caller, matchWrap());

        TextView prayerLabel = label(prayer + " prayer", 20, subColor, false);
        prayerLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams prayerLp = matchWrap();
        prayerLp.topMargin = dp(5);
        body.addView(prayerLabel, prayerLp);

        if (!reminderText.isEmpty()) {
            TextView incoming = label(reminderText, 13, subColor, true);
            incoming.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams inLp = matchWrap();
            inLp.topMargin = dp(10);
            body.addView(incoming, inLp);
        }

        TextView style = label(styleLabel, 11, Color.argb(170, Color.red(subColor), Color.green(subColor), Color.blue(subColor)), false);
        style.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams styleLp = matchWrap();
        styleLp.topMargin = dp(5);
        body.addView(style, styleLp);

        View spacer = new View(this);
        body.addView(spacer, new LinearLayout.LayoutParams(1, 0, .55f));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        body.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(138)));

        LinearLayout declineBox = controlColumn("Decline", red, textColor, this::dismissCall, samsungGlow);
        LinearLayout answerBox = controlColumn("Answer", green, textColor, this::answerCall, samsungGlow);
        controls.addView(declineBox, new LinearLayout.LayoutParams(0, dp(138), 1f));
        controls.addView(answerBox, new LinearLayout.LayoutParams(0, dp(138), 1f));
        pulseTarget = answerBox.getChildAt(0);

        TextView hint = label("Tap a button to respond", 12, subColor, false);
        hint.setGravity(Gravity.CENTER);
        body.addView(hint, matchWrap());
    }

    private LinearLayout controlColumn(String name, int color, int labelColor, Runnable action, boolean glow) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        TextView circle = label("☎", 34, Color.WHITE, true);
        circle.setGravity(Gravity.CENTER);
        GradientDrawable bg = round(color, dp(44));
        if (glow) bg.setStroke(dp(2), Color.argb(120,255,255,255));
        circle.setBackground(bg);
        circle.setElevation(dp(6));
        circle.setOnClickListener(v -> action.run());
        col.addView(circle, new LinearLayout.LayoutParams(dp(82), dp(82)));
        TextView text = label(name, 15, labelColor, false);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textLp = matchWrap();
        textLp.topMargin = dp(8);
        col.addView(text, textLp);
        return col;
    }

    private LinearLayout body(FrameLayout root) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(28), dp(24), dp(28), dp(28));
        root.addView(body, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return body;
    }

    private void addStyleButton(FrameLayout root, int color) {
        TextView styleButton = label("STYLE", 11, color, true);
        styleButton.setGravity(Gravity.CENTER);
        styleButton.setLetterSpacing(.08f);
        styleButton.setBackground(round(Color.argb(48,255,255,255), dp(18)));
        styleButton.setOnClickListener(v -> startActivity(new Intent(this, CallStyleSettingsActivity.class)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(76), dp(38), Gravity.TOP | Gravity.END);
        lp.topMargin = dp(14);
        lp.rightMargin = dp(16);
        root.addView(styleButton, lp);
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
            default:
                return false;
        }
    }

    private String arabicPrayer(String prayer) {
        if ("Fajr".equalsIgnoreCase(prayer)) return "الفجر";
        if ("Dhuhr".equalsIgnoreCase(prayer)) return "الظهر";
        if ("Asr".equalsIgnoreCase(prayer)) return "العصر";
        if ("Maghrib".equalsIgnoreCase(prayer)) return "المغرب";
        if ("Isha".equalsIgnoreCase(prayer)) return "العشاء";
        return "الصلاة";
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

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
