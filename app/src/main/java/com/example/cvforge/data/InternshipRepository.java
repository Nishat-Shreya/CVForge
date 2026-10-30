package com.example.cvforge.data;

import com.example.cvforge.data.model.Internship;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Internships posted inside CVForge. They live in the Firestore "internships" collection. */
public class InternshipRepository {

    /** Callback for one internship. The internship is null when it was not found. */
    public interface InternshipListener {
        void onLoaded(Internship internship);
    }

    /** Callback for a list of internships, newest first. */
    public interface InternshipListListener {
        void onLoaded(List<Internship> internships);

        void onError(String message);
    }

    private static final String INTERNSHIPS = "internships";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Saves a post. A new post gets its document id and creation time here.
     * An existing post (same id) is overwritten with the new values.
     */
    public void post(Internship internship, ResultListener listener) {
        if (internship.id == null) {
            internship.id = db.collection(INTERNSHIPS).document().getId();
        }
        if (internship.createdAt == 0) {
            internship.createdAt = System.currentTimeMillis();
        }

        db.collection(INTERNSHIPS).document(internship.id).set(internship)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Loads the posts of one poster, newest first. */
    public void loadByPoster(String posterId, InternshipListListener listener) {
        db.collection(INTERNSHIPS).whereEqualTo("posterId", posterId).get()
                .addOnSuccessListener(snapshot -> {
                    List<Internship> list = new ArrayList<>(snapshot.toObjects(Internship.class));
                    Collections.sort(list, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                    listener.onLoaded(list);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void loadById(String id, InternshipListener listener) {
        db.collection(INTERNSHIPS).document(id).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(snapshot.toObject(Internship.class)))
                .addOnFailureListener(e -> listener.onLoaded(null));
    }

    /** Closes a post (students no longer see it) or opens it again. */
    public void setStatus(String id, String status, ResultListener listener) {
        db.collection(INTERNSHIPS).document(id).update("status", status)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void delete(String id, ResultListener listener) {
        db.collection(INTERNSHIPS).document(id).delete()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
}
