package com.example.cvforge.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cvforge.R;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Radar chart that compares the student with the role (feature 8, W9 custom view).
 * Every axis is one skill the role needs, from 3 to 5 axes.
 * The coral shape is what the role needs (full on every axis). The violet shape is the student:
 * full on the skills the student has, and small on the skills that are missing.
 * It is drawn on a Canvas and grows from the center when it appears.
 */
public class MatchRadarView extends View {

    private static final int MAX_AXES = 5;
    /** How small the violet shape is on an axis where the skill is missing. */
    private static final float MISSING_LEVEL = 0.18f;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint youPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint youStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private final List<String> labels = new ArrayList<>();
    private final List<Boolean> matched = new ArrayList<>();
    /** 0 to 1. The shapes are drawn at this size while they grow. */
    private float grow = 1f;
    private ValueAnimator animator;

    public MatchRadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * density);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.border));

        rolePaint.setStyle(Paint.Style.FILL);
        rolePaint.setColor(ContextCompat.getColor(context, R.color.coral));
        rolePaint.setAlpha(45);
        roleStrokePaint.setStyle(Paint.Style.STROKE);
        roleStrokePaint.setStrokeWidth(2f * density);
        roleStrokePaint.setColor(ContextCompat.getColor(context, R.color.coral));

        youPaint.setStyle(Paint.Style.FILL);
        youPaint.setColor(ContextCompat.getColor(context, R.color.violet));
        youPaint.setAlpha(80);
        youStrokePaint.setStyle(Paint.Style.STROKE);
        youStrokePaint.setStrokeWidth(2f * density);
        youStrokePaint.setColor(ContextCompat.getColor(context, R.color.violet));

        labelPaint.setColor(ContextCompat.getColor(context, R.color.muted));
        labelPaint.setTextSize(11f * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        // exact text positions, not snapped to whole pixels, so the labels stay steady when the card moves
        labelPaint.setSubpixelText(true);
        labelPaint.setLinearText(true);
        labelPaint.setTypeface(ResourcesCompat.getFont(context, R.font.dm_sans_bold));
    }

    /**
     * Sets the skills to draw. Only the first five are used.
     * @param skills  the skills the role needs
     * @param hasSkill for every skill, true if the student has it
     */
    public void setData(List<String> skills, List<Boolean> hasSkill) {
        labels.clear();
        matched.clear();
        for (int i = 0; i < skills.size() && i < MAX_AXES; i++) {
            labels.add(skills.get(i));
            matched.add(hasSkill.get(i));
        }
        invalidate();
    }

    /** True when the chart has enough skills (3 or more) to be a shape. */
    public boolean hasEnoughAxes() {
        return labels.size() >= 3;
    }

    /** The shapes grow from the center in 1 second. */
    public void animateIn() {
        if (animator != null) {
            animator.cancel();
        }
        if (!AnimUtils.animationsEnabled(getContext())) {
            grow = 1f;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            grow = (float) a.getAnimatedValue();
            invalidate();
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
        int count = labels.size();
        if (count < 3) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        // leave room around the shape for the skill names
        float radius = Math.min(getWidth(), getHeight()) / 2f - 22f * density;

        // grid: three rings
        for (int ring = 1; ring <= 3; ring++) {
            drawShape(canvas, centerX, centerY, radius * ring / 3f, count, null, gridPaint, null);
        }

        // what the role needs: full on every axis
        drawShape(canvas, centerX, centerY, radius * grow, count, null, rolePaint, roleStrokePaint);

        // the student: full on skills they have, small on missing ones
        float[] levels = new float[count];
        for (int i = 0; i < count; i++) {
            levels[i] = matched.get(i) ? 1f : MISSING_LEVEL;
        }
        drawShape(canvas, centerX, centerY, radius * grow, count, levels, youPaint, youStrokePaint);

        // skill names
        for (int i = 0; i < count; i++) {
            double angle = angleOf(i, count);
            float x = centerX + (float) Math.cos(angle) * (radius + 12f * density);
            float y = centerY + (float) Math.sin(angle) * (radius + 12f * density) + 4f * density;
            canvas.drawText(shorten(labels.get(i)), x, y, labelPaint);
        }
    }

    /** Draws one polygon. levels is the length of every axis from 0 to 1 (null = all full). */
    private void drawShape(Canvas canvas, float cx, float cy, float radius, int count, float[] levels,
                           Paint fill, Paint stroke) {
        path.reset();
        for (int i = 0; i < count; i++) {
            float level = levels == null ? 1f : levels[i];
            double angle = angleOf(i, count);
            float x = cx + (float) Math.cos(angle) * radius * level;
            float y = cy + (float) Math.sin(angle) * radius * level;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        if (fill != gridPaint) {
            canvas.drawPath(path, fill);
        }
        canvas.drawPath(path, stroke != null ? stroke : fill);
    }

    /** The first axis points up, the others follow clockwise. */
    private double angleOf(int index, int count) {
        return -Math.PI / 2 + 2 * Math.PI * index / count;
    }

    private String shorten(String text) {
        return text.length() > 10 ? text.substring(0, 9) + "…" : text;
    }
}
