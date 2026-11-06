package com.example.cvforge.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.cvforge.data.model.Application;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReminderPlannerTest {

    private static final long NOW = 1_000_000_000_000L;
    private static final long HOUR = 60L * 60 * 1000;
    private static final long DAY = 24 * HOUR;

    private Application application(String id, String status) {
        Application application = new Application();
        application.id = id;
        application.status = status;
        application.companyName = "Nexa Labs";
        application.internshipTitle = "Android Intern";
        return application;
    }

    @Test
    public void interview_getsAReminderOneHourBefore() {
        Application interview = application("a1", Application.STATUS_INTERVIEW);
        interview.interviewAt = NOW + 5 * HOUR;

        List<ReminderPlanner.Reminder> reminders = ReminderPlanner.plan(Collections.singletonList(interview), NOW);

        assertEquals(1, reminders.size());
        assertEquals(ReminderPlanner.TYPE_INTERVIEW, reminders.get(0).type);
        assertEquals(NOW + 4 * HOUR, reminders.get(0).triggerAt);
    }

    @Test
    public void savedInternship_getsAReminderOneDayBeforeTheDeadline() {
        Application saved = application("a2", Application.STATUS_SAVED);
        saved.deadline = NOW + 3 * DAY;

        List<ReminderPlanner.Reminder> reminders = ReminderPlanner.plan(Collections.singletonList(saved), NOW);

        assertEquals(1, reminders.size());
        assertEquals(ReminderPlanner.TYPE_DEADLINE, reminders.get(0).type);
        assertEquals(NOW + 2 * DAY, reminders.get(0).triggerAt);
    }

    @Test
    public void reminderTimesInThePast_areLeftOut() {
        Application soon = application("a3", Application.STATUS_INTERVIEW);
        soon.interviewAt = NOW + 30 * 60 * 1000; // in 30 minutes, the reminder time has passed
        Application late = application("a4", Application.STATUS_SAVED);
        late.deadline = NOW + 3 * HOUR;

        assertTrue(ReminderPlanner.plan(Arrays.asList(soon, late), NOW).isEmpty());
    }

    @Test
    public void appliedOrRejected_getNoReminder() {
        Application applied = application("a5", Application.STATUS_APPLIED);
        applied.deadline = NOW + 3 * DAY;
        Application rejected = application("a6", Application.STATUS_REJECTED);
        rejected.interviewAt = NOW + 5 * HOUR;

        assertTrue(ReminderPlanner.plan(Arrays.asList(applied, rejected), NOW).isEmpty());
    }

    @Test
    public void sameApplicationAndType_alwaysGiveTheSameCode() {
        assertEquals(ReminderPlanner.codeFor("x", "interview"), ReminderPlanner.codeFor("x", "interview"));
        assertTrue(ReminderPlanner.codeFor("x", "interview") != ReminderPlanner.codeFor("x", "deadline"));
    }
}
