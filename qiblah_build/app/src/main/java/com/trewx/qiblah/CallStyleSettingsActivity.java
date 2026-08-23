package com.trewx.qiblah;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class CallStyleSettingsActivity extends Activity {
    private static final int BG = Color.rgb(248, 247, 242);
    private static final int INK = Color.rgb(24, 24, 24);
    private static final int MUTED = Color.rgb(105, 105, 105);
    private static final int ORANGE = Color.rgb(255, 116, 23);
    private LinearLayout list;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();

        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), dp(24), dp(20), dp(34));
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Call screen style", 30, INK, true);
        list.addView(title);
        TextView sub = text("Choose how prayer reminders appear. Your selection is saved and used for every future prayer call.", 15, MUTED, false);
        sub.setPadding(0, dp(8), 0, dp(16));
        list.addView(sub);
        rebuildChoices();
    }

    private void rebuildChoices() {
        while (list.getChildCount() > 2) list.removeViewAt(2);
        String selected = CallStylePrefs.getStyle(this);
        for (int i = 0; i < CallStylePrefs.KEYS.length; i++) {
            final String key = CallStylePrefs.KEYS[i];
            boolean active = key.equals(selected);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackground(cardBg(active));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(10);
            list.addView(card, cardLp);

            View swatch = new View(this);
            swatch.setBackground(previewBg(key));
            card.addView(swatch, new LinearLayout.LayoutParams(dp(54), dp(72)));

            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.setPadding(dp(14), 0, dp(8), 0);
            card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView name = text(CallStylePrefs.LABELS[i], 17, INK, true);
            copy.addView(name);
            TextView desc = text(CallStylePrefs.DESCRIPTIONS[i], 13, MUTED, false);
            desc.setPadding(0, dp(4), 0, 0);
            copy.addView(desc);

            TextView check = text(active ? "✓" : "", 25, ORANGE, true);
            check.setGravity(Gravity.CENTER);
            card.addView(check, new LinearLayout.LayoutParams(dp(34), dp(54)));
            card.setOnClickListener(v -> {
                CallStylePrefs.setStyle(this, key);
                rebuildChoices();
            });
        }
        TextView note = text("Tip: open TEST CALL SCREEN, tap STYLE, choose a design here, then press Back to preview it immediately.", 12, MUTED, false);
        note.setPadding(dp(4), dp(6), dp(4), dp(10));
        list.addView(note);
    }

    private GradientDrawable cardBg(boolean active) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.WHITE);
        g.setCornerRadius(dp(20));
        g.setStroke(dp(active ? 2 : 1), active ? ORANGE : Color.rgb(225,223,216));
        return g;
    }

    private GradientDrawable previewBg(String key) {
        int[] colors;
        if (CallStylePrefs.WHATSAPP.equals(key)) colors = new int[]{Color.rgb(6,35,28), Color.rgb(12,78,61)};
        else if (CallStylePrefs.SAMSUNG.equals(key)) colors = new int[]{Color.rgb(19,24,48), Color.rgb(48,55,90)};
        else if (CallStylePrefs.ISLAMIC.equals(key)) colors = new int[]{Color.rgb(4,43,34), Color.rgb(12,88,65)};
        else if (CallStylePrefs.MINIMAL.equals(key)) colors = new int[]{Color.BLACK, Color.rgb(17,17,17)};
        else if (CallStylePrefs.ORANGE.equals(key)) colors = new int[]{Color.rgb(18,18,18), Color.rgb(82,40,12)};
        else if (CallStylePrefs.IPHONE.equals(key)) colors = new int[]{Color.rgb(18,18,20), Color.rgb(37,37,41)};
        else colors = new int[]{Color.rgb(26,28,40), Color.rgb(54,58,70)};
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(dp(15));
        return g;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0f, 1.08f);
        return t;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
