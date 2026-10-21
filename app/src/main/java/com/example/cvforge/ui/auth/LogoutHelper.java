package com.example.cvforge.ui.auth;

import android.app.Activity;
import android.content.Intent;

import com.example.cvforge.data.UserRepository;
import com.example.cvforge.util.AnimUtils;

/** Signs out and goes back to the Welcome screen. Used by every screen that has a Log out button. */
public class LogoutHelper {

    private LogoutHelper() {
    }

    public static void logOut(Activity from) {
        new UserRepository().logout();
        Intent intent = new Intent(from, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        AnimUtils.fadeTransition(from);
    }
}
