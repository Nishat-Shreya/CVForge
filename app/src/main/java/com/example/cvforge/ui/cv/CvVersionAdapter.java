package com.example.cvforge.ui.cv;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ItemCvVersionBinding;

import java.util.ArrayList;
import java.util.List;

/** Row of chips, one per CV version, plus a "New version" chip at the end. */
public class CvVersionAdapter extends RecyclerView.Adapter<CvVersionAdapter.ViewHolder> {

    public interface Listener {
        void onVersionClick(Cv cv);

        void onVersionLongClick(Cv cv);

        void onNewVersionClick();
    }

    private final List<Cv> versions = new ArrayList<>();
    private final Listener listener;
    private String selectedId;

    public CvVersionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setVersions(List<Cv> newVersions, String selectedId) {
        versions.clear();
        versions.addAll(newVersions);
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCvVersionBinding binding = ItemCvVersionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == versions.size()) {
            holder.bindNewVersion();
        } else {
            holder.bindVersion(versions.get(position), versions.get(position).id.equals(selectedId));
        }
    }

    @Override
    public int getItemCount() {
        return versions.size() + 1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCvVersionBinding binding;

        ViewHolder(ItemCvVersionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindVersion(Cv cv, boolean selected) {
            int background = selected ? R.color.violet : R.color.violet_tint;
            int text = selected ? R.color.white : R.color.violet_text;
            style(background, text);
            binding.chipVersion.setText(cv.title == null || cv.title.isEmpty() ? "CV" : cv.title);
            binding.chipVersion.setOnClickListener(v -> listener.onVersionClick(cv));
            binding.chipVersion.setOnLongClickListener(v -> {
                listener.onVersionLongClick(cv);
                return true;
            });
        }

        void bindNewVersion() {
            style(R.color.amber_tint, R.color.amber_text);
            binding.chipVersion.setText(R.string.new_version);
            binding.chipVersion.setOnClickListener(v -> listener.onNewVersionClick());
            binding.chipVersion.setOnLongClickListener(null);
        }

        private void style(int backgroundRes, int textRes) {
            binding.chipVersion.setBackgroundTintList(
                    ContextCompat.getColorStateList(itemView.getContext(), backgroundRes));
            binding.chipVersion.setTextColor(ContextCompat.getColor(itemView.getContext(), textRes));
        }
    }
}
