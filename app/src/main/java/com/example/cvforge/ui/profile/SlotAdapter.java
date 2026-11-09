package com.example.cvforge.ui.profile;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.MentorSlot;
import com.example.cvforge.databinding.ItemSlotBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Mentor slot cards. The same card is used by the student (button "Book") and by the
 * alumni (button "Delete" while nobody booked the slot).
 */
public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder> {

    public interface Listener {
        void onAction(MentorSlot slot);
    }

    private final List<MentorSlot> slots = new ArrayList<>();
    private final boolean forStudent;
    private final String myUid;
    private final Listener listener;

    public SlotAdapter(boolean forStudent, String myUid, Listener listener) {
        this.forStudent = forStudent;
        this.myUid = myUid;
        this.listener = listener;
    }

    public void setSlots(List<MentorSlot> newSlots) {
        slots.clear();
        slots.addAll(newSlots);
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSlotBinding binding = ItemSlotBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        MentorSlot slot = slots.get(position);
        ItemSlotBinding b = holder.binding;

        String person = forStudent ? slot.mentorName : slot.bookedByName;
        b.avatar.textInitials.setText(initials(forStudent ? slot.mentorName : person));
        b.textTitle.setText(forStudent
                ? context.getString(R.string.slot_title, slot.mentorName)
                : context.getString(slot.isOpen() ? R.string.slot_title_mine : R.string.slot_title_booked_by, person));
        b.textTime.setText(DateFormat.format("EEE d MMM, HH:mm", new Date(slot.startAt))
                + " · " + context.getString(R.string.slot_minutes, MentorSlot.LENGTH_MINUTES));

        showStatusAndButton(context, b, slot);
        b.btnAction.setOnClickListener(v -> listener.onAction(slot));
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 6));
    }

    /** Status has text and color. The button is only there when the action is possible. */
    private void showStatusAndButton(Context context, ItemSlotBinding b, MentorSlot slot) {
        boolean mine = myUid != null && myUid.equals(slot.bookedById);
        if (slot.isOpen()) {
            b.textStatus.setText(R.string.slot_open);
            b.textStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.teal_tint));
            b.textStatus.setTextColor(ContextCompat.getColor(context, R.color.teal_text));
            b.btnAction.setText(forStudent ? R.string.slot_book : R.string.slot_delete);
            b.btnAction.setEnabled(true);
            b.btnAction.setVisibility(android.view.View.VISIBLE);
        } else {
            b.textStatus.setText(forStudent && mine ? R.string.slot_booked_by_you : R.string.slot_booked);
            b.textStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.amber_tint));
            b.textStatus.setTextColor(ContextCompat.getColor(context, R.color.amber_text));
            // the alumni and the student who booked can open the chat of this slot
            boolean chat = !forStudent || mine;
            b.btnAction.setText(R.string.slot_open_chat);
            b.btnAction.setEnabled(true);
            b.btnAction.setVisibility(chat ? android.view.View.VISIBLE : android.view.View.GONE);
        }
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
        return slots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSlotBinding binding;

        ViewHolder(ItemSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
