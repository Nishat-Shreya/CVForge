package com.example.cvforge.data;

import com.example.cvforge.data.model.MentorSlot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mentor slots of alumni, in the Firestore "mentorSlots" collection. */
public class MentorRepository {

    public interface ListListener {
        void onLoaded(List<MentorSlot> slots);

        void onError(String message);
    }

    private static final String SLOTS = "mentorSlots";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** An alumni offers a new time. */
    public void addSlot(String mentorId, String mentorName, long startAt, ResultListener listener) {
        MentorSlot slot = new MentorSlot();
        slot.id = db.collection(SLOTS).document().getId();
        slot.mentorId = mentorId;
        slot.mentorName = mentorName;
        slot.startAt = startAt;
        db.collection(SLOTS).document(slot.id).set(slot)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** All slots of one alumni, soonest first. */
    public void loadByMentor(String mentorId, ListListener listener) {
        db.collection(SLOTS).whereEqualTo("mentorId", mentorId).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(soonestFirst(snapshot.toObjects(MentorSlot.class), false)))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Open slots in the future, soonest first. */
    public void loadOpen(ListListener listener) {
        db.collection(SLOTS).whereEqualTo("status", MentorSlot.STATUS_OPEN).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(soonestFirst(snapshot.toObjects(MentorSlot.class), true)))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** The slots that one student booked. */
    public void loadBookedBy(String studentId, ListListener listener) {
        db.collection(SLOTS).whereEqualTo("bookedById", studentId).get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(soonestFirst(snapshot.toObjects(MentorSlot.class), false)))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Books a slot. A transaction reads the slot first, so two students cannot book the
     * same time: the second one gets the message that the slot is taken.
     */
    public void book(MentorSlot slot, String studentId, String studentName, ResultListener listener) {
        DocumentReference ref = db.collection(SLOTS).document(slot.id);
        db.runTransaction(transaction -> {
            MentorSlot current = transaction.get(ref).toObject(MentorSlot.class);
            if (current == null || !current.isOpen()) {
                throw new IllegalStateException("This slot was just booked by someone else.");
            }
            transaction.update(ref,
                    "status", MentorSlot.STATUS_BOOKED,
                    "bookedById", studentId,
                    "bookedByName", studentName,
                    "bookedAt", System.currentTimeMillis());
            return null;
        }).addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** The alumni removes a slot that nobody booked. */
    public void deleteSlot(String slotId, ResultListener listener) {
        db.collection(SLOTS).document(slotId).delete()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    private List<MentorSlot> soonestFirst(List<MentorSlot> list, boolean futureOnly) {
        long now = System.currentTimeMillis();
        List<MentorSlot> result = new ArrayList<>();
        for (MentorSlot slot : list) {
            if (!futureOnly || slot.startAt >= now) {
                result.add(slot);
            }
        }
        Collections.sort(result, (a, b) -> Long.compare(a.startAt, b.startAt));
        return result;
    }
}
