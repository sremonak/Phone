package com.aurora.phone.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Animated7-day bar chart drawn on canvas (no chart library needed). */
public class WeekChartView extends View {

    private int[] counts = new int[7];
    private String[] labels = new String[] {"Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"};
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 0f;
    private int primaryColor = 0xFF7C5CFF;

    public WeekChartView(Context context) {
        super(context);
        init(context);
    }

    public WeekChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeekChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(com.aurora.phone.R.attr.av_colorPrimary, tv, true)) {
            primaryColor = tv.data;
        }
        barPaint.setColor(primaryColor);
        trackPaint.setColor(0x14000000);
        countPaint.setColor(0xFF3A3F52);
        countPaint.setTextAlign(Paint.Align.CENTER);
        countPaint.setTextSize(12f * getResources().getDisplayMetrics().scaledDensity);
        countPaint.setFakeBoldText(true);
        labelPaint.setColor(0xFF8A90A6);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);
    }

    public void setData(int[] values, String[] days) {
        if (values != null && values.length == 7) {
            counts = values;
        }
        if (days != null && days.length == 7) {
            labels = days;
        }
        progress = 0f;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(650);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    public boolean isEmpty() {
        int sum = 0;
        for (int c : counts) {
            sum += c;
        }
        return sum == 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float labelSpace = 20f * density;
        float topSpace = 22f * density;
        float chartH = h - labelSpace - topSpace;
        float slot = w / 7f;
        float barW = slot * 0.46f;

        int max = 1;
        for (int c : counts) {
            if (c > max) {
                max = c;
            }
        }

        for (int i = 0; i < 7; i++) {
            float cx = slot * i + slot / 2f;
            float full = (counts[i] / (float) max) * chartH;
            float grown = full * progress;

            // track
            rect.set(cx - barW / 2f, topSpace, cx + barW / 2f, topSpace + chartH);
            canvas.drawRoundRect(rect, 8f * density, 8f * density, trackPaint);
            // bar
            if (grown > 4f * density) {
                rect.set(cx - barW / 2f, topSpace + chartH - grown,
                        cx + barW / 2f, topSpace + chartH);
                canvas.drawRoundRect(rect, 8f * density, 8f * density, barPaint);
            }
            // count above
            if (counts[i] > 0 && progress > 0.6f) {
                canvas.drawText(String.valueOf(counts[i]), cx,
                        topSpace + chartH - grown - 6f * density, countPaint);
            }
            // day label
            canvas.drawText(labels[i], cx, h - 4f * density, labelPaint);
        }
    }
}
