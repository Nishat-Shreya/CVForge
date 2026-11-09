package com.example.cvforge.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.EndorsementRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Endorsement;
import com.example.cvforge.databinding.FragmentProfileBinding;
import com.example.cvforge.ui.profile.BookSlotActivity;
import com.example.cvforge.ui.profile.EndorsementAdapter;
import com.example.cvforge.ui.profile.RequestEndorsementActivity;
import com.example.cvforge.util.AnimUtils;

import java.util.List;

/**
 * Profile tab for now: the verified endorsements and buttons to ask for one or book a mentor.
 * The rest of the profile is built in a later checkpoint.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private final EndorsementAdapter adapter = new EndorsementAdapter();
    private final UserRepository userRepository = new UserRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.listEndorsements.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listEndorsements.setAdapter(adapter);
        binding.btnRequestEndorsement.setOnClickListener(v -> open(RequestEndorsementActivity.class));
        binding.btnBookMentor.setOnClickListener(v -> open(BookSlotActivity.class));
        AnimUtils.slideUpIn(binding.textTitle, 0);
        AnimUtils.slideUpIn(binding.listEndorsements, 1);
    }

    /** Reloads when the student comes back, so a new endorsement shows up. */
    @Override
    public void onStart() {
        super.onStart();
        loadEndorsements();
    }

    private void loadEndorsements() {
        new EndorsementRepository().loadApproved(userRepository.getCurrentUid(),
                new EndorsementRepository.ListListener() {
                    @Override
                    public void onLoaded(List<Endorsement> endorsements) {
                        if (binding == null) {
                            return;
                        }
                        adapter.setItems(endorsements);
                        binding.textNoEndorsements.setText(R.string.endorsements_none);
                        binding.textNoEndorsements.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (binding != null && adapter.isEmpty()) {
                            binding.textNoEndorsements.setText(R.string.error_loading);
                            binding.textNoEndorsements.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(requireContext(), screen));
        AnimUtils.fadeTransition(requireActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
