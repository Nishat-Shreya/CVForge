package com.example.cvforge.ui.cv;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ItemEducationBinding;

import java.util.List;

/** Editable list of education entries. Typing in a field changes the Cv.Education object directly. */
public class EducationAdapter extends EditableListAdapter<Cv.Education, EducationAdapter.ViewHolder> {

    public EducationAdapter(List<Cv.Education> items) {
        super(items);
    }

    @Override
    protected Cv.Education createItem() {
        return new Cv.Education();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEducationBinding binding = ItemEducationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cv.Education item = items.get(position);
        holder.binding.editSchool.setText(item.school);
        holder.binding.editDegree.setText(item.degree);
        holder.binding.editYears.setText(item.years);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemEducationBinding binding;

        ViewHolder(ItemEducationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // the watchers are added once and always write to the entry at the current position
            SimpleTextWatcher.watch(binding.editSchool, text -> {
                Cv.Education item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.school = text;
                }
            });
            SimpleTextWatcher.watch(binding.editDegree, text -> {
                Cv.Education item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.degree = text;
                }
            });
            SimpleTextWatcher.watch(binding.editYears, text -> {
                Cv.Education item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.years = text;
                }
            });
            binding.btnRemove.setOnClickListener(v -> removeAt(getAdapterPosition()));
        }
    }
}
