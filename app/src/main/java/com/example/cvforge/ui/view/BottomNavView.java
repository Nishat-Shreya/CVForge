package com.example.cvforge.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;
import com.example.cvforge.databinding.ItemBottomNavTabBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom navigation bar from DESIGN.md section 4. The active tab has a pill
 * behind its icon, and the pill slides to the new tab (animation A9).
 * Used by the student shell (5 tabs) and the company shell (4 tabs).
 */
public class BottomNavView extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private static final int PILL_WIDTH_DP = 52;
    private static final int PILL_HEIGHT_DP = 28;

    private final LinearLayout tabsRow;
    private final View pill;
    private final List<ItemBottomNavTabBinding> tabs = new ArrayList<>();
    private OnTabSelectedListener listener;
    private int selectedIndex = -1;

    public BottomNavView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(ContextCompat.getColor(context, R.color.card));
        float density = getResources().getDisplayMetrics().density;

        // 1dp border line on top
        View border = new View(context);
        border.setBackgroundColor(ContextCompat.getColor(context, R.color.border));
        addView(border, new LayoutParams(LayoutParams.MATCH_PARENT, Math.round(density)));

        pill = new View(context);
        pill.setBackgroundResource(R.drawable.bg_nav_pill);
        LayoutParams pillParams = new LayoutParams(Math.round(PILL_WIDTH_DP * density),
                Math.round(PILL_HEIGHT_DP * density));
        pillParams.topMargin = Math.round(10 * density);
        pill.setVisibility(INVISIBLE);
        addView(pill, pillParams);

        tabsRow = new LinearLayout(context);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        addView(tabsRow, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    /** Adds one tab. Call once per tab, in order, before select(). */
    public void addTab(@StringRes int labelRes, @DrawableRes int iconRes) {
        ItemBottomNavTabBinding tab = ItemBottomNavTabBinding.inflate(LayoutInflater.from(getContext()), tabsRow, false);
        tab.tabLabel.setText(labelRes);
        tab.tabIcon.setImageResource(iconRes);
        int index = tabs.size();
        tab.getRoot().setOnClickListener(v -> onTabClicked(index));
        tabs.add(tab);
        tabsRow.addView(tab.getRoot());
        applyColors(tab, false);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** Selects a tab without calling the listener (used when restoring state). */
    public void select(int index, boolean animate) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        if (selectedIndex >= 0) {
            applyColors(tabs.get(selectedIndex), false);
        }
        selectedIndex = index;
        applyColors(tabs.get(index), true);
        movePill(index, animate);
    }

    private void onTabClicked(int index) {
        if (index == selectedIndex) {
            return;
        }
        select(index, true);
        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    private void applyColors(ItemBottomNavTabBinding tab, boolean active) {
        int colorRes = active ? R.color.violet_text : R.color.muted;
        int color = ContextCompat.getColor(getContext(), colorRes);
        tab.tabIcon.setColorFilter(color);
        tab.tabLabel.setTextColor(color);
        tab.tabLabel.setTypeface(tab.tabLabel.getTypeface(), active ? Typeface.BOLD : Typeface.NORMAL);
    }

    /** A9: slide the pill so it sits behind the icon of the selected tab. */
    private void movePill(int index, boolean animate) {
        post(() -> {
            if (getWidth() == 0 || tabs.isEmpty()) {
                return;
            }
            float tabWidth = getWidth() / (float) tabs.size();
            float target = tabWidth * index + tabWidth / 2f - pill.getWidth() / 2f;
            boolean wasHidden = pill.getVisibility() != VISIBLE;
            pill.setVisibility(VISIBLE);
            if (animate && !wasHidden && AnimUtils.animationsEnabled(getContext())) {
                pill.animate().translationX(target).setDuration(250).start();
            } else {
                pill.setTranslationX(target);
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (selectedIndex >= 0) {
            movePill(selectedIndex, false);
        }
    }
}
