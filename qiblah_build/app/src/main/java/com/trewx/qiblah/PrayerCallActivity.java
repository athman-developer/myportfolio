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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrayerCallActivity extends Activity {
    private static final int ORANGE = Color.rgb(255, 116, 23);
    private static final int INK = Color.rgb(20, 20, 20);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int notificationId = -1;

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
        getWindow().setStatusBarColor(INK);
        getWindow().setNavigationBarColor(INK);

        String prayer = getIntent().getStringExtra("prayer");
        if (prayer == null || prayer.trim().isEmpty()) prayer = "Prayer";
        notificationId = getIntent().getIntExtra("notification_id", -1);
        buildUi(prayer);
        handler.postDelayed(this::dismissCall, 90000L);
    }

    private void buildUi(String prayer) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(36), dp(28), dp(34));
        root.setBackgroundColor(INK);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(28) + insets.getSystemWindowInsetLeft(),
                    dp(36) + insets.getSystemWindowInsetTop(),
                    dp(28) + insets.getSystemWindowInsetRight(),
                    dp(34) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();

        TextView time = label(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), 16, Color.LTGRAY, false);
        root.addView(time, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0));

        View avatar = new View(this);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(ORANGE);
        avatar.setBackground(avatarBg);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(116), dp(116));
        avatarLp.topMargin = dp(80);
        root.addView(avatar, avatarLp);

        TextView caller = label("الله", 58, Color.WHITE, true);
        caller.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams callerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        callerLp.topMargin = dp(34);
        root.addView(caller, callerLp);

        TextView incoming = label("PRAYER REMINDER", 13, ORANGE, true);
        incoming.setLetterSpacing(.14f);
        incoming.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams incLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        incLp.topMargin = dp(10);
        root.addView(incoming, incLp);

        TextView prayerLabel = label(prayer + " prayer time", 22, Color.WHITE, false);
        prayerLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams prayerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        prayerLp.topMargin = dp(16);
        root.addView(prayerLabel, prayerLp);

        TextView hint = label("Answer to open Qibla Compass, or dismiss the reminder.", 14, Color.rgb(180,180,180), false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(10);
        root.addView(hint, hintLp);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        root.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));

        Button dismiss = action("DISMISS", Color.rgb(78, 78, 78));
        Button answer = action("ANSWER", Color.rgb(21, 160, 99));
        LinearLayout.LayoutParams btn = new LinearLayout.LayoutParams(0, dp(62), 1f);
        btn.rightMargin = dp(8);
        actions.addView(dismiss, btn);
        LinearLayout.LayoutParams btn2 = new LinearLayout.LayoutParams(0, dp(62), 1f);
        btn2.leftMargin = dp(8);
        actions.addView(answer, btn2);

        dismiss.setOnClickListener(v -> dismissCall());
        answer.setOnClickListener(v -> {
            cancelNotification();
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            open.putExtra("open_prayers", true);
            startActivity(open);
            finish();
        });
    }

    private Button action(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(30));
        b.setBackground(bg);
        return b;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int left, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.leftMargin = left;
        p.topMargin = top;
        return p;
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
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
