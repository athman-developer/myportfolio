package com.trewx.qiblah;

import android.content.Context;
import android.content.SharedPreferences;

public final class CallStylePrefs {
    private static final String PREFS = "qibla_call_style";
    private static final String KEY_STYLE = "style";

    public static final String IPHONE = "iphone";
    public static final String WHATSAPP = "whatsapp";
    public static final String PIXEL = "pixel";
    public static final String SAMSUNG = "samsung";
    public static final String ISLAMIC = "islamic";
    public static final String MINIMAL = "minimal";
    public static final String ORANGE = "orange";

    public static final String[] KEYS = {
            IPHONE, WHATSAPP, PIXEL, SAMSUNG, ISLAMIC, MINIMAL, ORANGE
    };

    public static final String[] LABELS = {
            "iPhone-inspired",
            "WhatsApp-inspired",
            "Google Pixel-inspired",
            "Samsung One UI-inspired",
            "Islamic Premium",
            "Minimal",
            "Qibla Compass Orange"
    };

    public static final String[] DESCRIPTIONS = {
            "Clean dark screen with separate red decline and green answer circles.",
            "Deep green call screen with familiar circular controls and a soft pulse.",
            "Wide bottom slide-to-answer bar based on the current Qibla Compass call screen.",
            "Soft dark gradient with large circular controls and a subtle glow animation.",
            "Emerald and gold prayer screen with Arabic prayer names and premium Islamic styling.",
            "Pure black AMOLED layout with an extra-large caller name and minimal controls.",
            "Black and orange branded layout with the Qibla Compass slider interaction."
    };

    private CallStylePrefs() { }

    public static String getStyle(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getString(KEY_STYLE, PIXEL);
    }

    public static void setStyle(Context context, String style) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_STYLE, style).apply();
    }

    public static String labelFor(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return LABELS[i];
        return LABELS[2];
    }
}
