package com.example.cvforge.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One chat between two people (document in the "chats" collection).
 * The maps are keyed by the uid of each person.
 */
public class Chat {

    public String id;
    public List<String> participants = new ArrayList<>();
    public Map<String, String> names = new HashMap<>();
    public Map<String, String> roles = new HashMap<>();
    /** What the chat is about, for example "Android Intern". */
    public String contextTitle;
    public String lastMessage;
    public long lastAt;
    /** How many messages each person has not read yet. */
    public Map<String, Long> unread = new HashMap<>();

    // Firestore needs an empty constructor
    public Chat() {
    }

    /** The uid of the other person. */
    public String otherId(String myUid) {
        for (String uid : participants) {
            if (!uid.equals(myUid)) {
                return uid;
            }
        }
        return "";
    }

    public String otherName(String myUid) {
        String name = names == null ? null : names.get(otherId(myUid));
        return name == null ? "" : name;
    }

    public String otherRole(String myUid) {
        String role = roles == null ? null : roles.get(otherId(myUid));
        return role == null ? "" : role;
    }

    public int unreadFor(String myUid) {
        Long count = unread == null ? null : unread.get(myUid);
        return count == null ? 0 : count.intValue();
    }
}
