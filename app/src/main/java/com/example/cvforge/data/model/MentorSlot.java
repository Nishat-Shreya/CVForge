package com.example.cvforge.data.model;

/**
 * A mentor slot: a time that an alumni offers for a 30 minute talk (feature 17).
 * One student can book it. Public fields so Firestore can read them without extra code.
 */
public class MentorSlot {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_BOOKED = "booked";

    /** Length of one session in minutes. */
    public static final int LENGTH_MINUTES = 30;

    public String id;
    public String mentorId;
    public String mentorName;
    /** Start time in milliseconds since 1970. */
    public long startAt;
    public String status = STATUS_OPEN;

    public String bookedById;
    public String bookedByName;
    public long bookedAt;

    // Firestore needs an empty constructor
    public MentorSlot() {
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }
}
