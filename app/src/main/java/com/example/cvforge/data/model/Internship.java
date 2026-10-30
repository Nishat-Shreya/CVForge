package com.example.cvforge.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One document in the Firestore "internships" collection.
 * Public fields so Firestore and Gson can read them without extra code.
 */
public class Internship {

    public static final String TYPE_REMOTE = "remote";
    public static final String TYPE_ONSITE = "onsite";
    public static final String TYPE_HYBRID = "hybrid";

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_CLOSED = "closed";

    public String id;

    // who posted it: a Company, Teacher or Alumni
    public String posterId;
    public String posterName;
    public String posterRole;

    public String title;
    public String description;
    public List<String> skills = new ArrayList<>();
    /** Monthly stipend. 0 means unpaid. */
    public int stipend;
    public String location;
    public String type = TYPE_ONSITE;
    /** Last day to apply, in milliseconds since 1970. */
    public long deadline;
    public String status = STATUS_OPEN;
    public long createdAt;

    // Firestore needs an empty constructor
    public Internship() {
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }
}
