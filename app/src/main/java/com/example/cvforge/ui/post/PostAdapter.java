package com.example.cvforge.ui.post;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.ItemPostBinding;
import com.example.cvforge.util.AnimUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** The poster's own internships, each with Edit, Close or Reopen, and Delete. */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(Internship post);

        void onToggleStatus(Internship post);

        void onDelete(Internship post);
    }

    private final List<Internship> posts = new ArrayList<>();
    private final Listener listener;

    public PostAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPosts(List<Internship> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    public void remove(Internship post) {
        int index = posts.indexOf(post);
        if (index >= 0) {
            posts.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void refresh(Internship post) {
        int index = posts.indexOf(post);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    public boolean isEmpty() {
        return posts.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(posts.get(position));
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 8));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPostBinding binding;

        ViewHolder(ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Internship post) {
            Context context = itemView.getContext();
            binding.textTitle.setText(post.title);
            binding.textMeta.setText(context.getString(R.string.post_meta, typeName(context, post.type), post.location));
            binding.textDeadline.setText(context.getString(R.string.deadline_format,
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(post.deadline))));

            // the status always has a text label as well as a color
            boolean open = post.isOpen();
            binding.chipStatus.setText(open ? R.string.status_open : R.string.status_closed);
            binding.chipStatus.setBackgroundTintList(ContextCompat.getColorStateList(context,
                    open ? R.color.teal_tint : R.color.coral_tint));
            binding.chipStatus.setTextColor(ContextCompat.getColor(context,
                    open ? R.color.teal_text : R.color.coral_text));

            binding.btnToggle.setText(open ? R.string.close_post : R.string.reopen_post);
            binding.btnEdit.setOnClickListener(v -> listener.onEdit(post));
            binding.btnToggle.setOnClickListener(v -> listener.onToggleStatus(post));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(post));
        }
    }

    private static String typeName(Context context, String type) {
        if (Internship.TYPE_REMOTE.equals(type)) {
            return context.getString(R.string.type_remote);
        }
        if (Internship.TYPE_HYBRID.equals(type)) {
            return context.getString(R.string.type_hybrid);
        }
        return context.getString(R.string.type_onsite);
    }
}
