package com.aurora.phone.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Skeleton placeholder with a sweeping shimmer highlight (loading state). */
public class ShimmerView extends View {

    private final Paint rowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private ValueAnimator animator;
    private float phase = -1f;
    private int primaryColor = 0x337C5CFF;

    public ShimmerView(Context context) {
        super(context);
        init(context);
    }

    public ShimmerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ShimmerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(com.aurora.phone.R.attr.av_colorPrimary, tv, true)) {
            primaryColor = tv.data;
        }
        rowPaint.setColor(0x14000000);
        barPaint.setColor(0x10000000);
        shimmerPaint.setColor(0x22FFFFFF);
        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(-0.4f, 1.4f);
        animator.setDuration(1300);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                phase = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        float rowH = 74f * getResources().getDisplayMetrics().density;
        float avatar = 46f * getResources().getDisplayMetrics().density;
        float padStart = 18f * getResources().getDisplayMetrics().density;
        float padTop = 14f * getResources().getDisplayMetrics().density;

        int rows = Math.max(1, (int) ((h - padTop) / rowH));
        for (int i = 0; i < rows && i < 8; i++) {
            float top = padTop + i * rowH;
            float cx = padStart + avatar / 2f;
            float cy = top + avatar / 2f;
            canvas.drawCircle(cx, cy, avatar / 2f, rowPaint);

            float barLeft = padStart + avatar + 14f * getResources().getDisplayMetrics().density;
            rect.set(barLeft, top + 8f, barLeft + w * 0.42f, top + 8f + 14f);
            canvas.drawRoundRect(rect, 7f, 7f, barPaint);
            rect.set(barLeft, top + 32f, barLeft + w * 0.62f, top + 32f + 11f);
            canvas.drawRoundRect(rect, 6f, 6f, barPaint);
        }

        if (phase >= 0f) {
            float band = w * 0.5f;
            float x = phase * (w + band) - band;
            LinearGradient gradient = new LinearGradient(x, 0, x + band, 0,
                    0x00FFFFFF, shimmerPaint.getColor(), android.graphics.Shader.TileMode.CLAMP);
            shimmerPaint.setShader(gradient);
            canvas.drawRect(x, 0, x + band, h, shimmerPaint);
            shimmerPaint.setShader(null);
        }

        // subtle accent line while loading
        rect.set(0, 0, w, 3f);
        barPaint.setColor(primaryColor);
        canvas.drawRect(rect, barPaint);
        barPaint.setColor(0x10000000);
    }
}
