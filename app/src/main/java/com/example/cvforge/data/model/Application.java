package com.example.cvforge.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A student's application to one internship. One document in the Firestore "applications"
 * collection. The same document is used for a saved internship (status "saved").
 */
public class Application {

    // the six statuses of the data model
    public static final String STATUS_SAVED = "saved";
    public static final String STATUS_APPLIED = "applied";
    public static final String STATUS_SHORTLISTED = "shortlisted";
    public static final String STATUS_INTERVIEW = "interview";
    public static final String STATUS_OFFER = "offer";
    public static final String STATUS_REJECTED = "rejected";

    /** One entry of the status timeline. */
    public static class StatusChange {
        public String status;
        public long at;

        public StatusChange() {
        }

        public StatusChange(String status, long at) {
            this.status = status;
            this.at = at;
        }
    }

    /** studentId + "_" + internshipId, so a student can apply to an internship only once. */
    public String id;
    public String studentId;
    public String studentName;
    public String internshipId;
    /** The CV version the student applied with. */
    public String cvId;

    // a copy of the internship, so the tracker can show it without loading the post
    public String internshipTitle;
    public String companyName;
    /** Who posted the internship. The poster sees this application. */
    public String posterId;

    public String status = STATUS_SAVED;
    /**
     * A copy of the CV the student applied with. The company reads it here. The CV itself stays
     * private, so a CV is shared only when the student applies. (null for saved internships)
     */
    public Cv cv;
    public List<StatusChange> statusHistory = new ArrayList<>();
    public long createdAt;
    public long appliedAt;
    /** Time of the last status change. The Ghosting Detector reads this. */
    public long lastChangeAt;
    /** When the interview is, set by the company (0 = no interview yet). */
    public long interviewAt;
    /** Deadline of the internship, copied when the student saves it. */
    public long deadline;

    // Firestore needs an empty constructor
    public Application() {
    }

    public static String idFor(String studentId, String internshipId) {
        return studentId + "_" + internshipId;
    }

    /** The Kanban board has four columns. Shortlisted counts as Applied. */
    public static String columnOf(String status) {
        if (STATUS_SAVED.equals(status)) {
            return STATUS_SAVED;
        }
        if (STATUS_INTERVIEW.equals(status)) {
            return STATUS_INTERVIEW;
        }
        if (STATUS_OFFER.equals(status)) {
            return STATUS_OFFER;
        }
        return STATUS_APPLIED;
    }

    /**
     * The column this card is in. A rejected card stays in the column where it was rejected,
     * which is the status before "rejected" in the timeline.
     */
    public String column() {
        if (!STATUS_REJECTED.equals(status)) {
            return columnOf(status);
        }
        for (int i = statusHistory.size() - 1; i >= 0; i--) {
            String before = statusHistory.get(i).status;
            if (!STATUS_REJECTED.equals(before)) {
                return columnOf(before);
            }
        }
        return STATUS_APPLIED;
    }

    /** True when the company has not answered for 7 days after the student applied (feature 13). */
    public boolean isGhosted(long now) {
        boolean waiting = STATUS_APPLIED.equals(status);
        return waiting && now - lastChangeAt >= 7L * 24 * 60 * 60 * 1000;
    }

    /** Sets a new status and adds it to the timeline. */
    public void changeStatus(String newStatus, long now) {
        status = newStatus;
        lastChangeAt = now;
        statusHistory.add(new StatusChange(newStatus, now));
    }
}
