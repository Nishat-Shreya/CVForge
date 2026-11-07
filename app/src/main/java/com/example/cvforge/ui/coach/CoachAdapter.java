package com.example.cvforge.ui.coach;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;

import java.util.ArrayList;
import java.util.List;

/** The practice chat. Two view types: the coach on the left, the student on the right. */
public class CoachAdapter extends RecyclerView.Adapter<CoachAdapter.ViewHolder> {

    /** One bubble. */
    public static class Message {
        public final String text;
        public final boolean fromCoach;

        public Message(String text, boolean fromCoach) {
            this.text = text;
            this.fromCoach = fromCoach;
        }
    }

    private static final int TYPE_COACH = 0;
    private static final int TYPE_STUDENT = 1;

    private final List<Message> messages = new ArrayList<>();

    public void add(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /** Removes the last bubble (the "typing" bubble). */
    public void removeLast() {
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
            notifyItemRemoved(messages.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).fromCoach ? TYPE_COACH : TYPE_STUDENT;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_COACH ? R.layout.item_coach_question : R.layout.item_coach_answer;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.text.setText(messages.get(position).text);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView text;

        ViewHolder(View view) {
            super(view);
            text = view.findViewById(R.id.textBubble);
        }
    }
}
