package com.example.cvforge.ui.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.EndorsementRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Endorsement;
import com.example.cvforge.databinding.ActivityEndorseBinding;
import com.example.cvforge.databinding.ItemEndorseRequestBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/** For a Teacher or Alumni: the endorsement requests of students. Endorse with a comment, or decline. */
public class EndorseActivity extends AppCompatActivity {

    private ActivityEndorseBinding binding;
    private final EndorsementRepository repository = new EndorsementRepository();
    private final RequestAdapter adapter = new RequestAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEndorseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(R.string.endorse_title);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.listRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.listRequests.setAdapter(adapter);
        load();
    }

    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        repository.loadPendingForEndorser(new UserRepository().getCurrentUid(), new EndorsementRepository.ListListener() {
            @Override
            public void onLoaded(List<Endorsement> endorsements) {
                if (isDestroyed()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                adapter.setItems(endorsements);
                showEmptyState(getString(R.string.endorse_none), getString(R.string.endorse_none_message));
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                showEmptyState(getString(R.string.error_loading), message);
            }
        });
    }

    private void respond(Endorsement endorsement, boolean approve, String comment) {
        repository.respond(endorsement, approve, comment, new ResultListener() {
            @Override
            public void onSuccess() {
                adapter.remove(endorsement);
                Toast.makeText(EndorseActivity.this,
                        approve ? R.string.endorse_done : R.string.endorse_declined_done, Toast.LENGTH_SHORT).show();
                showEmptyState(getString(R.string.endorse_none), getString(R.string.endorse_none_message));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EndorseActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    /** The request cards. The comment box belongs to the card, so it is read when a button is tapped. */
    private class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

        private final List<Endorsement> items = new ArrayList<>();

        void setItems(List<Endorsement> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        void remove(Endorsement endorsement) {
            int index = items.indexOf(endorsement);
            if (index >= 0) {
                items.remove(index);
                notifyItemRemoved(index);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemEndorseRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Endorsement item = items.get(position);
            ItemEndorseRequestBinding b = holder.binding;
            b.textStudent.setText(item.studentName);
            b.avatar.textInitials.setText(initials(item.studentName));
            b.textSkill.setText(item.skill);
            b.editComment.setText("");
            b.btnEndorse.setOnClickListener(v ->
                    respond(item, true, b.editComment.getText().toString().trim()));
            b.btnDecline.setOnClickListener(v -> respond(item, false, ""));
            AnimUtils.slideUpIn(holder.itemView, Math.min(position, 6));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private final ItemEndorseRequestBinding binding;

            ViewHolder(ItemEndorseRequestBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }
}
