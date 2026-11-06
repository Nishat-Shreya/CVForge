package com.example.cvforge.util;

import com.example.cvforge.data.model.Application;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/** Counts what happens today, for the morning digest. It only works with data, so it is easy to test. */
public class DigestPlanner {

    /** How many deadlines and interviews are today. */
    public static class Counts {
        public final int deadlines;
        public final int interviews;

        Counts(int deadlines, int interviews) {
            this.deadlines = deadlines;
            this.interviews = interviews;
        }

        public boolean isEmpty() {
            return deadlines == 0 && interviews == 0;
        }
    }

    private DigestPlanner() {
    }

    /**
     * Deadlines of saved internships and planned interviews that fall on the same day as now.
     * A deadline counts only while the internship is still saved (not applied yet).
     */
    public static Counts countToday(List<Application> applications, long now, TimeZone timeZone) {
        long dayStart = startOfDay(now, timeZone);
        long dayEnd = dayStart + 24L * 60 * 60 * 1000;

        int deadlines = 0;
        int interviews = 0;
        for (Application application : applications) {
            boolean saved = Application.STATUS_SAVED.equals(application.status);
            if (saved && inside(application.deadline, dayStart, dayEnd, now)) {
                deadlines++;
            }
            boolean interviewPlanned = Application.STATUS_INTERVIEW.equals(application.status);
            if (interviewPlanned && inside(application.interviewAt, dayStart, dayEnd, now)) {
                interviews++;
            }
        }
        return new Counts(deadlines, interviews);
    }

    /** Today and not yet over. */
    private static boolean inside(long time, long dayStart, long dayEnd, long now) {
        return time > 0 && time >= dayStart && time < dayEnd && time >= now - 60 * 60 * 1000;
    }

    private static long startOfDay(long now, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
