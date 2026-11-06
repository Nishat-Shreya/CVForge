package com.example.cvforge.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.cvforge.util.ReminderScheduler;

/**
 * Broadcast Receiver (W10) for system events. Android deletes all alarms when the phone
 * restarts, and also when the app is updated. This receiver hears about that and sets the
 * reminder alarms again, so no interview or deadline reminder is lost.
 *
 * Lifecycle: it is declared in the manifest, so Android can start it even when the app is
 * closed. It does almost nothing itself: it starts a WorkManager job, because onReceive must
 * finish in a few seconds and loading the applications needs the internet.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            ReminderScheduler.refreshNow(context);
        }
    }
}
