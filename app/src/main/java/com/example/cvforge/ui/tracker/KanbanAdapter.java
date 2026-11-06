package com.example.cvforge.ui.tracker;

import android.content.res.ColorStateList;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.databinding.ItemKanbanColumnBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The columns of the Kanban board. They sit next to each other and the board scrolls sideways. */
public class KanbanAdapter extends RecyclerView.Adapter<KanbanAdapter.ViewHolder> {

    private static final KanbanColumn[] COLUMNS = KanbanColumn.values();

    /** Called when a card is dropped in another column. */
    public interface OnCardMovedListener {
        void onCardMoved(Application application, KanbanColumn target);
    }

    private final CardAdapter.OnCardClickListener listener;
    private final OnCardMovedListener movedListener;
    /** One list of cards for each column, in the order of COLUMNS. */
    private final List<List<Application>> cardsByColumn = new ArrayList<>();

    public KanbanAdapter(CardAdapter.OnCardClickListener listener, OnCardMovedListener movedListener) {
        this.listener = listener;
        this.movedListener = movedListener;
        for (int i = 0; i < COLUMNS.length; i++) {
            cardsByColumn.add(new ArrayList<>());
        }
    }

    /** Sorts the applications into the four columns. A rejected card stays in its old column. */
    public void setApplications(List<Application> applications) {
        for (List<Application> cards : cardsByColumn) {
            cards.clear();
        }
        for (Application application : applications) {
            for (int i = 0; i < COLUMNS.length; i++) {
                if (COLUMNS[i].status.equals(application.column())) {
                    cardsByColumn.get(i).add(application);
                }
            }
        }
        // the card that changed last is at the top of its column
        for (List<Application> cards : cardsByColumn) {
            Collections.sort(cards, (a, b) -> Long.compare(b.lastChangeAt, a.lastChangeAt));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemKanbanColumnBinding binding = ItemKanbanColumnBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(COLUMNS[position], cardsByColumn.get(position));
    }

    @Override
    public int getItemCount() {
        return COLUMNS.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemKanbanColumnBinding binding;
        private CardAdapter cardAdapter;
        private KanbanColumn column;

        ViewHolder(ItemKanbanColumnBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.listCards.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
            binding.getRoot().setOnDragListener(this::onDrag);
        }

        /** The whole column is a drop target. It lights up while a card is over it. */
        private boolean onDrag(View view, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getLocalState() instanceof Application;
                case DragEvent.ACTION_DRAG_ENTERED:
                    highlight(true);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    highlight(false);
                    return true;
                case DragEvent.ACTION_DROP:
                    highlight(false);
                    Application dropped = (Application) event.getLocalState();
                    if (!column.status.equals(dropped.column())) {
                        movedListener.onCardMoved(dropped, column);
                    }
                    return true;
                default:
                    return true;
            }
        }

        private void highlight(boolean on) {
            if (!on) {
                binding.getRoot().setBackground(null);
                return;
            }
            int tint = ContextCompat.getColor(itemView.getContext(), column.tintColor);
            binding.getRoot().setBackgroundResource(R.drawable.bg_tile);
            binding.getRoot().setBackgroundTintList(ColorStateList.valueOf(tint));
        }

        void bind(KanbanColumn column, List<Application> cards) {
            this.column = column;
            int tint = ContextCompat.getColor(itemView.getContext(), column.tintColor);
            int text = ContextCompat.getColor(itemView.getContext(), column.textColor);
            int strong = ContextCompat.getColor(itemView.getContext(), column.color);

            // header pill: soft color with its dark text, and a dot in the strong color
            binding.headerColumn.setBackgroundTintList(ColorStateList.valueOf(tint));
            binding.dotColumn.setBackgroundTintList(ColorStateList.valueOf(strong));
            binding.textColumnName.setText(column.nameRes);
            binding.textColumnName.setTextColor(text);
            binding.textColumnCount.setText(String.valueOf(cards.size()));
            binding.textColumnCount.setTextColor(text);

            // an empty column shows a dashed box in its color
            binding.textEmptyColumn.setBackgroundTintList(ColorStateList.valueOf(strong));
            binding.textEmptyColumn.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);

            cardAdapter = new CardAdapter(column, listener);
            binding.listCards.setAdapter(cardAdapter);
            cardAdapter.setCards(cards);
        }
    }
}
