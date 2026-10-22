package com.example.cvforge.ui.cv;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Base for the CV form lists (education, projects, ...). It keeps the list that is shared
 * with the Cv object, so edits are saved with the CV, and handles add and remove.
 */
public abstract class EditableListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    protected final List<T> items;

    protected EditableListAdapter(List<T> items) {
        this.items = items;
    }

    /** A new empty entry for the Add button. */
    protected abstract T createItem();

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem() {
        items.add(createItem());
        notifyItemInserted(items.size() - 1);
    }

    protected void removeAt(int position) {
        if (position != RecyclerView.NO_POSITION) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    /** The entry shown at a position, or null while the position is not known. */
    protected T itemAt(int position) {
        return position == RecyclerView.NO_POSITION ? null : items.get(position);
    }
}
