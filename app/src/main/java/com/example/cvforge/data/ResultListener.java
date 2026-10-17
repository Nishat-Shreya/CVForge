package com.example.cvforge.data;

/** Simple callback for repository calls that finish later (Firebase works in the background). */
public interface ResultListener {

    void onSuccess();

    void onError(String message);
}
