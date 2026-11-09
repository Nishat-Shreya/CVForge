package com.example.cvforge.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.model.Chat;
import com.example.cvforge.databinding.ItemChatBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/** The rows of the chat list: the other person, what the chat is about, the last message, unread count. */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface Listener {
        void onChatClicked(Chat chat);
    }

    private final List<Chat> chats = new ArrayList<>();
    private final String myUid;
    private final Listener listener;

    public ChatListAdapter(String myUid, Listener listener) {
        this.myUid = myUid;
        this.listener = listener;
    }

    public void setChats(List<Chat> newChats) {
        chats.clear();
        chats.addAll(newChats);
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return chats.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding binding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chats.get(position);
        ItemChatBinding b = holder.binding;
        String name = chat.otherName(myUid);

        b.textInitials.setText(initials(name));
        b.textName.setText(name);
        b.textContext.setText(chat.contextTitle);
        b.textContext.setVisibility(chat.contextTitle == null || chat.contextTitle.isEmpty() ? View.GONE : View.VISIBLE);
        b.textLast.setText(chat.lastMessage == null ? "" : chat.lastMessage);

        int unread = chat.unreadFor(myUid);
        b.textUnread.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        b.textUnread.setText(String.valueOf(unread));

        b.getRoot().setOnClickListener(v -> listener.onChatClicked(chat));
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 6));
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemChatBinding binding;

        ViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
