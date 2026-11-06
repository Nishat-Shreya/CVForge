package com.example.cvforge.util;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.example.cvforge.R;

/**
 * Exact alarms (W10 permissions). From Android 12 the user must allow them in the settings,
 * so interview reminders arrive on time. Without it the reminders still come, a little late.
 */
public class ExactAlarmPermission {

    private static final String PREFS = "reminders";
    private static final String KEY_ASKED = "asked_exact_alarm";

    private ExactAlarmPermission() {
    }

    /** Explains why, and opens the settings screen. It asks only once. */
    public static void requestOnce(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (alarmManager == null || alarmManager.canScheduleExactAlarms() || prefs.getBoolean(KEY_ASKED, false)) {
            return;
        }
        prefs.edit().putBoolean(KEY_ASKED, true).apply();

        new AlertDialog.Builder(activity)
                .setTitle(R.string.exact_alarm_title)
                .setMessage(R.string.exact_alarm_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                })
                .setNegativeButton(R.string.not_now, null)
                .show();
    }
}
