package com.example.cvforge.ui.cv;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.databinding.ItemRepoBinding;
import com.example.cvforge.network.model.GithubRepo;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/** GitHub repositories with a checkbox on each card. */
public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<GithubRepo> repos = new ArrayList<>();
    private final OnSelectionChangedListener listener;

    public RepoAdapter(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setRepos(List<GithubRepo> newRepos) {
        repos.clear();
        repos.addAll(newRepos);
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    public boolean isEmpty() {
        return repos.isEmpty();
    }

    /** The repositories the student ticked. */
    public List<GithubRepo> getSelected() {
        List<GithubRepo> selected = new ArrayList<>();
        for (GithubRepo repo : repos) {
            if (repo.selected) {
                selected.add(repo);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRepoBinding binding = ItemRepoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(repos.get(position));
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 8));
    }

    @Override
    public int getItemCount() {
        return repos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemRepoBinding binding;

        ViewHolder(ItemRepoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GithubRepo repo) {
            binding.textRepoName.setText(repo.name);
            binding.textRepoDescription.setText(repo.description);
            binding.textRepoDescription.setVisibility(
                    repo.description == null || repo.description.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
            binding.textRepoLanguage.setText(repo.language == null ? "-" : repo.language);
            binding.textRepoStars.setText(itemView.getContext().getString(R.string.stars_format, repo.stars));

            // set the state before the listener, so binding does not count as a click
            binding.checkRepo.setOnCheckedChangeListener(null);
            binding.checkRepo.setChecked(repo.selected);
            binding.checkRepo.setOnCheckedChangeListener((button, checked) -> {
                repo.selected = checked;
                listener.onSelectionChanged(getSelected().size());
            });
            binding.getRoot().setOnClickListener(v -> binding.checkRepo.toggle());
        }
    }
}
