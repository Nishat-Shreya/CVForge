package com.example.cvforge.ui.cv;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ItemProjectBinding;
import com.example.cvforge.util.ListText;

import java.util.List;

/** Editable list of projects. Tags are typed as one line separated by commas. */
public class ProjectAdapter extends EditableListAdapter<Cv.Project, ProjectAdapter.ViewHolder> {

    public ProjectAdapter(List<Cv.Project> items) {
        super(items);
    }

    @Override
    protected Cv.Project createItem() {
        return new Cv.Project();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProjectBinding binding = ItemProjectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cv.Project item = items.get(position);
        holder.binding.editName.setText(item.name);
        holder.binding.editDescription.setText(item.description);
        holder.binding.editTags.setText(ListText.join(item.tags, ", "));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemProjectBinding binding;

        ViewHolder(ItemProjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            SimpleTextWatcher.watch(binding.editName, text -> {
                Cv.Project item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.name = text;
                }
            });
            SimpleTextWatcher.watch(binding.editDescription, text -> {
                Cv.Project item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.description = text;
                }
            });
            SimpleTextWatcher.watch(binding.editTags, text -> {
                Cv.Project item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.tags = ListText.split(text, ",");
                }
            });
            binding.btnRemove.setOnClickListener(v -> removeAt(getAdapterPosition()));
        }
    }
}
