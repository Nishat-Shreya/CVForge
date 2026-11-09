package com.example.cvforge.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.ai.ScamReview;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.ItemFlaggedPostBinding;
import com.example.cvforge.util.ListText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Flagged posts for the admin. The AI opinion is kept per post, so it stays when the list scrolls. */
public class FlaggedPostAdapter extends RecyclerView.Adapter<FlaggedPostAdapter.ViewHolder> {

    public interface Listener {
        void onAskAi(Internship post);

        void onKeep(Internship post);

        void onRemove(Internship post);
    }

    private final List<Internship> posts = new ArrayList<>();
    private final Map<String, ScamReview.Result> opinions = new HashMap<>();
    /** Posts where the AI is still working. */
    private final List<String> waiting = new ArrayList<>();
    private final Listener listener;

    public FlaggedPostAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPosts(List<Internship> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    public void removePost(Internship post) {
        int index = posts.indexOf(post);
        if (index >= 0) {
            posts.remove(index);
            notifyItemRemoved(index);
        }
    }

    public boolean isEmpty() {
        return posts.isEmpty();
    }

    public void setWaiting(Internship post, boolean isWaiting) {
        if (isWaiting) {
            waiting.add(post.id);
        } else {
            waiting.remove(post.id);
        }
        refresh(post);
    }

    public void setOpinion(Internship post, ScamReview.Result result) {
        opinions.put(post.id, result);
        refresh(post);
    }

    private void refresh(Internship post) {
        int index = posts.indexOf(post);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFlaggedPostBinding binding = ItemFlaggedPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Internship post = posts.get(position);
        ItemFlaggedPostBinding b = holder.binding;
        b.textTitle.setText(post.title);
        b.textPoster.setText(b.getRoot().getContext().getString(R.string.scam_poster, post.posterName, post.posterRole));
        b.textReasons.setText(post.flagReasons == null ? "" : "• " + ListText.join(post.flagReasons, "\n• "));

        boolean isWaiting = waiting.contains(post.id);
        b.btnOpinion.setEnabled(!isWaiting);
        b.btnOpinion.setText(isWaiting ? R.string.ai_writing : R.string.scam_ask_ai);

        ScamReview.Result opinion = opinions.get(post.id);
        if (opinion == null) {
            b.textOpinion.setVisibility(View.GONE);
        } else {
            showOpinion(b, opinion);
        }

        b.btnOpinion.setOnClickListener(v -> listener.onAskAi(post));
        b.btnKeep.setOnClickListener(v -> listener.onKeep(post));
        b.btnRemove.setOnClickListener(v -> listener.onRemove(post));
    }

    /** Verdict and reason. SAFE is teal, SUSPICIOUS is amber, SCAM is coral, with the word as text. */
    private void showOpinion(ItemFlaggedPostBinding b, ScamReview.Result opinion) {
        int tint;
        int text;
        if ("SAFE".equals(opinion.verdict)) {
            tint = R.color.teal_tint;
            text = R.color.teal_text;
        } else if ("SCAM".equals(opinion.verdict)) {
            tint = R.color.coral_tint;
            text = R.color.coral_text;
        } else {
            tint = R.color.amber_tint;
            text = R.color.amber_text;
        }
        android.content.Context context = b.getRoot().getContext();
        b.textOpinion.setBackgroundTintList(ContextCompat.getColorStateList(context, tint));
        b.textOpinion.setTextColor(ContextCompat.getColor(context, text));
        b.textOpinion.setText(context.getString(R.string.scam_opinion, opinion.verdict, opinion.reason));
        b.textOpinion.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFlaggedPostBinding binding;

        ViewHolder(ItemFlaggedPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
