package com.example.cvforge.ui;

import android.app.Activity;
import android.content.Intent;

import com.example.cvforge.data.model.User;
import com.example.cvforge.ui.auth.RolePlaceholderActivity;
import com.example.cvforge.util.AnimUtils;

/** Decides which home screen a signed-in user sees, based on the role in the users document. */
public class RoleRouter {

    private RoleRouter() {
    }

    /** Opens the screen for this user and closes the login screens. */
    public static void open(Activity from, User user) {
        // every role has the same temporary screen for now, the real ones come in checkpoint 12
        Intent intent = new Intent(from, RolePlaceholderActivity.class);
        intent.putExtra(RolePlaceholderActivity.EXTRA_ROLE, user.getRole());
        intent.putExtra(RolePlaceholderActivity.EXTRA_NAME, user.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        AnimUtils.fadeTransition(from);
    }
}
