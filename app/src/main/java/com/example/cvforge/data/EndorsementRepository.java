package com.example.cvforge.data;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.Endorsement;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads and writes the Firestore "endorsements" collection. */
public class EndorsementRepository {

    public interface ListListener {
        void onLoaded(List<Endorsement> endorsements);

        void onError(String message);
    }

    private static final String ENDORSEMENTS = "endorsements";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** The student asks an endorser to endorse one skill. */
    public void request(Endorsement endorsement, ResultListener listener) {
        endorsement.id = db.collection(ENDORSEMENTS).document().getId();
        endorsement.status = Endorsement.STATUS_PENDING;
        endorsement.createdAt = System.currentTimeMillis();
        db.collection(ENDORSEMENTS).document(endorsement.id).set(endorsement)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** All requests of one student (pending, approved and declined), newest first. */
    public void loadForStudent(String studentId, ListListener listener) {
        db.collection(ENDORSEMENTS).whereEqualTo("studentId", studentId).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(newestFirst(snapshot.toObjects(Endorsement.class))))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Only the approved endorsements of a student. Anyone signed in may read these. */
    public void loadApproved(String studentId, ListListener listener) {
        db.collection(ENDORSEMENTS).whereEqualTo("studentId", studentId)
                .whereEqualTo("status", Endorsement.STATUS_APPROVED).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(newestFirst(snapshot.toObjects(Endorsement.class))))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Puts the endorsed skills of the CV owner into cv.verifiedSkills, then calls onDone
     * (also when loading fails, then the list stays as it was).
     */
    public void fillVerifiedSkills(Cv cv, String studentId, Runnable onDone) {
        loadApproved(studentId, new ListListener() {
            @Override
            public void onLoaded(List<Endorsement> endorsements) {
                List<String> skills = new ArrayList<>();
                for (Endorsement endorsement : endorsements) {
                    if (!skills.contains(endorsement.skill)) {
                        skills.add(endorsement.skill);
                    }
                }
                cv.verifiedSkills = skills;
                onDone.run();
            }

            @Override
            public void onError(String message) {
                onDone.run();
            }
        });
    }

    /** The requests that wait for one teacher or alumni. */
    public void loadPendingForEndorser(String endorserId, ListListener listener) {
        db.collection(ENDORSEMENTS).whereEqualTo("endorserId", endorserId)
                .whereEqualTo("status", Endorsement.STATUS_PENDING).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(newestFirst(snapshot.toObjects(Endorsement.class))))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** The endorsements that one teacher or alumni approved (for the dashboard number). */
    public void loadApprovedBy(String endorserId, ListListener listener) {
        db.collection(ENDORSEMENTS).whereEqualTo("endorserId", endorserId)
                .whereEqualTo("status", Endorsement.STATUS_APPROVED).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(newestFirst(snapshot.toObjects(Endorsement.class))))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** The endorser approves (with a comment) or declines a request. */
    public void respond(Endorsement endorsement, boolean approve, String comment, ResultListener listener) {
        db.collection(ENDORSEMENTS).document(endorsement.id)
                .update("status", approve ? Endorsement.STATUS_APPROVED : Endorsement.STATUS_DECLINED,
                        "comment", comment,
                        "endorsedAt", System.currentTimeMillis())
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    private List<Endorsement> newestFirst(List<Endorsement> list) {
        List<Endorsement> sorted = new ArrayList<>(list);
        Collections.sort(sorted, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return sorted;
    }
}
