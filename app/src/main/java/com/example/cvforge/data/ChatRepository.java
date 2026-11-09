package com.example.cvforge.data;

import com.example.cvforge.data.model.Chat;
import com.example.cvforge.data.model.ChatMessage;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chats live in Firestore: a document in "chats" (who talks, last message, unread counts, who is
 * typing) and the messages in its "messages" sub collection. Two people share one chat id, so a
 * chat is always the same one for the same application or mentor slot.
 */
public class ChatRepository {

    /** Callback with all messages, oldest first. Runs on the main thread, again after every change. */
    public interface MessagesListener {
        void onMessages(List<ChatMessage> messages);

        void onError(String message);
    }

    /** Callback when the other person starts or stops typing. */
    public interface TypingListener {
        void onTyping(boolean otherIsTyping);
    }

    /** Callback with all chats of the user, newest first. Runs again after every change. */
    public interface ChatsListener {
        void onChats(List<Chat> chats);

        void onError(String message);
    }

    /** One of the two people of a chat. */
    public static class Person {
        public final String uid;
        public final String name;
        public final String role;

        public Person(String uid, String name, String role) {
            this.uid = uid;
            this.name = name == null ? "" : name;
            this.role = role == null ? "" : role;
        }
    }

    private static final String CHATS = "chats";
    private static final String MESSAGES = "messages";

    /** Typing counts for this long, then the dots go away. */
    public static final long TYPING_MS = 5000;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Chat id for the talk about one application. */
    public static String applicationChatId(String applicationId) {
        return "app_" + applicationId;
    }

    /** Chat id for the talk about one booked mentor slot. */
    public static String slotChatId(String slotId) {
        return "slot_" + slotId;
    }

    /**
     * Makes sure the chat document exists. The two people are saved in "participants", and the
     * security rules only let these two read the chat.
     */
    public void openChat(String chatId, Person me, Person other, String contextTitle, ResultListener listener) {
        Map<String, Object> names = new HashMap<>();
        names.put(me.uid, me.name);
        names.put(other.uid, other.name);
        Map<String, Object> roles = new HashMap<>();
        roles.put(me.uid, me.role);
        roles.put(other.uid, other.role);

        Map<String, Object> data = new HashMap<>();
        // the same order for both people, so the list never changes when the other one opens the chat
        List<String> participants = new ArrayList<>(Arrays.asList(me.uid, other.uid));
        Collections.sort(participants);
        data.put("participants", participants);
        data.put("names", names);
        data.put("roles", roles);
        data.put("contextTitle", contextTitle);
        db.collection(CHATS).document(chatId).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Live list of messages. Call remove() on the result when the screen stops. */
    public ListenerRegistration listenMessages(String chatId, MessagesListener listener) {
        return db.collection(CHATS).document(chatId).collection(MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ChatMessage message = doc.toObject(ChatMessage.class);
                        if (message != null) {
                            message.id = doc.getId();
                            list.add(message);
                        }
                    }
                    listener.onMessages(list);
                });
    }

    /** Sends a message. The unread count of the other person goes up by one. */
    public void sendMessage(String chatId, String senderId, String otherId, String text, String type,
                            ResultListener listener) {
        DocumentReference chat = db.collection(CHATS).document(chatId);
        ChatMessage message = new ChatMessage();
        message.senderId = senderId;
        message.text = text;
        message.type = type;
        message.createdAt = System.currentTimeMillis();

        chat.collection(MESSAGES).add(message)
                .addOnSuccessListener(ref -> {
                    chat.update("lastMessage", text, "lastAt", message.createdAt, "typingUid", null,
                            "unread." + otherId, FieldValue.increment(1));
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Opens the chat if needed and puts a notice in it, for example "Interview set: ...". */
    public void announce(String chatId, Person me, Person other, String contextTitle, String text) {
        openChat(chatId, me, other, contextTitle, new ResultListener() {
            @Override
            public void onSuccess() {
                sendMessage(chatId, me.uid, other.uid, text, ChatMessage.TYPE_SYSTEM, new ResultListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(String message) {
                    }
                });
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    /** The user has read the chat: their unread count goes back to 0. */
    public void markRead(String chatId, String myUid) {
        db.collection(CHATS).document(chatId).update("unread." + myUid, 0);
    }

    /** Tells the other person that this user is typing. */
    public void setTyping(String chatId, String myUid) {
        db.collection(CHATS).document(chatId)
                .update("typingUid", myUid, "typingAt", System.currentTimeMillis());
    }

    /** Live: is the other person typing right now? Call remove() on the result when the screen stops. */
    public ListenerRegistration listenTyping(String chatId, String myUid, TypingListener listener) {
        return db.collection(CHATS).document(chatId).addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                return;
            }
            String typingUid = snapshot.getString("typingUid");
            Long typingAt = snapshot.getLong("typingAt");
            boolean typing = typingUid != null && !typingUid.equals(myUid) && typingAt != null
                    && System.currentTimeMillis() - typingAt < TYPING_MS;
            listener.onTyping(typing);
        });
    }

    /** Live list of the chats of one user, newest first (also used for the unread badge). */
    public ListenerRegistration listenMyChats(String myUid, ChatsListener listener) {
        return db.collection(CHATS).whereArrayContains("participants", myUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    List<Chat> chats = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.id = doc.getId();
                            chats.add(chat);
                        }
                    }
                    Collections.sort(chats, (a, b) -> Long.compare(b.lastAt, a.lastAt));
                    listener.onChats(chats);
                });
    }

    /** The total of unread messages in a list of chats. */
    public static int totalUnread(List<Chat> chats, String myUid) {
        int total = 0;
        for (Chat chat : chats) {
            total += chat.unreadFor(myUid);
        }
        return total;
    }
}
