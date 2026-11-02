package com.example.cvforge.ui.discover;

import com.example.cvforge.data.model.Internship;

import java.io.Serializable;
import java.util.Locale;

/**
 * What the student searched and filtered for. Empty fields mean "any".
 * {@link #matches} says if one internship fits all the chosen filters.
 */
public class InternshipFilter implements Serializable {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    /** Search words, looked for in title, company, description and skills. */
    public String query = "";
    public String skill = "";
    public String location = "";
    /** Internship.TYPE_REMOTE, TYPE_ONSITE or TYPE_HYBRID. Empty means any. */
    public String type = "";
    public boolean paidOnly;
    /** Only internships that close within this many days. 0 means any deadline. */
    public int deadlineDays;

    /** True when nothing is filtered. */
    public boolean isEmpty() {
        return query.isEmpty() && skill.isEmpty() && location.isEmpty() && type.isEmpty()
                && !paidOnly && deadlineDays == 0;
    }

    /** How many filters from the filter sheet are on (search words are not counted). */
    public int sheetFilterCount() {
        int count = 0;
        if (!skill.isEmpty()) {
            count++;
        }
        if (!location.isEmpty()) {
            count++;
        }
        if (!type.isEmpty()) {
            count++;
        }
        if (paidOnly) {
            count++;
        }
        if (deadlineDays != 0) {
            count++;
        }
        return count;
    }

    public boolean matches(Internship post, long now) {
        if (!query.isEmpty() && !searchText(post).contains(query.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!skill.isEmpty() && !hasSkill(post, skill)) {
            return false;
        }
        if (!location.isEmpty() && !lower(post.location).contains(location.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!type.isEmpty() && !type.equals(post.type)) {
            return false;
        }
        if (paidOnly && post.stipend <= 0) {
            return false;
        }
        if (deadlineDays != 0 && post.deadline - now > deadlineDays * DAY_MS) {
            return false;
        }
        return true;
    }

    private static boolean hasSkill(Internship post, String skill) {
        String wanted = skill.toLowerCase(Locale.ROOT);
        if (post.skills != null) {
            for (String have : post.skills) {
                if (lower(have).contains(wanted)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String searchText(Internship post) {
        StringBuilder text = new StringBuilder();
        text.append(lower(post.title)).append(' ')
                .append(lower(post.posterName)).append(' ')
                .append(lower(post.description));
        if (post.skills != null) {
            for (String skill : post.skills) {
                text.append(' ').append(lower(skill));
            }
        }
        return text.toString();
    }

    private static String lower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
