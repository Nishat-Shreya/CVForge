package com.example.cvforge.ui.chat;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.ChatMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The messages of a chat. Three view types: my bubble (violet, right), the bubble of the
 * other person (white, left) and a centered system notice.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;
    private static final int TYPE_SYSTEM = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String myUid;

    public MessageAdapter(String myUid) {
        this.myUid = myUid;
    }

    /** Replaces the list. Only new messages are animated by the RecyclerView item animator (A11). */
    public void setMessages(List<ChatMessage> newMessages) {
        int oldSize = messages.size();
        messages.clear();
        messages.addAll(newMessages);
        if (oldSize == 0) {
            notifyDataSetChanged();
        } else if (messages.size() > oldSize) {
            notifyItemRangeInserted(oldSize, messages.size() - oldSize);
        } else {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isSystem()) {
            return TYPE_SYSTEM;
        }
        return myUid.equals(message.senderId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewType == TYPE_SENT) {
            layout = R.layout.item_message_sent;
        } else if (viewType == TYPE_RECEIVED) {
            layout = R.layout.item_message_received;
        } else {
            layout = R.layout.item_message_system;
        }
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.text.setText(message.text);
        if (holder.time != null) {
            holder.time.setText(DateFormat.getTimeFormat(holder.itemView.getContext()).format(new Date(message.createdAt)));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView text;
        /** The system notice has no time. */
        private final TextView time;

        ViewHolder(View view) {
            super(view);
            text = view.findViewById(R.id.textMessage);
            time = view.findViewById(R.id.textTime);
        }
    }
}
