package com.example.cvforge.data;

import com.example.cvforge.data.model.Internship;
import com.google.firebase.firestore.FirebaseFirestore;

/** Internships posted inside CVForge. They live in the Firestore "internships" collection. */
public class InternshipRepository {

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
}
