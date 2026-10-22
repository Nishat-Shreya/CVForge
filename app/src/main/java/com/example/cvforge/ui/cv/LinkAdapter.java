package com.example.cvforge.ui.cv;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ItemLinkBinding;

import java.util.List;

/** Editable list of links (GitHub, LinkedIn, portfolio). */
public class LinkAdapter extends EditableListAdapter<Cv.Link, LinkAdapter.ViewHolder> {

    public LinkAdapter(List<Cv.Link> items) {
        super(items);
    }

    @Override
    protected Cv.Link createItem() {
        return new Cv.Link();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLinkBinding binding = ItemLinkBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cv.Link item = items.get(position);
        holder.binding.editLabel.setText(item.label);
        holder.binding.editUrl.setText(item.url);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLinkBinding binding;

        ViewHolder(ItemLinkBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            SimpleTextWatcher.watch(binding.editLabel, text -> {
                Cv.Link item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.label = text;
                }
            });
            SimpleTextWatcher.watch(binding.editUrl, text -> {
                Cv.Link item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.url = text;
                }
            });
            binding.btnRemove.setOnClickListener(v -> removeAt(getAdapterPosition()));
        }
    }
}
