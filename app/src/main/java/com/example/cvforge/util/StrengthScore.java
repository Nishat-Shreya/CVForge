package com.example.cvforge.util;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Cv;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores a CV out of 100 for the Profile Strength Ring.
 * Every filled part adds points. The first missing part becomes the tip.
 */
public class StrengthScore {

    public static class Result {
        /** 0 to 100. */
        public final int score;
        /** String resource with the next thing to add, or 0 when the CV is complete. */
        public final int tipRes;

        Result(int score, int tipRes) {
            this.score = score;
            this.tipRes = tipRes;
        }
    }

    private static class Part {
        final int points;
        final boolean done;
        final int tipRes;

        Part(int points, boolean done, int tipRes) {
            this.points = points;
            this.done = done;
            this.tipRes = tipRes;
        }
    }

    private StrengthScore() {
    }

    public static Result of(Cv cv) {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(10, filled(cv.fullName), R.string.tip_name));
        parts.add(new Part(5, filled(cv.headline), R.string.tip_headline));
        parts.add(new Part(5, filled(cv.email) || filled(cv.phone), R.string.tip_contact));
        parts.add(new Part(10, filled(cv.summary), R.string.tip_summary));
        parts.add(new Part(15, size(cv.skills) >= 3, R.string.tip_skills));
        parts.add(new Part(10, size(cv.education) >= 1, R.string.tip_education));
        parts.add(new Part(15, size(cv.projects) >= 1, R.string.tip_project));
        parts.add(new Part(10, size(cv.projects) >= 2, R.string.tip_second_project));
        parts.add(new Part(10, size(cv.experience) >= 1, R.string.tip_experience));
        parts.add(new Part(5, size(cv.certificates) >= 1, R.string.tip_certificate));
        parts.add(new Part(5, size(cv.links) >= 1, R.string.tip_link));

        int score = 0;
        int tip = 0;
        for (Part part : parts) {
            if (part.done) {
                score += part.points;
            } else if (tip == 0) {
                tip = part.tipRes;
            }
        }
        return new Result(score, tip);
    }

    private static boolean filled(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
