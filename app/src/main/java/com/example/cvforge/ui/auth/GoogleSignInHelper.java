package com.example.cvforge.ui.auth;

import android.app.Activity;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.example.cvforge.R;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.ui.RoleRouter;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Google sign in with Credential Manager, then Firebase Authentication.
 * Steps: the phone shows the Google account picker, Google gives an ID token, Firebase signs
 * in with it. An account that signs in for the first time has no users document yet, so it
 * gets one with the chosen role (Company and Alumni then wait for the admin, like email sign up).
 * The Google web client id is made by the google-services plugin from google-services.json,
 * so no key is written in the code.
 */
public class GoogleSignInHelper {

    /** Tells the screen what is happening, so it can show a loading state or an error. */
    public interface Callback {
        void onStarted();

        /** Nothing was signed in: the user closed the picker, or something went wrong. */
        void onStopped(@Nullable String errorMessage);
    }

    private final Activity activity;
    private final UserRepository userRepository = new UserRepository();

    public GoogleSignInHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * @param roleForNewAccount role chosen on the Welcome screen, used only if the Google account
     *                          is new. When it is null, a new account is asked for its role.
     */
    public void start(@Nullable String roleForNewAccount, Callback callback) {
        callback.onStarted();

        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        CredentialManager.create(activity).getCredentialAsync(activity, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleCredential(response.getCredential(), roleForNewAccount, callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException error) {
                        if (error instanceof GetCredentialCancellationException) {
                            callback.onStopped(null);
                        } else if (error instanceof NoCredentialException) {
                            callback.onStopped(activity.getString(R.string.google_no_account));
                        } else {
                            callback.onStopped(activity.getString(R.string.google_failed));
                        }
                    }
                });
    }

    private void handleCredential(Credential credential, @Nullable String role, Callback callback) {
        boolean isGoogleToken = credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType());
        if (!isGoogleToken) {
            callback.onStopped(activity.getString(R.string.google_failed));
            return;
        }
        GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(google.getIdToken(), null);

        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                .addOnSuccessListener(result -> checkProfile(role, callback))
                .addOnFailureListener(e -> callback.onStopped(e.getMessage()));
    }

    /** Existing account: go to its home screen. New account: it needs a role first. */
    private void checkProfile(@Nullable String role, Callback callback) {
        userRepository.findCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                if (user != null) {
                    RoleRouter.open(activity, user);
                } else if (role != null) {
                    createProfile(role, callback);
                } else {
                    askForRole(callback);
                }
            }

            @Override
            public void onError(String message) {
                signOutAndStop(callback, message);
            }
        });
    }

    private void askForRole(Callback callback) {
        String[] roles = {User.ROLE_STUDENT, User.ROLE_COMPANY, User.ROLE_TEACHER, User.ROLE_ALUMNI};
        String[] names = {
                activity.getString(R.string.role_student), activity.getString(R.string.role_company),
                activity.getString(R.string.role_teacher), activity.getString(R.string.role_alumni)};
        new AlertDialog.Builder(activity)
                .setTitle(R.string.google_choose_role)
                .setMessage(R.string.google_role_note)
                .setItems(names, (dialog, which) -> createProfile(roles[which], callback))
                // no role, no profile: the Google login is removed again, so the next try starts clean
                .setOnCancelListener(dialog -> signOutAndStop(callback, null))
                .show();
    }

    private void createProfile(String role, Callback callback) {
        userRepository.createProfileForCurrentAccount(role, new ResultListener() {
            @Override
            public void onSuccess() {
                userRepository.loadCurrentUser(new UserListener() {
                    @Override
                    public void onUser(User user) {
                        RoleRouter.open(activity, user);
                    }

                    @Override
                    public void onError(String message) {
                        signOutAndStop(callback, message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                signOutAndStop(callback, message);
            }
        });
    }

    private void signOutAndStop(Callback callback, @Nullable String message) {
        userRepository.logout();
        callback.onStopped(message);
    }
}
