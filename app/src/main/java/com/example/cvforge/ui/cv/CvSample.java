package com.example.cvforge.ui.cv;

import com.example.cvforge.data.model.Cv;

/** Example CV shown in the preview until the student saves their own. */
public class CvSample {

    private CvSample() {
    }

    public static Cv create() {
        Cv cv = new Cv();
        cv.fullName = "Your Name";
        cv.headline = "Android Developer";
        cv.email = "you@example.com";
        cv.location = "Your City";
        cv.summary = "A short summary about you appears here. Tap Edit CV to write your own.";
        cv.skills.add("Java");
        cv.skills.add("Room");
        cv.skills.add("Firebase");
        cv.skills.add("Git");

        Cv.Education education = new Cv.Education();
        education.school = "Your University";
        education.degree = "BSc in Computer Science";
        education.years = "2022 - 2026";
        cv.education.add(education);

        Cv.Project project = new Cv.Project();
        project.name = "Offline Diary App";
        project.description = "Room database, notes sync with Firebase, dark mode.";
        project.tags.add("Java");
        project.tags.add("Room");
        project.tags.add("Firebase");
        cv.projects.add(project);
        return cv;
    }
}
