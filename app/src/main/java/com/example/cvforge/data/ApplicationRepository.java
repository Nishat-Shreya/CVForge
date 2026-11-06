package com.example.cvforge.data;

import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.Internship;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Applications and saved internships of students. They live in the Firestore "applications" collection. */
public class ApplicationRepository {

    /** Callback for one application. The application is null when there is none. */
    public interface ApplicationListener {
        void onLoaded(Application application);
    }

    /** Callback for the live list of applications. Runs on the main thread. */
    public interface ListListener {
        void onChanged(List<Application> applications);

        void onError(String message);
    }

    private static final String APPLICATIONS = "applications";
    private static final String NOTES = "applicationNotes";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Listens to all applications of a student. The listener is called now and again every time
     * something changes in Firestore, also when a company changes a status.
     * Call remove() on the result when the screen stops.
     */
    public ListenerRegistration listen(String studentId, ListListener listener) {
        return db.collection(APPLICATIONS).whereEqualTo("studentId", studentId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (snapshot != null) {
                        listener.onChanged(new ArrayList<>(snapshot.toObjects(Application.class)));
                    }
                });
    }

    /** Listens to all applications to the internships of a poster. Saved internships are left out. */
    public ListenerRegistration listenByPoster(String posterId, ListListener listener) {
        return db.collection(APPLICATIONS).whereEqualTo("posterId", posterId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    List<Application> list = new ArrayList<>();
                    for (Application application : snapshot.toObjects(Application.class)) {
                        if (!Application.STATUS_SAVED.equals(application.status)) {
                            list.add(application);
                        }
                    }
                    listener.onChanged(list);
                });
    }

    /** The poster plans an interview: the status becomes "interview" and the time is saved. */
    public void scheduleInterview(Application application, long interviewAt, ResultListener listener) {
        application.interviewAt = interviewAt;
        long now = System.currentTimeMillis();
        application.changeStatus(Application.STATUS_INTERVIEW, now);
        db.collection(APPLICATIONS).document(application.id)
                .update("status", application.status,
                        "statusHistory", application.statusHistory,
                        "lastChangeAt", application.lastChangeAt,
                        "interviewAt", interviewAt)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Callback for the private notes. Runs on the main thread.
     */
    public interface NotesListener {
        void onLoaded(String notes);
    }

    /**
     * The private notes are kept in their own collection, so a company that can read the
     * application can never read the notes.
     */
    public void loadNotes(String applicationId, NotesListener listener) {
        db.collection(NOTES).document(applicationId).get()
                .addOnSuccessListener(snapshot -> {
                    String notes = snapshot.getString("notes");
                    listener.onLoaded(notes == null ? "" : notes);
                })
                .addOnFailureListener(e -> listener.onLoaded(""));
    }

    /** Saves the private notes of the student. */
    public void saveNotes(String applicationId, String studentId, String notes, ResultListener listener) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("studentId", studentId);
        data.put("notes", notes);
        db.collection(NOTES).document(applicationId).set(data)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Listens to one application. The application is null if it was deleted. */
    public ListenerRegistration listenOne(String applicationId, ApplicationListener listener) {
        return db.collection(APPLICATIONS).document(applicationId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error == null && snapshot != null) {
                        listener.onLoaded(snapshot.toObject(Application.class));
                    }
                });
    }

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

    /**
     * Moves a card to another status, for example when the student drags it to another column.
     * The change is added to the status timeline, so the history stays complete.
     */
    public void changeStatus(Application application, String newStatus, ResultListener listener) {
        long now = System.currentTimeMillis();
        application.changeStatus(newStatus, now);
        if (Application.STATUS_APPLIED.equals(newStatus) && application.appliedAt == 0) {
            application.appliedAt = now;
        }
        db.collection(APPLICATIONS).document(application.id)
                .update("status", application.status,
                        "statusHistory", application.statusHistory,
                        "lastChangeAt", application.lastChangeAt,
                        "appliedAt", application.appliedAt)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
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
