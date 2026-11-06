package com.example.cvforge.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.cvforge.R;

/** Creates the notification channel and shows notifications. */
public class NotificationHelper {

    /** Status changes, interview reminders, chat messages. Must match the default channel in the manifest. */
    public static final String CHANNEL_ID = "cvforge_updates";

    private NotificationHelper() {
    }

    /**
     * Android 8 and newer need a channel before a notification can be shown.
     * Calling this more than once is fine.
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Shows a notification. The PendingIntent says what opens when the student taps it.
     * Nothing is shown when the student turned notifications off.
     */
    public static void show(Context context, int id, String title, String text, PendingIntent onTap) {
        createChannel(context);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(onTap);
        manager.notify(id, builder.build());
    }
}
