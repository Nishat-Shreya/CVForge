package com.example.cvforge.ui;

import android.app.Activity;
import android.content.Intent;

import com.example.cvforge.data.model.User;
import com.example.cvforge.ui.admin.AdminActivity;
import com.example.cvforge.ui.auth.PendingActivity;
import com.example.cvforge.ui.auth.RolePlaceholderActivity;
import com.example.cvforge.ui.student.StudentActivity;
import com.example.cvforge.util.AnimUtils;

/** Decides which home screen a signed-in user sees, based on the role in the users document. */
public class RoleRouter {

    private RoleRouter() {
    }

    /** Opens the screen for this user and closes the login screens. */
    public static void open(Activity from, User user) {
        Intent intent = new Intent(from, targetFor(user));
        intent.putExtra(RolePlaceholderActivity.EXTRA_ROLE, user.getRole());
        intent.putExtra(RolePlaceholderActivity.EXTRA_NAME, user.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        AnimUtils.fadeTransition(from);
    }

    private static Class<? extends Activity> targetFor(User user) {
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            return AdminActivity.class;
        }
        // Company and Alumni wait for the admin
        if (User.needsApproval(user.getRole()) && !user.isVerified()) {
            return PendingActivity.class;
        }
        if (User.ROLE_STUDENT.equals(user.getRole())) {
            return StudentActivity.class;
        }
        // the real screens for Company, Teacher and Alumni come in checkpoint 12
        return RolePlaceholderActivity.class;
    }
}
