package com.example.cvforge.ui.student;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.databinding.ItemFocusBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/** Cards for Today's Focus. They slide up one after the other (animation A4). */
public class FocusAdapter extends RecyclerView.Adapter<FocusAdapter.ViewHolder> {

    private final List<FocusItem> items = new ArrayList<>();

    public void setItems(List<FocusItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFocusBinding binding = ItemFocusBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
        AnimUtils.slideUpIn(holder.itemView, position + 3);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFocusBinding binding;

        ViewHolder(ItemFocusBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FocusItem item) {
            Context context = itemView.getContext();
            int tint = ContextCompat.getColor(context, item.tintColor);
            int text = ContextCompat.getColor(context, item.textColor);

            binding.textInitials.setText(item.initials);
            binding.textInitials.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tint));
            binding.textInitials.setTextColor(text);
            binding.textTitle.setText(item.title);
            binding.textSubtitle.setText(item.subtitle);
            binding.textChip.setText(item.chipText);
            binding.textChip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tint));
            binding.textChip.setTextColor(text);
            binding.getRoot().setOnClickListener(v -> item.onClick.run());
        }
    }
}
