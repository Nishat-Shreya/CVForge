package com.example.cvforge.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.cvforge.data.model.Application;
import com.example.cvforge.receiver.ReminderReceiver;
import com.example.cvforge.service.DigestWorker;
import com.example.cvforge.service.ReminderWorker;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Sets the alarms for the reminders. At the time of an alarm, Android calls ReminderReceiver.
 * WorkManager keeps the alarms up to date in the background.
 */
public class ReminderScheduler {

    private static final String PREFS = "reminders";
    private static final String KEY_CODES = "scheduled_codes";
    private static final String WORK_PERIODIC = "reminder_refresh";
    private static final String WORK_NOW = "reminder_refresh_now";
    private static final String WORK_DIGEST = "morning_digest";

    private ReminderScheduler() {
    }

    /**
     * Makes alarms for all reminders in the future and cancels the alarms that are not needed
     * any more (for example when an interview moved or an application was rejected).
     */
    public static void schedule(Context context, List<Application> applications) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> oldCodes = prefs.getStringSet(KEY_CODES, new HashSet<>());

        Set<String> newCodes = new HashSet<>();
        for (ReminderPlanner.Reminder reminder : ReminderPlanner.plan(applications, System.currentTimeMillis())) {
            setAlarm(context, alarmManager, reminder);
            newCodes.add(String.valueOf(reminder.code));
        }

        for (String oldCode : oldCodes) {
            if (!newCodes.contains(oldCode)) {
                alarmManager.cancel(pendingIntent(context, Integer.parseInt(oldCode), null));
            }
        }
        prefs.edit().putStringSet(KEY_CODES, newCodes).apply();
    }

    private static void setAlarm(Context context, AlarmManager alarmManager, ReminderPlanner.Reminder reminder) {
        Intent extras = new Intent();
        extras.putExtra(ReminderReceiver.EXTRA_TYPE, reminder.type);
        extras.putExtra(ReminderReceiver.EXTRA_COMPANY, reminder.companyName);
        extras.putExtra(ReminderReceiver.EXTRA_TITLE, reminder.jobTitle);
        PendingIntent pending = pendingIntent(context, reminder.code, extras);

        if (canUseExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending);
        } else {
            // without the exact alarm permission Android may delay the alarm a little
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending);
        }
    }

    /** From Android 12 the user has to allow exact alarms. */
    public static boolean canUseExactAlarms(AlarmManager alarmManager) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms();
    }

    /** The same request code always gives the same PendingIntent, so the alarm can be replaced or cancelled. */
    private static PendingIntent pendingIntent(Context context, int code, Intent extras) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        if (extras != null) {
            intent.putExtras(extras);
        }
        intent.putExtra(ReminderReceiver.EXTRA_CODE, code);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, code, intent, flags);
    }

    // ---------- WorkManager ----------

    /** Keeps the alarms fresh: a check every 6 hours, and one check right now. */
    public static void enqueueRefresh(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(ReminderWorker.class, 6, TimeUnit.HOURS).build());
        refreshNow(context);
    }

    /** The morning digest: once a day at about 8:00. The first run waits until the next 8:00. */
    public static void enqueueDigest(Context context) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 8);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        long delay = next.getTimeInMillis() - System.currentTimeMillis();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DigestWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_DIGEST,
                ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** Runs the worker once now. BootReceiver uses it after a restart. */
    public static void refreshNow(Context context) {
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(ReminderWorker.class).build());
    }
}
