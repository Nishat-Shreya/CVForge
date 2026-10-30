package com.example.cvforge.ui.discover;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.ItemInternshipBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/** The internship cards in the Discover list. */
public class InternshipAdapter extends RecyclerView.Adapter<InternshipAdapter.ViewHolder> {

    public interface OnInternshipClickListener {
        void onInternshipClick(Internship internship);
    }

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final List<Internship> items = new ArrayList<>();
    private final OnInternshipClickListener listener;

    public InternshipAdapter(OnInternshipClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Internship> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInternshipBinding binding = ItemInternshipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
        // A4: the cards slide up one after the other
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 8));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemInternshipBinding binding;

        ViewHolder(ItemInternshipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Internship post) {
            Context context = itemView.getContext();
            binding.textLogo.setText(initials(post.posterName));
            binding.textTitle.setText(post.title);
            binding.textMeta.setText(context.getString(R.string.post_meta, post.posterName, post.location));

            binding.chipType.setText(typeName(context, post.type));
            if (post.stipend > 0) {
                binding.chipPaid.setText(context.getString(R.string.paid_format, post.stipend));
                binding.chipPaid.setVisibility(View.VISIBLE);
            } else {
                binding.chipPaid.setVisibility(View.GONE);
            }
            showDeadline(context, post);
            binding.chipWarning.setVisibility(post.flagged ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> listener.onInternshipClick(post));
        }

        /** "Closes in 2 days" only appears when the deadline is close. */
        private void showDeadline(Context context, Internship post) {
            long daysLeft = (post.deadline - System.currentTimeMillis()) / DAY_MS;
            if (daysLeft <= 7) {
                binding.chipDeadline.setText(daysLeft <= 0
                        ? context.getString(R.string.closes_today)
                        : context.getResources().getQuantityString(R.plurals.closes_in_days, (int) daysLeft, (int) daysLeft));
                binding.chipDeadline.setVisibility(View.VISIBLE);
            } else {
                binding.chipDeadline.setVisibility(View.GONE);
            }
        }
    }

    static String typeName(Context context, String type) {
        if (Internship.TYPE_REMOTE.equals(type)) {
            return context.getString(R.string.type_remote);
        }
        if (Internship.TYPE_HYBRID.equals(type)) {
            return context.getString(R.string.type_hybrid);
        }
        return context.getString(R.string.type_onsite);
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }
}
