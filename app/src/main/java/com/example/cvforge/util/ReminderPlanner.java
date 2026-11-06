package com.example.cvforge.util;

import com.example.cvforge.data.model.Application;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides which reminders the student should get. It only works with data, so it is easy to test.
 * ReminderScheduler turns the plan into alarms.
 */
public class ReminderPlanner {

    public static final String TYPE_INTERVIEW = "interview";
    public static final String TYPE_DEADLINE = "deadline";

    private static final long HOUR_MS = 60L * 60 * 1000;
    /** An interview reminder comes 1 hour before. */
    public static final long INTERVIEW_LEAD_MS = HOUR_MS;
    /** A deadline reminder comes 1 day before. */
    public static final long DEADLINE_LEAD_MS = 24 * HOUR_MS;

    /** One reminder. */
    public static class Reminder {
        /** The same application and type always give the same code, so a new alarm replaces the old one. */
        public final int code;
        public final long triggerAt;
        public final String type;
        public final String companyName;
        public final String jobTitle;

        Reminder(int code, long triggerAt, String type, String companyName, String jobTitle) {
            this.code = code;
            this.triggerAt = triggerAt;
            this.type = type;
            this.companyName = companyName;
            this.jobTitle = jobTitle;
        }
    }

    private ReminderPlanner() {
    }

    /** Makes the list of reminders that are still in the future. */
    public static List<Reminder> plan(List<Application> applications, long now) {
        List<Reminder> reminders = new ArrayList<>();
        for (Application application : applications) {
            // an interview: 1 hour before
            long interviewReminder = application.interviewAt - INTERVIEW_LEAD_MS;
            boolean interviewPlanned = Application.STATUS_INTERVIEW.equals(application.status);
            if (interviewPlanned && application.interviewAt > 0 && interviewReminder > now) {
                reminders.add(new Reminder(codeFor(application.id, TYPE_INTERVIEW), interviewReminder,
                        TYPE_INTERVIEW, application.companyName, application.internshipTitle));
            }

            // a saved internship whose deadline is coming: 1 day before
            long deadlineReminder = application.deadline - DEADLINE_LEAD_MS;
            boolean stillSaved = Application.STATUS_SAVED.equals(application.status);
            if (stillSaved && application.deadline > 0 && deadlineReminder > now) {
                reminders.add(new Reminder(codeFor(application.id, TYPE_DEADLINE), deadlineReminder,
                        TYPE_DEADLINE, application.companyName, application.internshipTitle));
            }
        }
        return reminders;
    }

    static int codeFor(String applicationId, String type) {
        return (applicationId + "#" + type).hashCode();
    }
}
