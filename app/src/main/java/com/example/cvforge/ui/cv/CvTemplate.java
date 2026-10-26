package com.example.cvforge.ui.cv;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Cv;

/** The three CV looks. The colors are resource ids so the renderer can look them up. */
public enum CvTemplate {

    MODERN(Cv.TEMPLATE_MODERN, R.color.violet, R.color.violet_tint, R.color.violet_text),
    CLASSIC(Cv.TEMPLATE_CLASSIC, R.color.coral, R.color.coral_tint, R.color.coral_text),
    MINIMAL(Cv.TEMPLATE_MINIMAL, R.color.teal, R.color.teal_tint, R.color.teal_text);

    public final String id;
    public final int accentColor;
    public final int tintColor;
    public final int textColor;

    CvTemplate(String id, int accentColor, int tintColor, int textColor) {
        this.id = id;
        this.accentColor = accentColor;
        this.tintColor = tintColor;
        this.textColor = textColor;
    }

    /** Finds a template by the id saved in the CV. Unknown ids fall back to Modern. */
    public static CvTemplate fromId(String id) {
        for (CvTemplate template : values()) {
            if (template.id.equals(id)) {
                return template;
            }
        }
        return MODERN;
    }
}
