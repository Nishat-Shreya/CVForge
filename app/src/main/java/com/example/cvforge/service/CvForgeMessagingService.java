package com.example.cvforge.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cvforge.R;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.ui.auth.WelcomeActivity;
import com.example.cvforge.util.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging service (W11 advanced API, W10 Service). Firebase starts it
 * when a push message arrives while the app is open, and when Firebase makes a new token.
 */
public class CvForgeMessagingService extends FirebaseMessagingService {

    private static final String TAG = "CvForgeMessaging";

    /** Extra of the Intent that a notification opens. Chat and details use it in later checkpoints. */
    public static final String EXTRA_TARGET = "target";

    /** A push message arrived while the app is open. It is shown as a notification. */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = data.get("title");
        String text = data.get("body");

        // a message sent from the Firebase console has a notification part instead of data
        if (message.getNotification() != null) {
            if (title == null) {
                title = message.getNotification().getTitle();
            }
            if (text == null) {
                text = message.getNotification().getBody();
            }
        }
        if (title == null) {
            title = getString(R.string.app_name);
        }
        if (text == null) {
            text = "";
        }
        Log.d(TAG, "push received: " + title);

        NotificationHelper.show(this, (int) System.currentTimeMillis(), title, text,
                openAppIntent(data.get(EXTRA_TARGET)));
    }

    /** Firebase made a new token for this phone. It is saved, so the server can send pushes to it. */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "new FCM token");
        new UserRepository().saveFcmToken(token);
    }

    /**
     * What opens when the student taps the notification. The app starts at Welcome, which sends
     * a signed-in student to the right screen. The target tells later checkpoints where to go.
     */
    private PendingIntent openAppIntent(String target) {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (target != null) {
            intent.putExtra(EXTRA_TARGET, target);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, 0, intent, flags);
    }
}
