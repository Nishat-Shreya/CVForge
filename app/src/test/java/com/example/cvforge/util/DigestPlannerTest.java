package com.example.cvforge.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.cvforge.data.model.Application;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

public class DigestPlannerTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long HOUR = 60L * 60 * 1000;

    /** 8:00 in the morning on 20 October 2026. */
    private static long morning() {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(2026, Calendar.OCTOBER, 20, 8, 0, 0);
        return calendar.getTimeInMillis();
    }

    private Application application(String status) {
        Application application = new Application();
        application.status = status;
        return application;
    }

    @Test
    public void twoDeadlinesAndOneInterview_areCounted() {
        long now = morning();
        Application deadline1 = application(Application.STATUS_SAVED);
        deadline1.deadline = now + 6 * HOUR;
        Application deadline2 = application(Application.STATUS_SAVED);
        deadline2.deadline = now + 12 * HOUR;
        Application interview = application(Application.STATUS_INTERVIEW);
        interview.interviewAt = now + 3 * HOUR;

        DigestPlanner.Counts counts = DigestPlanner.countToday(Arrays.asList(deadline1, deadline2, interview), now, UTC);

        assertEquals(2, counts.deadlines);
        assertEquals(1, counts.interviews);
    }

    @Test
    public void tomorrowAndYesterday_areNotCounted() {
        long now = morning();
        Application tomorrow = application(Application.STATUS_SAVED);
        tomorrow.deadline = now + 20 * HOUR; // 4:00 the next morning
        Application yesterday = application(Application.STATUS_INTERVIEW);
        yesterday.interviewAt = now - 10 * HOUR;

        assertTrue(DigestPlanner.countToday(Arrays.asList(tomorrow, yesterday), now, UTC).isEmpty());
    }

    @Test
    public void appliedInternships_haveNoDeadlineInTheDigest() {
        long now = morning();
        Application applied = application(Application.STATUS_APPLIED);
        applied.deadline = now + 5 * HOUR;

        assertTrue(DigestPlanner.countToday(Collections.singletonList(applied), now, UTC).isEmpty());
    }
}
