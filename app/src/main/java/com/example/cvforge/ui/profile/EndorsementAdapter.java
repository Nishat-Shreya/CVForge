package com.example.cvforge.ui.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Endorsement;
import com.example.cvforge.databinding.ItemEndorsementBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/** Endorsement cards. Approved ones have the teal Verified chip, the others show their status. */
public class EndorsementAdapter extends RecyclerView.Adapter<EndorsementAdapter.ViewHolder> {

    private static final int[] BORDER_COLORS = {
            R.color.violet, R.color.teal, R.color.coral, R.color.amber};

    private final List<Endorsement> items = new ArrayList<>();

    public void setItems(List<Endorsement> newItems) {
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
        ItemEndorsementBinding binding = ItemEndorsementBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        Endorsement item = items.get(position);
        ItemEndorsementBinding b = holder.binding;

        b.textSkill.setText(item.skill);
        b.avatar.textInitials.setText(initials(item.endorserName));
        b.viewBorder.setBackgroundColor(ContextCompat.getColor(context, BORDER_COLORS[position % BORDER_COLORS.length]));
        b.textEndorser.setText(context.getString(R.string.endorsed_by, item.endorserName, roleName(context, item.endorserRole)));

        boolean hasComment = item.comment != null && !item.comment.isEmpty();
        b.textComment.setVisibility(hasComment ? View.VISIBLE : View.GONE);
        b.textComment.setText(hasComment ? "“" + item.comment + "”" : "");

        showStatus(context, b, item);
        // A4: the cards slide up one after the other
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 6));
    }

    /** The status always has text, not only a color. */
    private void showStatus(Context context, ItemEndorsementBinding b, Endorsement item) {
        int textRes;
        int tint;
        int text;
        if (item.isApproved()) {
            textRes = R.string.endorsement_verified;
            tint = R.color.teal_tint;
            text = R.color.teal_text;
        } else if (Endorsement.STATUS_DECLINED.equals(item.status)) {
            textRes = R.string.endorsement_declined;
            tint = R.color.coral_tint;
            text = R.color.coral_text;
        } else {
            textRes = R.string.endorsement_pending;
            tint = R.color.amber_tint;
            text = R.color.amber_text;
        }
        b.chipStatus.setText(textRes);
        b.chipStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, tint));
        b.chipStatus.setTextColor(ContextCompat.getColor(context, text));
    }

    private String roleName(Context context, String role) {
        return context.getString("alumni".equals(role) ? R.string.role_alumni : R.string.role_teacher);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemEndorsementBinding binding;

        ViewHolder(ItemEndorsementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
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
