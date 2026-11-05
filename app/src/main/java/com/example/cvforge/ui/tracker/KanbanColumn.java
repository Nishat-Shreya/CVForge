package com.example.cvforge.ui.tracker;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Application;

/** The four columns of the Kanban board with their colors. */
public enum KanbanColumn {

    SAVED(Application.STATUS_SAVED, R.string.column_saved, R.color.violet, R.color.violet_tint, R.color.violet_text),
    APPLIED(Application.STATUS_APPLIED, R.string.column_applied, R.color.sky, R.color.sky_tint, R.color.sky_text),
    INTERVIEW(Application.STATUS_INTERVIEW, R.string.column_interview, R.color.amber, R.color.amber_tint, R.color.amber_text),
    OFFER(Application.STATUS_OFFER, R.string.column_offer, R.color.teal, R.color.teal_tint, R.color.teal_text);

    /** The status that belongs in this column. */
    public final String status;
    public final int nameRes;
    /** The strong color: dot, top border of the cards. */
    public final int color;
    /** The soft color of the header pill, with its dark text color. */
    public final int tintColor;
    public final int textColor;

    KanbanColumn(String status, int nameRes, int color, int tintColor, int textColor) {
        this.status = status;
        this.nameRes = nameRes;
        this.color = color;
        this.tintColor = tintColor;
        this.textColor = textColor;
    }
}
