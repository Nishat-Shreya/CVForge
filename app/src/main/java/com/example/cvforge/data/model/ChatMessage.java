package com.example.cvforge.data.model;

/**
 * One message in a chat. It is stored in chats/{chatId}/messages.
 * A "system" message is a centered notice such as "Interview set".
 */
public class ChatMessage {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_SYSTEM = "system";

    public String id;
    public String senderId;
    public String text;
    public String type = TYPE_TEXT;
    public long createdAt;

    // Firestore needs an empty constructor
    public ChatMessage() {
    }

    public boolean isSystem() {
        return TYPE_SYSTEM.equals(type);
    }
}
