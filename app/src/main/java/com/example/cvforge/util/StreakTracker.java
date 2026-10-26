package com.example.cvforge.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.TimeZone;

/** Counts how many days in a row the student opened Home. Saved in SharedPreferences. */
public class StreakTracker {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private StreakTracker() {
    }

    /** Call when Home opens. Returns the current streak (at least 1). */
    public static int recordVisit(Context context, String uid) {
        SharedPreferences prefs = context.getSharedPreferences("streak_" + uid, Context.MODE_PRIVATE);
        long today = currentDay();
        long lastDay = prefs.getLong("last_day", -1);
        int streak = prefs.getInt("streak", 0);

        if (lastDay == today) {
            return Math.max(streak, 1);
        }
        streak = (lastDay == today - 1) ? streak + 1 : 1;
        prefs.edit().putLong("last_day", today).putInt("streak", streak).apply();
        return streak;
    }

    /** The saved streak, without counting a visit. It is 0 when the streak is broken. */
    public static int current(Context context, String uid) {
        SharedPreferences prefs = context.getSharedPreferences("streak_" + uid, Context.MODE_PRIVATE);
        long lastDay = prefs.getLong("last_day", -1);
        boolean alive = lastDay == currentDay() || lastDay == currentDay() - 1;
        return alive ? prefs.getInt("streak", 0) : 0;
    }

    /** Days since 1970 in the phone's time zone. */
    private static long currentDay() {
        long now = System.currentTimeMillis();
        return (now + TimeZone.getDefault().getOffset(now)) / DAY_MS;
    }
}
