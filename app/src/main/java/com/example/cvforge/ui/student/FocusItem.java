package com.example.cvforge.ui.student;

/** One card in Today's Focus on the Home screen. */
public class FocusItem {

    public final String initials;
    public final String title;
    public final String subtitle;
    public final String chipText;
    /** Color resources for the tile and the chip, for example R.color.violet_tint. */
    public final int tintColor;
    public final int textColor;
    /** Called when the student taps the card. */
    public final Runnable onClick;

    public FocusItem(String initials, String title, String subtitle, String chipText,
                     int tintColor, int textColor, Runnable onClick) {
        this.initials = initials;
        this.title = title;
        this.subtitle = subtitle;
        this.chipText = chipText;
        this.tintColor = tintColor;
        this.textColor = textColor;
        this.onClick = onClick;
    }
}
