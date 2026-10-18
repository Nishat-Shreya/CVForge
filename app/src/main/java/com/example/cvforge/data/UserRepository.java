package com.example.cvforge.data;

import com.example.cvforge.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private void saveUser(User user, ResultListener listener) {
        db.collection(USERS).document(user.getUid()).set(user)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
}
