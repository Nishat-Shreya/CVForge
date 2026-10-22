package com.example.cvforge.ui.cv;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ItemExperienceBinding;
import com.example.cvforge.util.ListText;

import java.util.List;

/** Editable list of work experience. Bullet points are typed one per line. */
public class ExperienceAdapter extends EditableListAdapter<Cv.Experience, ExperienceAdapter.ViewHolder> {

    public ExperienceAdapter(List<Cv.Experience> items) {
        super(items);
    }

    @Override
    protected Cv.Experience createItem() {
        return new Cv.Experience();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExperienceBinding binding = ItemExperienceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cv.Experience item = items.get(position);
        holder.binding.editCompany.setText(item.company);
        holder.binding.editRole.setText(item.role);
        holder.binding.editYears.setText(item.years);
        holder.binding.editBullets.setText(ListText.join(item.bullets, "\n"));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemExperienceBinding binding;

        ViewHolder(ItemExperienceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            SimpleTextWatcher.watch(binding.editCompany, text -> {
                Cv.Experience item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.company = text;
                }
            });
            SimpleTextWatcher.watch(binding.editRole, text -> {
                Cv.Experience item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.role = text;
                }
            });
            SimpleTextWatcher.watch(binding.editYears, text -> {
                Cv.Experience item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.years = text;
                }
            });
            SimpleTextWatcher.watch(binding.editBullets, text -> {
                Cv.Experience item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.bullets = ListText.split(text, "\n");
                }
            });
            binding.btnRemove.setOnClickListener(v -> removeAt(getAdapterPosition()));
        }
    }
}
