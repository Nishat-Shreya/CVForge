package com.example.cvforge.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Broadcast Receiver (W10) that hears when the internet connection changes.
 *
 * Lifecycle: it is not declared in the manifest. The screen registers it in onStart and
 * unregisters it in onStop, so it only listens while that screen is visible.
 */
public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkReceiver";

    /** The screen that wants to know when the connection changes. */
    public interface Listener {
        void onNetworkChanged(boolean online);
    }

    private final Listener listener;

    public NetworkReceiver(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean online = isOnline(context);
        Log.d(TAG, "onReceive: the phone is " + (online ? "online" : "offline"));
        listener.onNetworkChanged(online);
    }

    /** True when the phone has an internet connection right now. */
    public static boolean isOnline(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo network = manager.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }
}
