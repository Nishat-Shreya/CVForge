package com.example.cvforge.util;

import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;

/**
 * Runtime permission for notifications (W10 permissions). Android 13 and newer ask the user.
 * Older versions allow notifications without asking, so nothing happens there.
 *
 * Create it in onCreate of the Activity (the launcher must be registered before the screen
 * starts), then call requestIfNeeded().
 */
public class NotificationPermission {

    /** The name is written as text, because the constant exists only from Android 13. */
    private static final String PERMISSION = "android.permission.POST_NOTIFICATIONS";

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String> launcher;

    public NotificationPermission(AppCompatActivity activity) {
        this.activity = activity;
        // the answer of the user is not needed here: without it, notifications are simply not shown
        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> { });
    }

    /** Explains in one sentence why the permission is needed, then asks. Asks only once per screen. */
    public void requestIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        boolean granted = ContextCompat.checkSelfPermission(activity, PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setPositiveButton(R.string.allow, (dialog, which) -> launcher.launch(PERMISSION))
                .setNegativeButton(R.string.not_now, null)
                .show();
    }
}
