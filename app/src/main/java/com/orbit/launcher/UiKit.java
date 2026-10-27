package com.orbit.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** Shared look for buttons and text, so both screens match. */
final class UiKit {

    private UiKit() {}

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** Rounded "pill" button: translucent normally, solid white when the remote focuses it. */
    static TextView pill(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        int padH = dp(c, 20), padV = dp(c, 9);
        t.setPadding(padH, padV, padH, padV);
        t.setFocusable(true);
        t.setClickable(true);

        float radius = dp(c, 24);
        GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(radius);
        normal.setColor(Color.argb(40, 255, 255, 255));
        GradientDrawable focused = new GradientDrawable();
        focused.setCornerRadius(radius);
        focused.setColor(Color.WHITE);

        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{android.R.attr.state_pressed}, focused);
        bg.addState(new int[]{}, normal);
        t.setBackground(bg);

        t.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {android.R.attr.state_pressed}, {}},
                new int[]{Color.rgb(12, 14, 20), Color.rgb(12, 14, 20), Color.argb(230, 255, 255, 255)}));

        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                float s = hasFocus ? 1.06f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(120).start();
            }
        });
        return t;
    }

    static TextView text(Context c, String s, float sp, int alpha, String family) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.argb(alpha, 255, 255, 255));
        t.setTypeface(Typeface.create(family, Typeface.NORMAL));
        return t;
    }
}
