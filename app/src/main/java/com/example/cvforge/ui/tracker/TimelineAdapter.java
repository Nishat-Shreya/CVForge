package com.example.cvforge.ui.tracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.model.Application;
import com.example.cvforge.databinding.ItemTimelineBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** The status timeline of an application, newest step first. */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private final List<Application.StatusChange> steps = new ArrayList<>();

    public void setSteps(List<Application.StatusChange> history) {
        steps.clear();
        steps.addAll(history);
        Collections.reverse(steps);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimelineBinding binding = ItemTimelineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(steps.get(position), position == steps.size() - 1);
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 6));
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemTimelineBinding binding;

        ViewHolder(ItemTimelineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Application.StatusChange step, boolean isLast) {
            Context context = itemView.getContext();
            int strong = ContextCompat.getColor(context, StatusStyle.colorRes(step.status));

            binding.viewDot.setBackgroundTintList(ColorStateList.valueOf(strong));
            binding.chipStatus.setText(StatusStyle.labelRes(step.status));
            binding.chipStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, StatusStyle.tintRes(step.status)));
            binding.chipStatus.setTextColor(ContextCompat.getColor(context, StatusStyle.textRes(step.status)));
            binding.textTime.setText(DateFormat.format("d MMM yyyy, h:mm a", new Date(step.at)));
            // the line goes down to the next (older) step, so the last one has no line
            binding.viewLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        }
    }
}
