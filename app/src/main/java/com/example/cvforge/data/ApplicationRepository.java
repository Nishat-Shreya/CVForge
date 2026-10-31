package com.example.cvforge.data;

import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.Internship;
import com.google.firebase.firestore.FirebaseFirestore;

/** Applications and saved internships of students. They live in the Firestore "applications" collection. */
public class ApplicationRepository {

    /** Callback for one application. The application is null when there is none. */
    public interface ApplicationListener {
        void onLoaded(Application application);
    }

    private static final String APPLICATIONS = "applications";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Loads the application of a student for one internship, or null if there is none. */
    public void load(String studentId, String internshipId, ApplicationListener listener) {
        String id = Application.idFor(studentId, internshipId);
        db.collection(APPLICATIONS).document(id).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(snapshot.toObject(Application.class)))
                .addOnFailureListener(e -> listener.onLoaded(null));
    }

    /** Saves an internship for later (status "saved"). Does nothing if the student already has it. */
    public void save(String studentId, String studentName, Internship internship, ResultListener listener) {
        load(studentId, internship.id, existing -> {
            if (existing != null) {
                listener.onSuccess();
                return;
            }
            Application application = newApplication(studentId, studentName, internship);
            application.changeStatus(Application.STATUS_SAVED, System.currentTimeMillis());
            write(application, listener);
        });
    }

    /**
     * Applies to an internship with a CV. A saved internship becomes "applied".
     * Applying a second time is refused.
     */
    public void apply(String studentId, String studentName, Internship internship, Cv cv, ResultListener listener) {
        load(studentId, internship.id, existing -> {
            if (existing != null && !Application.STATUS_SAVED.equals(existing.status)) {
                listener.onError("You already applied to this internship.");
                return;
            }
            Application application = existing != null ? existing : newApplication(studentId, studentName, internship);
            long now = System.currentTimeMillis();
            application.cvId = cv.id;
            application.cv = cv;
            application.appliedAt = now;
            application.changeStatus(Application.STATUS_APPLIED, now);
            write(application, listener);
        });
    }

    private Application newApplication(String studentId, String studentName, Internship internship) {
        Application application = new Application();
        application.id = Application.idFor(studentId, internship.id);
        application.studentId = studentId;
        application.studentName = studentName;
        application.internshipId = internship.id;
        application.internshipTitle = internship.title;
        application.companyName = internship.posterName;
        application.posterId = internship.posterId;
        application.createdAt = System.currentTimeMillis();
        application.deadline = internship.deadline;
        return application;
    }

    private void write(Application application, ResultListener listener) {
        db.collection(APPLICATIONS).document(application.id).set(application)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
}
