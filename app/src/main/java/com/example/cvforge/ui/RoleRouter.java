package com.example.cvforge.ui;

import android.app.Activity;
import android.content.Intent;

import com.example.cvforge.data.model.User;
import com.example.cvforge.ui.admin.AdminActivity;
import com.example.cvforge.ui.alumni.AlumniActivity;
import com.example.cvforge.ui.auth.PendingActivity;
import com.example.cvforge.ui.company.CompanyActivity;
import com.example.cvforge.ui.student.StudentActivity;
import com.example.cvforge.ui.teacher.TeacherActivity;
import com.example.cvforge.util.AnimUtils;

/** Decides which home screen a signed-in user sees, based on the role in the users document. */
public class RoleRouter {

    private RoleRouter() {
    }

    /** Opens the screen for this user and closes the login screens. */
    public static void open(Activity from, User user) {
        Intent intent = new Intent(from, targetFor(user));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        AnimUtils.fadeTransition(from);
    }

    private static Class<? extends Activity> targetFor(User user) {
        String role = user.getRole();
        if (User.ROLE_ADMIN.equals(role)) {
            return AdminActivity.class;
        }
        // Company and Alumni wait for the admin
        if (User.needsApproval(role) && !user.isVerified()) {
            return PendingActivity.class;
        }
        switch (role) {
            case User.ROLE_COMPANY:
                return CompanyActivity.class;
            case User.ROLE_TEACHER:
                return TeacherActivity.class;
            case User.ROLE_ALUMNI:
                return AlumniActivity.class;
            default:
                return StudentActivity.class;
        }
    }
}
