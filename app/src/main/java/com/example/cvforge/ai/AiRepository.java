package com.example.cvforge.ai;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.QuotaExceededException;
import com.google.firebase.ai.type.RequestTimeoutException;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * The only place that talks to Gemini. The app uses Firebase AI Logic, so no Gemini key is
 * stored in the app: the request goes to Firebase, and Firebase calls Gemini with the key
 * it keeps on the server. Every answer is only a suggestion that the student can edit.
 *
 * Needs "Firebase AI Logic" to be turned on for the project in the Firebase console.
 */
public class AiRepository {

    /** Callback with the answer text. Always runs on the main thread. */
    public interface Callback {
        void onResult(String text);

        /**
         * The AI could not answer. The message is short and ready to show to the student:
         * no internet, limit reached, or "AI service is not available right now".
         * The full error is in Logcat (tag AiRepository).
         */
        void onError(String message);
    }

    private static final String TAG = "AiRepository";

    /**
     * The Gemini model of every AI feature. It is written once, so it is easy to change.
     * Gemini 2.5 models are being shut down by Google (October 2026), so a current stable
     * Flash model is used. Check the list in the Firebase AI Logic docs ("models") when it fails.
     */
    public static final String MODEL_NAME = "gemini-3.5-flash";

    private final Context context;
    private final Executor mainExecutor;
    private GenerativeModelFutures model;

    public AiRepository(Context context) {
        this.context = context.getApplicationContext();
        mainExecutor = ContextCompat.getMainExecutor(context);
        try {
            // the Gemini Developer API through Firebase AI Logic (free tier)
            GenerativeModel generativeModel = FirebaseAI.Companion
                    .getInstance(GenerativeBackend.googleAI())
                    .generativeModel(MODEL_NAME);
            model = GenerativeModelFutures.Companion.from(generativeModel);
        } catch (RuntimeException e) {
            // generate() reports this to the screen, so the app does not crash
            Log.e(TAG, "Could not create the Gemini model " + MODEL_NAME, e);
        }
    }

    /** Sends a prompt to Gemini. The answer arrives later through the callback. */
    public void generate(String prompt, Callback callback) {
        if (model == null) {
            callback.onError(context.getString(R.string.ai_error_service));
            return;
        }
        ListenableFuture<GenerateContentResponse> future;
        try {
            Content content = new Content.Builder().addText(prompt).build();
            future = model.generateContent(content);
        } catch (RuntimeException e) {
            fail(e, callback);
            return;
        }

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(@Nullable GenerateContentResponse response) {
                String text = null;
                try {
                    text = response == null ? null : response.getText();
                } catch (RuntimeException e) {
                    // for example a blocked answer
                    Log.e(TAG, "Gemini answer could not be read, model=" + MODEL_NAME, e);
                }
                if (text == null || text.trim().isEmpty()) {
                    Log.e(TAG, "Gemini gave an empty answer, model=" + MODEL_NAME);
                    callback.onError(context.getString(R.string.ai_error_service));
                } else {
                    callback.onResult(text.trim());
                }
            }

            @Override
            public void onFailure(@NonNull Throwable error) {
                fail(error, callback);
            }
        }, mainExecutor);
    }

    /** Writes the whole error to Logcat, then gives the screen a short message. */
    private void fail(Throwable error, Callback callback) {
        Kind kind = classify(error);
        // the stack trace and the cause chain are printed, the HTTP status is in the messages
        Log.e(TAG, "Gemini call failed: model=" + MODEL_NAME + ", kind=" + kind
                + ", error=" + error.getClass().getName() + ", messages=" + allMessages(error), error);
        callback.onError(context.getString(kind.messageRes));
    }

    /** What kind of problem it is, so the student gets a message that helps. */
    enum Kind {
        NETWORK(R.string.ai_error_network),
        QUOTA(R.string.ai_error_quota),
        SERVICE(R.string.ai_error_service);

        final int messageRes;

        Kind(int messageRes) {
            this.messageRes = messageRes;
        }
    }

    /**
     * No internet (the request never arrived), limit reached (HTTP 429), or everything else:
     * a model that does not exist (404), no permission (403), AI Logic not turned on, a bad key.
     */
    static Kind classify(Throwable error) {
        for (Throwable e = error; e != null; e = e.getCause() == e ? null : e.getCause()) {
            if (e instanceof IOException || e instanceof RequestTimeoutException) {
                return Kind.NETWORK;
            }
        }
        for (Throwable e = error; e != null; e = e.getCause() == e ? null : e.getCause()) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (e instanceof QuotaExceededException || message.contains("quota")
                    || message.contains("429") || message.contains("resource_exhausted")
                    || message.contains("rate limit")) {
                return Kind.QUOTA;
            }
        }
        return Kind.SERVICE;
    }

    private static String allMessages(Throwable error) {
        StringBuilder text = new StringBuilder();
        for (Throwable e = error; e != null; e = e.getCause() == e ? null : e.getCause()) {
            text.append('[').append(e.getMessage()).append("] ");
        }
        return text.toString().trim();
    }
}
