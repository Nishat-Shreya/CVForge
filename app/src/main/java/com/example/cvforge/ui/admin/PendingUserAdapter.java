package com.example.cvforge.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ItemPendingUserBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/** Shows the accounts waiting for admin approval. */
public class PendingUserAdapter extends RecyclerView.Adapter<PendingUserAdapter.ViewHolder> {

    public interface OnApproveClickListener {
        void onApproveClick(User user);
    }

    private final List<User> users = new ArrayList<>();
    private final OnApproveClickListener listener;

    public PendingUserAdapter(OnApproveClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> newUsers) {
        users.clear();
        users.addAll(newUsers);
        notifyDataSetChanged();
    }

    public void removeUser(User user) {
        int index = users.indexOf(user);
        if (index >= 0) {
            users.remove(index);
            notifyItemRemoved(index);
        }
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingUserBinding binding = ItemPendingUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position));
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 8));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPendingUserBinding binding;

        ViewHolder(ItemPendingUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.textName.setText(user.getName());
            binding.textEmail.setText(user.getEmail());
            binding.textRole.setText(itemView.getContext().getString(R.string.role_pending, user.getRole()));
            binding.avatar.textInitials.setText(initials(user.getName()));
            binding.btnApprove.setOnClickListener(v -> listener.onApproveClick(user));
        }
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }
}
