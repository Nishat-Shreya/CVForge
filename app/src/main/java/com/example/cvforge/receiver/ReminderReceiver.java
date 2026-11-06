package com.example.cvforge.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.cvforge.R;
import com.example.cvforge.ui.auth.WelcomeActivity;
import com.example.cvforge.util.NotificationHelper;
import com.example.cvforge.util.ReminderPlanner;

/**
 * Broadcast Receiver (W10). Android calls it at the time of an alarm that ReminderScheduler
 * set. It shows the reminder as a notification.
 *
 * Lifecycle: it is declared in the manifest, so it also works when the app is closed.
 * onReceive must finish quickly, so the notification is built here and nothing else.
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_COMPANY = "company";
    public static final String EXTRA_TITLE = "title";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(EXTRA_TYPE);
        String company = intent.getStringExtra(EXTRA_COMPANY);
        String job = intent.getStringExtra(EXTRA_TITLE);
        int code = intent.getIntExtra(EXTRA_CODE, 0);
        Log.d(TAG, "onReceive: " + type);

        String title;
        String text;
        if (ReminderPlanner.TYPE_INTERVIEW.equals(type)) {
            title = context.getString(R.string.reminder_interview_title);
            text = context.getString(R.string.reminder_interview_text, job, company);
        } else {
            title = context.getString(R.string.reminder_deadline_title);
            text = context.getString(R.string.reminder_deadline_text, job, company);
        }
        NotificationHelper.show(context, code, title, text, openAppIntent(context));
    }

    private PendingIntent openAppIntent(Context context) {
        Intent open = new Intent(context, WelcomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 1, open, flags);
    }
}
