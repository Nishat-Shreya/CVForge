package com.example.cvforge.ui.tracker;

import android.content.ClipData;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.databinding.ItemKanbanCardBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** The application cards inside one column of the Kanban board. */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    public interface OnCardClickListener {
        void onCardClick(Application application);
    }

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final List<Application> cards = new ArrayList<>();
    private final KanbanColumn column;
    private final OnCardClickListener listener;

    public CardAdapter(KanbanColumn column, OnCardClickListener listener) {
        this.column = column;
        this.listener = listener;
    }

    public void setCards(List<Application> newCards) {
        cards.clear();
        cards.addAll(newCards);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemKanbanCardBinding binding = ItemKanbanCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(cards.get(position));
        // A4: the cards slide up one after the other
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 6));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemKanbanCardBinding binding;

        ViewHolder(ItemKanbanCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Application application) {
            Context context = itemView.getContext();
            binding.viewTopBorder.setBackgroundColor(ContextCompat.getColor(context, column.color));
            binding.textTitle.setText(application.internshipTitle);
            binding.textCompany.setText(application.companyName);
            showChips(context, application);
            binding.getRoot().setOnClickListener(v -> listener.onCardClick(application));
            setUpDrag(application);
        }

        /**
         * A8: a long press lifts the card. It follows the finger tilted by 3 degrees with a violet
         * border, and the original stays faded until the card is dropped in a column.
         */
        private void setUpDrag(Application application) {
            View card = binding.getRoot();
            card.setAlpha(1f);
            card.setOnLongClickListener(v -> {
                v.setAlpha(0.35f);
                ClipData clip = ClipData.newPlainText("application", application.id);
                ViewCompat.startDragAndDrop(v, clip, new CardDragShadow(v), application, 0);
                return true;
            });
            card.setOnDragListener((v, event) -> {
                if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                    return true;
                }
                if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    v.setAlpha(1f);
                }
                // false lets the drop reach the column behind the card
                return false;
            });
        }

        /** Status always has a text label as well as a color. */
        private void showChips(Context context, Application application) {
            binding.rowChips.removeAllViews();
            long now = System.currentTimeMillis();

            if (Application.STATUS_REJECTED.equals(application.status)) {
                addChip(context, context.getString(R.string.chip_rejected), R.color.coral_tint, R.color.coral_text);
            } else if (Application.STATUS_SHORTLISTED.equals(application.status)) {
                addChip(context, context.getString(R.string.chip_shortlisted), R.color.teal_tint, R.color.teal_text);
            }

            if (application.isGhosted(now)) {
                long days = (now - application.lastChangeAt) / DAY_MS;
                addChip(context, context.getString(R.string.chip_no_reply, days), R.color.coral_tint, R.color.coral_text);
            }

            if (Application.STATUS_SAVED.equals(application.status) && application.deadline > 0) {
                long daysLeft = (application.deadline - now) / DAY_MS;
                if (daysLeft >= 0 && daysLeft <= 7) {
                    addChip(context, daysLeft == 0
                                    ? context.getString(R.string.closes_today)
                                    : context.getResources().getQuantityString(R.plurals.closes_in_days, (int) daysLeft, (int) daysLeft),
                            R.color.amber_tint, R.color.amber_text);
                }
            }

            if (application.interviewAt > now) {
                CharSequence when = DateFormat.format("EEE d MMM, h:mm a", new Date(application.interviewAt));
                addChip(context, context.getString(R.string.chip_interview_at, when), R.color.amber_tint, R.color.amber_text);
            }
        }

        private void addChip(Context context, String text, int backgroundRes, int textRes) {
            TextView chip = (TextView) LayoutInflater.from(context)
                    .inflate(R.layout.include_status_chip, binding.rowChips, false);
            chip.setText(text);
            chip.setBackgroundTintList(ContextCompat.getColorStateList(context, backgroundRes));
            chip.setTextColor(ContextCompat.getColor(context, textRes));
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chip.getLayoutParams();
            params.bottomMargin = (int) (4 * context.getResources().getDisplayMetrics().density);
            binding.rowChips.addView(chip);
        }
    }
}
