package com.example.cvforge.data;

import com.example.cvforge.data.model.User;

/** Callback for loading a user document. */
public interface UserListener {

    void onUser(User user);

    void onError(String message);
}
