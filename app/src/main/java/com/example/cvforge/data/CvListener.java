package com.example.cvforge.data;

import com.example.cvforge.data.model.Cv;

/** Callback for loading a CV. Always runs on the main thread. */
public interface CvListener {

    /** The CV, or null when the student has not saved one yet. */
    void onLoaded(Cv cv);
}
