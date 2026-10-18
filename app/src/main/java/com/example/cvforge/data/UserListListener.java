package com.example.cvforge.data;

import com.example.cvforge.data.model.User;

import java.util.List;

/** Callback for loading several user documents. */
public interface UserListListener {

    void onUsers(List<User> users);

    void onError(String message);
}
