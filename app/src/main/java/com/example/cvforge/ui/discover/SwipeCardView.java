package com.example.cvforge.ui.discover;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.ViewSwipeCardBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.MatchScore;

import java.util.ArrayList;
import java.util.List;

/**
 * The big internship card on Discover. The student drags it with a finger:
 * to the right to apply, to the left to skip (animation A6).
 * While nobody touches it, it sways gently from side to side.
 */
public class SwipeCardView extends FrameLayout {

    public interface Listener {
        /** The card was dragged far to the left. */
        void onSwipeLeft();

        /** The card was dragged far to the right. The listener decides if the card leaves. */
        void onSwipeRight();

        /** The card was tapped. */
        void onCardClick();
    }

    public static final int LEFT = -1;
    public static final int RIGHT = 1;
    public static final int UP = 0;

    private final ViewSwipeCardBinding binding;
    private final int touchSlop;
    private Listener listener;
    private ObjectAnimator swayAnimator;

    private float startX;
    private float startY;
    private boolean dragging;

    public SwipeCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = ViewSwipeCardBinding.inflate(LayoutInflater.from(context), this);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ---------- showing an internship ----------

    /** Shows an internship. The card appears with a slide up (A4) and starts to sway. */
    public void bind(Internship post, List<String> studentSkills) {
        resetPosition();

        binding.textLogo.setText(initials(post.posterName));
        binding.textTitle.setText(post.title);
        binding.textMeta.setText(getContext().getString(R.string.card_meta, post.posterName, post.location,
                InternshipAdapter.typeName(getContext(), post.type)));
        showSource(post);
        showMatch(post, studentSkills);

        AnimUtils.slideUpIn(binding.cardContent, 0);
        startSway();
    }

    private void showSource(Internship post) {
        if (post.isExternal()) {
            binding.chipSource.setText(getContext().getString(R.string.via_source, InternshipAdapter.sourceName(post.source)));
        } else {
            binding.chipSource.setText(R.string.verified_company);
        }
    }

    private void showMatch(Internship post, List<String> studentSkills) {
        List<String> required = post.skills == null ? new ArrayList<>() : post.skills;
        MatchScore.Result result = MatchScore.compare(studentSkills, required);

        binding.ringMatch.setVisibility(required.isEmpty() ? GONE : VISIBLE);
        binding.ringMatch.animateTo(result.percent, 1300);

        List<Boolean> hasSkill = new ArrayList<>();
        for (String skill : required) {
            hasSkill.add(result.matched.contains(skill));
        }
        binding.radar.setData(required, hasSkill);
        binding.rowRadar.setVisibility(binding.radar.hasEnoughAxes() ? VISIBLE : GONE);
        binding.radar.animateIn();
        binding.textSkillsLine.setText(ListTextJoin.join(required));

        binding.groupMatch.removeAllViews();
        for (String skill : result.matched) {
            addChip(skill, R.color.teal_tint, R.color.teal_text);
        }
        for (String skill : result.missing) {
            addChip(getContext().getString(R.string.missing_format, skill), R.color.coral_tint, R.color.coral_text);
        }

        if (result.missing.isEmpty()) {
            binding.textTip.setVisibility(GONE);
        } else {
            binding.textTip.setVisibility(VISIBLE);
            binding.textTip.setText(getContext().getString(R.string.match_tip, result.missing.get(0),
                    MatchScore.percentWithOneMore(result)));
        }
    }

    private void addChip(String text, int backgroundRes, int textRes) {
        TextView chip = (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.include_status_chip, binding.groupMatch, false);
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), backgroundRes));
        chip.setTextColor(ContextCompat.getColor(getContext(), textRes));
        binding.groupMatch.addView(chip);
    }

    // ---------- sway (A6) ----------

    /** The card content rotates between -1.5 and 1.5 degrees, 5 seconds for one way and back. */
    private void startSway() {
        stopSway();
        if (!AnimUtils.animationsEnabled(getContext())) {
            setCardLayer(false);
            return;
        }
        setCardLayer(true);
        swayAnimator = ObjectAnimator.ofFloat(binding.cardContent, ROTATION, -1.5f, 1.5f);
        swayAnimator.setDuration(2500);
        swayAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        swayAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        swayAnimator.start();
    }

    private void stopSway() {
        if (swayAnimator != null) {
            swayAnimator.cancel();
            swayAnimator = null;
        }
        binding.cardContent.setRotation(0f);
    }

    /**
     * While the card moves or sways, all of it (texts, ring, radar, chips) is drawn once into a
     * hardware layer. The layer is a picture that is moved and rotated as one piece, so the
     * letters are not drawn again, at a new angle, on every frame. That redrawing made them tremble.
     */
    private void setCardLayer(boolean on) {
        binding.cardContent.setLayerType(on ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE, null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSway();
        setCardLayer(false);
    }

    // ---------- dragging ----------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startX;
                float dy = event.getRawY() - startY;
                if (!dragging && Math.abs(dx) > touchSlop) {
                    dragging = true;
                    stopSway();
                    setCardLayer(true);
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (dragging) {
                    setTranslationX(dx);
                    setTranslationY(dy * 0.3f);
                    setRotation(dx / getWidth() * 18f);
                }
                return true;

            case MotionEvent.ACTION_UP:
                finishDrag(event.getRawX() - startX);
                return true;

            case MotionEvent.ACTION_CANCEL:
                springBack();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void finishDrag(float dx) {
        if (!dragging) {
            performClick();
            return;
        }
        float limit = getWidth() * 0.3f;
        if (dx > limit && listener != null) {
            listener.onSwipeRight();
        } else if (dx < -limit && listener != null) {
            listener.onSwipeLeft();
        } else {
            springBack();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (listener != null) {
            listener.onCardClick();
        }
        return true;
    }

    // ---------- leaving ----------

    /** The card flies out of the screen to the right, to the left or up, then calls onEnd. */
    public void flyOut(int direction, Runnable onEnd) {
        stopSway();
        float width = getResources().getDisplayMetrics().widthPixels;
        float height = getResources().getDisplayMetrics().heightPixels;
        float targetX = direction * width * 1.3f;
        float targetY = direction == UP ? -height : getTranslationY();
        float targetRotation = direction * 25f;

        if (!AnimUtils.animationsEnabled(getContext())) {
            onEnd.run();
            return;
        }
        setCardLayer(true);
        animate().translationX(direction == UP ? 0f : targetX)
                .translationY(targetY)
                .rotation(direction == UP ? 0f : targetRotation)
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    setCardLayer(false);
                    onEnd.run();
                })
                .start();
    }

    /** The card goes back to the middle, with a small bounce. */
    public void springBack() {
        setCardLayer(true);
        animate().translationX(0f).translationY(0f).rotation(0f).alpha(1f)
                .setDuration(250)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(this::startSway)
                .start();
    }

    private void resetPosition() {
        animate().cancel();
        setTranslationX(0f);
        setTranslationY(0f);
        setRotation(0f);
        setAlpha(1f);
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }

    /** Joins skills with a dot between them. */
    private static class ListTextJoin {
        static String join(List<String> skills) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < skills.size(); i++) {
                if (i > 0) {
                    text.append(" · ");
                }
                text.append(skills.get(i));
            }
            return text.toString();
        }
    }
}
