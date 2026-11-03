package com.example.cvforge.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Match Radar logic (feature 8). No AI is needed: the skills in the student's CV are compared
 * with the skills the internship asks for.
 * Match percentage = matched skills divided by required skills.
 */
public class MatchScore {

    public static class Result {
        /** 0 to 100. */
        public final int percent;
        /** Required skills the student has. */
        public final List<String> matched;
        /** Required skills the student does not have yet. */
        public final List<String> missing;

        Result(int percent, List<String> matched, List<String> missing) {
            this.percent = percent;
            this.matched = matched;
            this.missing = missing;
        }
    }

    private MatchScore() {
    }

    /** Compares the student's skills with the required skills of an internship. */
    public static Result compare(List<String> studentSkills, List<String> requiredSkills) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        if (requiredSkills == null) {
            return new Result(0, matched, missing);
        }

        for (String required : requiredSkills) {
            if (hasSkill(studentSkills, required)) {
                matched.add(required);
            } else {
                missing.add(required);
            }
        }
        int total = matched.size() + missing.size();
        int percent = total == 0 ? 0 : Math.round(100f * matched.size() / total);
        return new Result(percent, matched, missing);
    }

    /** The percentage the student would reach by learning one more missing skill. */
    public static int percentWithOneMore(Result result) {
        int total = result.matched.size() + result.missing.size();
        if (total == 0 || result.missing.isEmpty()) {
            return result.percent;
        }
        return Math.round(100f * (result.matched.size() + 1) / total);
    }

    /** Same skill, ignoring capital letters. "Android" also matches "Android SDK". */
    private static boolean hasSkill(List<String> studentSkills, String required) {
        if (studentSkills == null || required == null) {
            return false;
        }
        String wanted = normalize(required);
        if (wanted.isEmpty()) {
            return false;
        }
        for (String have : studentSkills) {
            String skill = normalize(have);
            if (skill.equals(wanted)) {
                return true;
            }
            // one name inside the other, but only for names of 3 or more letters
            boolean longEnough = skill.length() >= 3 && wanted.length() >= 3;
            if (longEnough && (skill.contains(wanted) || wanted.contains(skill))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String skill) {
        return skill == null ? "" : skill.trim().toLowerCase(Locale.ROOT);
    }
}
