package com.example.cvforge.data;

import com.example.cvforge.data.model.Cv;

import java.util.List;

/** Callback for loading all CV versions of a student. Always runs on the main thread. */
public interface CvListListener {

    /** Newest first. The list is empty when the student has no CV yet. */
    void onLoaded(List<Cv> cvs);
}
