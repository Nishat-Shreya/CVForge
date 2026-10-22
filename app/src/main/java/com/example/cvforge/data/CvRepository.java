package com.example.cvforge.data;

import android.content.Context;

import com.example.cvforge.data.local.AppDatabase;
import com.example.cvforge.data.local.CvDao;
import com.example.cvforge.data.local.CvEntity;
import com.example.cvforge.data.model.Cv;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * CVs live in two places: Firestore (the real copy) and Room (a copy on the phone,
 * so the CV opens without internet and a draft is not lost).
 */
public class CvRepository {

    private static final String CVS = "cvs";

    private final CvDao cvDao;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CvRepository(Context context) {
        cvDao = AppDatabase.getInstance(context).cvDao();
    }

    /**
     * Saves to Room first (always works), then to Firestore.
     * The listener hears about the Firestore result.
     */
    public void save(Cv cv, ResultListener listener) {
        CvEntity entity = CvEntity.fromCv(cv);
        AppDatabase.databaseExecutor.execute(() -> {
            cvDao.insert(entity);
            db.collection(CVS).document(cv.id).set(cv)
                    .addOnSuccessListener(unused -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        });
    }

    /** Saves a draft in Room only. Used when the screen is left (onPause). */
    public void saveDraft(Cv cv) {
        CvEntity entity = CvEntity.fromCv(cv);
        AppDatabase.databaseExecutor.execute(() -> cvDao.insert(entity));
    }

    /**
     * Loads the newest CV of a student. It compares the Room copy with the Firestore copy
     * and returns the newer one. With no internet the Room copy is returned.
     */
    public void loadLatest(String ownerUid, CvListener listener) {
        AppDatabase.databaseExecutor.execute(() -> {
            List<CvEntity> saved = cvDao.getAllForOwner(ownerUid);
            Cv local = saved.isEmpty() ? null : saved.get(0).toCv();

            db.collection(CVS).whereEqualTo("ownerUid", ownerUid).get()
                    .addOnSuccessListener(snapshot -> {
                        Cv cloud = newest(snapshot.toObjects(Cv.class));
                        if (cloud != null && (local == null || cloud.updatedAt > local.updatedAt)) {
                            CvEntity entity = CvEntity.fromCv(cloud);
                            AppDatabase.databaseExecutor.execute(() -> cvDao.insert(entity));
                            listener.onLoaded(cloud);
                        } else {
                            listener.onLoaded(local);
                        }
                    })
                    .addOnFailureListener(e -> listener.onLoaded(local));
        });
    }

    private Cv newest(List<Cv> cvs) {
        Cv result = null;
        for (Cv cv : cvs) {
            if (result == null || cv.updatedAt > result.updatedAt) {
                result = cv;
            }
        }
        return result;
    }
}
