package com.example.cvforge.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One CV (a student can have several versions). Saved in Firestore and cached in Room.
 * Fields are public so Firestore and Gson can read them without extra code.
 */
public class Cv {

    public static final String TEMPLATE_MODERN = "modern";
    public static final String TEMPLATE_CLASSIC = "classic";
    public static final String TEMPLATE_MINIMAL = "minimal";

    public String id;
    public String ownerUid;
    /** Name of this version, for example "Backend internship". */
    public String title;
    public String template = TEMPLATE_MODERN;
    public long updatedAt;

    // personal info
    public String fullName;
    public String headline;
    public String email;
    public String phone;
    public String location;
    public String summary;

    public List<Education> education = new ArrayList<>();
    public List<String> skills = new ArrayList<>();
    public List<Project> projects = new ArrayList<>();
    public List<Experience> experience = new ArrayList<>();
    public List<Certificate> certificates = new ArrayList<>();
    public List<Link> links = new ArrayList<>();

    // Firestore needs an empty constructor
    public Cv() {
    }

    public static class Education {
        public String school;
        public String degree;
        public String years;

        public Education() {
        }
    }

    public static class Project {
        public String name;
        public String description;
        public List<String> tags = new ArrayList<>();

        public Project() {
        }
    }

    public static class Experience {
        public String company;
        public String role;
        public String years;
        public List<String> bullets = new ArrayList<>();

        public Experience() {
        }
    }

    public static class Certificate {
        public String name;
        public String issuer;
        public String year;

        public Certificate() {
        }
    }

    public static class Link {
        public String label;
        public String url;

        public Link() {
        }
    }
}
