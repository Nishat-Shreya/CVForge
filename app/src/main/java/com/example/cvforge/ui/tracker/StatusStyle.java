package com.example.cvforge.ui.tracker;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Application;

/** Text and colors for each of the six statuses. Status always has a label as well as a color. */
public class StatusStyle {

    private StatusStyle() {
    }

    public static int labelRes(String status) {
        switch (status == null ? "" : status) {
            case Application.STATUS_APPLIED:
                return R.string.column_applied;
            case Application.STATUS_SHORTLISTED:
                return R.string.chip_shortlisted;
            case Application.STATUS_INTERVIEW:
                return R.string.column_interview;
            case Application.STATUS_OFFER:
                return R.string.column_offer;
            case Application.STATUS_REJECTED:
                return R.string.chip_rejected;
            default:
                return R.string.column_saved;
        }
    }

    /** The strong color (dot). */
    public static int colorRes(String status) {
        switch (status == null ? "" : status) {
            case Application.STATUS_APPLIED:
                return R.color.sky;
            case Application.STATUS_SHORTLISTED:
            case Application.STATUS_OFFER:
                return R.color.teal;
            case Application.STATUS_INTERVIEW:
                return R.color.amber;
            case Application.STATUS_REJECTED:
                return R.color.coral;
            default:
                return R.color.violet;
        }
    }

    /** The soft color for chips. */
    public static int tintRes(String status) {
        switch (status == null ? "" : status) {
            case Application.STATUS_APPLIED:
                return R.color.sky_tint;
            case Application.STATUS_SHORTLISTED:
            case Application.STATUS_OFFER:
                return R.color.teal_tint;
            case Application.STATUS_INTERVIEW:
                return R.color.amber_tint;
            case Application.STATUS_REJECTED:
                return R.color.coral_tint;
            default:
                return R.color.violet_tint;
        }
    }

    /** The dark text color that goes with the soft color. */
    public static int textRes(String status) {
        switch (status == null ? "" : status) {
            case Application.STATUS_APPLIED:
                return R.color.sky_text;
            case Application.STATUS_SHORTLISTED:
            case Application.STATUS_OFFER:
                return R.color.teal_text;
            case Application.STATUS_INTERVIEW:
                return R.color.amber_text;
            case Application.STATUS_REJECTED:
                return R.color.coral_text;
            default:
                return R.color.violet_text;
        }
    }
}
