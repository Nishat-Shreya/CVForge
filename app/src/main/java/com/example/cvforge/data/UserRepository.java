package com.example.cvforge.data;

import com.example.cvforge.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/** Accounts: Firebase Authentication plus the "users" document in Firestore. */
public class UserRepository {

    private static final String USERS = "users";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Creates the login, then saves the user document with the chosen role. */
    public void signUp(String name, String email, String password, String role, ResultListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        listener.onError("Could not create the account.");
                        return;
                    }
                    boolean verified = !User.needsApproval(role);
                    User user = new User(firebaseUser.getUid(), name, email, role, verified);
                    saveUser(user, listener);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void login(String email, String password, ResultListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Firebase uid of the signed-in account, or null when nobody is signed in. */
    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void logout() {
        auth.signOut();
    }

    /** Loads the users document of the signed-in account. */
    public void loadCurrentUser(UserListener listener) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onError("You are not logged in.");
            return;
        }
        db.collection(USERS).document(firebaseUser.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user == null) {
                        listener.onError("Profile not found for this account.");
                    } else {
                        listener.onUser(user);
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Like loadCurrentUser, but a missing profile is not an error: the listener gets null.
     * Used after Google sign in, where the account can be new and has no profile yet.
     */
    public void findCurrentUser(UserListener listener) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onError("You are not logged in.");
            return;
        }
        db.collection(USERS).document(firebaseUser.getUid()).get()
                .addOnSuccessListener(snapshot -> listener.onUser(snapshot.toObject(User.class)))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Saves the users document for an account that already exists in Firebase Authentication
     * (Google sign in). The name and email come from the Google account.
     */
    public void createProfileForCurrentAccount(String role, ResultListener listener) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onError("You are not logged in.");
            return;
        }
        String email = firebaseUser.getEmail() == null ? "" : firebaseUser.getEmail();
        String name = firebaseUser.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "CVForge user";
        }
        saveUser(new User(firebaseUser.getUid(), name, email, role, !User.needsApproval(role)), listener);
    }

    /** Company and Alumni accounts that an admin has not approved yet. */
    public void loadPendingUsers(UserListListener listener) {
        db.collection(USERS).whereEqualTo("verified", false).get()
                .addOnSuccessListener(snapshot -> {
                    List<User> pending = new ArrayList<>();
                    for (User user : snapshot.toObjects(User.class)) {
                        if (User.needsApproval(user.getRole())) {
                            pending.add(user);
                        }
                    }
                    listener.onUsers(pending);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Admin approves an account: sets verified = true. */
    public void approveUser(String uid, ResultListener listener) {
        db.collection(USERS).document(uid).update("verified", true)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    private void saveUser(User user, ResultListener listener) {
        db.collection(USERS).document(user.getUid()).set(user)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
}
