package com.example.cvforge.data.model;

/**
 * One skill endorsement (feature 15). A student asks a Teacher or Alumni to endorse a skill.
 * Only an approved endorsement is shown with the Verified chip.
 * Public fields so Firestore can read them without extra code.
 */
public class Endorsement {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_DECLINED = "declined";

    public String id;
    public String studentId;
    public String studentName;
    public String skill;

    public String endorserId;
    public String endorserName;
    /** teacher or alumni */
    public String endorserRole;

    public String status = STATUS_PENDING;
    /** The comment of the endorser, written when approving. */
    public String comment;
    public long createdAt;
    public long endorsedAt;

    // Firestore needs an empty constructor
    public Endorsement() {
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }
}
