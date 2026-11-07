package com.example.cvforge.ui.discover;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.RoadmapRepository.Step;
import com.example.cvforge.databinding.ItemStepBinding;

import java.util.ArrayList;
import java.util.List;

/** The steps of the skill gap roadmap. Ticking a step tells the screen, which saves it. */
public class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {

    public interface Listener {
        void onStepChanged();
    }

    private final List<Step> steps = new ArrayList<>();
    private final Listener listener;

    public StepAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setSteps(List<Step> newSteps) {
        steps.clear();
        steps.addAll(newSteps);
        notifyDataSetChanged();
    }

    public List<Step> getSteps() {
        return steps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStepBinding binding = ItemStepBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Step step = steps.get(position);
        holder.binding.textSkill.setText(step.skill);
        holder.binding.textTask.setText(step.task);
        holder.binding.textTime.setText(step.time);
        holder.binding.textTime.setVisibility(step.time == null || step.time.isEmpty()
                ? android.view.View.GONE : android.view.View.VISIBLE);
        // set the state before the listener, so binding does not count as a tick
        holder.binding.checkStep.setOnCheckedChangeListener(null);
        holder.binding.checkStep.setChecked(step.done);
        holder.binding.checkStep.setOnCheckedChangeListener((button, checked) -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                steps.get(pos).done = checked;
                listener.onStepChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemStepBinding binding;

        ViewHolder(ItemStepBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
