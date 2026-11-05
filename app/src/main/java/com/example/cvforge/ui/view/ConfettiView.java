package com.example.cvforge.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;
import com.example.cvforge.util.AnimUtils;

import java.util.Random;

/**
 * A short burst of confetti in the brand colors (animation A13). It plays once for 1.5 seconds
 * when a card reaches Offer. It does not take any touches.
 */
public class ConfettiView extends View {

    private static final int PIECES = 70;
    private static final long DURATION_MS = 1500;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colors;
    private final Random random = new Random();

    // one entry per piece
    private final float[] startX = new float[PIECES];
    private final float[] speedX = new float[PIECES];
    private final float[] speedY = new float[PIECES];
    private final float[] size = new float[PIECES];
    private final float[] spin = new float[PIECES];
    private final int[] color = new int[PIECES];

    /** 0 to 1 while the burst plays. */
    private float time;
    private boolean playing;
    private ValueAnimator animator;

    public ConfettiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        colors = new int[]{
                ContextCompat.getColor(context, R.color.violet),
                ContextCompat.getColor(context, R.color.coral),
                ContextCompat.getColor(context, R.color.amber),
                ContextCompat.getColor(context, R.color.teal),
                ContextCompat.getColor(context, R.color.sky)};
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Starts the burst from the top middle of the view. */
    public void burst() {
        if (!AnimUtils.animationsEnabled(getContext())) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < PIECES; i++) {
            startX[i] = getWidth() / 2f + (random.nextFloat() - 0.5f) * 80 * density;
            speedX[i] = (random.nextFloat() - 0.5f) * 520 * density;
            speedY[i] = (-random.nextFloat() * 420 - 120) * density;
            size[i] = (5 + random.nextFloat() * 6) * density;
            spin[i] = random.nextFloat() * 720 - 360;
            color[i] = colors[random.nextInt(colors.length)];
        }
        if (animator != null) {
            animator.cancel();
        }
        playing = true;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            time = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                playing = false;
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!playing) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float seconds = time * DURATION_MS / 1000f;
        float gravity = 900 * density;
        float startY = getHeight() * 0.25f;

        for (int i = 0; i < PIECES; i++) {
            float x = startX[i] + speedX[i] * seconds;
            float y = startY + speedY[i] * seconds + 0.5f * gravity * seconds * seconds;
            paint.setColor(color[i]);
            paint.setAlpha((int) (255 * (1f - time * time)));

            canvas.save();
            canvas.rotate(spin[i] * seconds, x, y);
            canvas.drawRect(x - size[i] / 2, y - size[i] / 2, x + size[i] / 2, y + size[i], paint);
            canvas.restore();
        }
    }
}
