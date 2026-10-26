package com.example.cvforge.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cvforge.R;
import com.example.cvforge.util.AnimUtils;

/**
 * Round progress ring with the number in the middle. Used for the Profile Strength on
 * Home (animation A1, 1.4 s) and for match percentages (animation A2, 1.3 s).
 * It is drawn on a Canvas: a full circle for the track and an arc for the value.
 */
public class ProfileStrengthRing extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private boolean showPercentSign = true;
    /** The number shown right now. It counts up while the animation runs. */
    private float shownValue = 0f;
    private ValueAnimator animator;

    public ProfileStrengthRing(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        float strokeWidth = 10 * density;
        float textSize = 24 * density;
        int ringColor = ContextCompat.getColor(context, R.color.amber);
        int trackColor = ContextCompat.getColor(context, R.color.violet_tint);
        int textColor = ContextCompat.getColor(context, R.color.ink);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProfileStrengthRing);
            ringColor = a.getColor(R.styleable.ProfileStrengthRing_ringColor, ringColor);
            trackColor = a.getColor(R.styleable.ProfileStrengthRing_ringTrackColor, trackColor);
            textColor = a.getColor(R.styleable.ProfileStrengthRing_ringTextColor, textColor);
            strokeWidth = a.getDimension(R.styleable.ProfileStrengthRing_ringStrokeWidth, strokeWidth);
            textSize = a.getDimension(R.styleable.ProfileStrengthRing_ringTextSize, textSize);
            showPercentSign = a.getBoolean(R.styleable.ProfileStrengthRing_ringShowPercentSign, true);
            a.recycle();
        }

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(trackColor);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(ringColor);

        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(ResourcesCompat.getFont(context, R.font.sora_extrabold));
    }

    /** Changes the color of the arc, for example teal for a very good match. */
    public void setRingColor(int color) {
        arcPaint.setColor(color);
        invalidate();
    }

    /** Sets the value (0 to 100) without animation. */
    public void setValue(int value) {
        cancelAnimation();
        shownValue = clamp(value);
        invalidate();
    }

    /** A1 / A2: fills the ring from 0 to the value and counts the number up with it. */
    public void animateTo(int value, long durationMs) {
        cancelAnimation();
        float target = clamp(value);
        if (!AnimUtils.animationsEnabled(getContext())) {
            shownValue = target;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(0f, target);
        animator.setDuration(durationMs);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            shownValue = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void cancelAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private float clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float half = trackPaint.getStrokeWidth() / 2f;
        arcBounds.set(half + getPaddingLeft(), half + getPaddingTop(),
                getWidth() - half - getPaddingRight(), getHeight() - half - getPaddingBottom());

        canvas.drawArc(arcBounds, 0, 360, false, trackPaint);
        canvas.drawArc(arcBounds, -90, 360f * shownValue / 100f, false, arcPaint);

        String text = Math.round(shownValue) + (showPercentSign ? "%" : "");
        float baseline = getHeight() / 2f - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(text, getWidth() / 2f, baseline, textPaint);
    }
}
