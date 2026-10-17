package com.example.cvforge.data.model;

/**
 * One document in the Firestore "users" collection.
 * The document id is the Firebase Auth uid.
 */
public class User {

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_COMPANY = "company";
    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_ALUMNI = "alumni";

    private String uid;
    private String name;
    private String email;
    private String role;
    // Company and Alumni stay false until an admin approves them
    private boolean verified;

    // Firestore needs an empty constructor
    public User() {
    }

    public User(String uid, String name, String email, String role, boolean verified) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.verified = verified;
    }

    /** Company and Alumni need admin approval. Student and Teacher are verified at sign up. */
    public static boolean needsApproval(String role) {
        return ROLE_COMPANY.equals(role) || ROLE_ALUMNI.equals(role);
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
