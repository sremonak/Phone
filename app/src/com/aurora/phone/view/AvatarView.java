package com.aurora.phone.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.aurora.phone.R;

/** Circular gradient avatar with initials, drawn entirely in code. */
public class AvatarView extends View {

    private String initials = "?";
    private int baseColor = 0xFF7C5CFF;
    private int ringWidthPx = 0;
    private int ringColor = 0xFFFFFFFF;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader gradient;
    private final RectF rect = new RectF();

    public AvatarView(Context context) {
        super(context);
        init();
    }

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        ringPaint.setStyle(Paint.Style.STROKE);
        setWillNotDraw(false);
    }

    /** Shows the first letters of [name] on a colour derived from it. */
    public void setPerson(String name) {
        initials = initialsOf(name);
        baseColor = colorFor(getContext(), name);
        gradient = null;
        invalidate();
    }

    public void setRing(int widthPx, int color) {
        ringWidthPx = widthPx;
        ringColor = color;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradient = null;
        if (h > 0) {
            textPaint.setTextSize(h * 0.36f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float inset = ringWidthPx > 0 ? ringWidthPx / 2f + 1f : 0f;
        rect.set(inset, inset, w - inset, h - inset);
        if (gradient == null) {
            gradient = new LinearGradient(0, 0, w, h, baseColor, darken(baseColor),
                    Shader.TileMode.CLAMP);
        }
        fillPaint.setShader(gradient);
        canvas.drawOval(rect, fillPaint);
        if (ringWidthPx > 0) {
            ringPaint.setColor(ringColor);
            ringPaint.setStrokeWidth(ringWidthPx);
            rect.set(0, 0, w, h);
            canvas.drawOval(rect, ringPaint);
        }
        if (initials.length() > 0) {
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float baseline = (h - (fm.ascent + fm.descent)) / 2f;
            canvas.drawText(initials, w / 2f, baseline, textPaint);
        }
    }

    public static String initialsOf(String name) {
        if (name == null || name.trim().length() == 0) {
            return "#";
        }
        String trimmed = name.trim();
        String[] parts = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        if (parts.length >= 2 && parts[0].length() > 0 && parts[1].length() > 0) {
            sb.append(Character.toUpperCase(parts[0].charAt(0)));
            sb.append(Character.toUpperCase(parts[1].charAt(0)));
        } else {
            sb.append(Character.toUpperCase(trimmed.charAt(0)));
        }
        return sb.toString();
    }

    public static int colorFor(Context c, String seed) {
        int[] palette = new int[] {
                R.color.av_1, R.color.av_2, R.color.av_3, R.color.av_4,
                R.color.av_5, R.color.av_6, R.color.av_7, R.color.av_8
        };
        int index;
        if (seed == null || seed.length() == 0) {
            index = 0;
        } else {
            index = Math.abs(seed.hashCode()) % palette.length;
        }
        return c.getColor(palette[index]);
    }

    private static int darken(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return Color.rgb((int) (r * 0.68f), (int) (g * 0.68f), (int) (b * 0.72f));
    }
}
