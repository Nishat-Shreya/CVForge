package com.example.cvforge.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.cvforge.data.local.AppDatabase;
import com.example.cvforge.data.local.CvDao;
import com.example.cvforge.data.local.CvEntity;
import com.example.cvforge.data.model.Cv;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CVs live in two places: Firestore (the real copy) and Room (a copy on the phone,
 * so the CV opens without internet and a draft is not lost).
 */
public class CvRepository {

    private static final String CVS = "cvs";

    private final CvDao cvDao;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

    /**
     * Loads every CV version of a student, newest first. When the same CV exists in Room and
     * in Firestore, the newer copy is used. With no internet only the Room copies are returned.
     */
    public void loadAll(String ownerUid, CvListListener listener) {
        AppDatabase.databaseExecutor.execute(() -> {
            Map<String, Cv> byId = new HashMap<>();
            for (CvEntity entity : cvDao.getAllForOwner(ownerUid)) {
                Cv cv = entity.toCv();
                byId.put(cv.id, cv);
            }

            db.collection(CVS).whereEqualTo("ownerUid", ownerUid).get()
                    .addOnSuccessListener(snapshot -> {
                        List<CvEntity> toCache = new ArrayList<>();
                        for (Cv cloud : snapshot.toObjects(Cv.class)) {
                            Cv local = byId.get(cloud.id);
                            if (local == null || cloud.updatedAt > local.updatedAt) {
                                byId.put(cloud.id, cloud);
                                toCache.add(CvEntity.fromCv(cloud));
                            }
                        }
                        AppDatabase.databaseExecutor.execute(() -> cvDao.insertAll(toCache));
                        listener.onLoaded(sortedNewestFirst(byId));
                    })
                    .addOnFailureListener(e -> listener.onLoaded(sortedNewestFirst(byId)));
        });
    }

    /** Loads one CV by id. Room is tried first, then Firestore. The listener gets null if it is not found. */
    public void loadById(String id, CvListener listener) {
        AppDatabase.databaseExecutor.execute(() -> {
            CvEntity entity = cvDao.getById(id);
            if (entity != null) {
                Cv cv = entity.toCv();
                mainHandler.post(() -> listener.onLoaded(cv));
                return;
            }
            db.collection(CVS).document(id).get()
                    .addOnSuccessListener(snapshot -> listener.onLoaded(snapshot.toObject(Cv.class)))
                    .addOnFailureListener(e -> listener.onLoaded(null));
        });
    }

    /** Deletes a CV from Room and from Firestore. */
    public void delete(Cv cv) {
        CvEntity entity = CvEntity.fromCv(cv);
        AppDatabase.databaseExecutor.execute(() -> cvDao.delete(entity));
        db.collection(CVS).document(cv.id).delete();
    }

    private List<Cv> sortedNewestFirst(Map<String, Cv> byId) {
        List<Cv> list = new ArrayList<>(byId.values());
        Collections.sort(list, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return list;
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
