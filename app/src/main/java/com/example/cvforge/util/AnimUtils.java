package com.example.cvforge.util;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * Small helpers for the animations in DESIGN.md section 6.
 * Every helper does nothing when the system animation scale is 0.
 */
public class AnimUtils {

    private AnimUtils() {
    }

    /** False when the user turned animations off in the system settings. */
    public static boolean animationsEnabled(Context context) {
        float scale = Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale != 0f;
    }

    /** A4: slide up 14dp and fade in. index delays each card by 65 ms. */
    public static void slideUpIn(View view, int index) {
        if (!animationsEnabled(view.getContext())) {
            return;
        }
        float distance = 14 * view.getResources().getDisplayMetrics().density;
        view.setAlpha(0f);
        view.setTranslationY(distance);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 65L)
                .setDuration(350)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /** A7: pulse the view between scale 1.0 and 1.4 forever. Call cancel on the result to stop. */
    public static ObjectAnimator pulse(View view) {
        if (!animationsEnabled(view.getContext())) {
            return null;
        }
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.4f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.4f));
        animator.setDuration(800);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.start();
        return animator;
    }

    /** A5: float up and down by 8dp, 4 s loop. startOffsetMs shifts where the loop starts. */
    public static ObjectAnimator floatUpDown(View view, long startOffsetMs) {
        if (!animationsEnabled(view.getContext())) {
            return null;
        }
        float distance = 8 * view.getResources().getDisplayMetrics().density;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -distance, distance);
        animator.setDuration(2000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.start();
        animator.setCurrentPlayTime(startOffsetMs);
        return animator;
    }

    /** A12: fade-through between screens, 200 ms. Call right after startActivity or finish. */
    public static void fadeTransition(Activity activity) {
        if (!animationsEnabled(activity)) {
            return;
        }
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
