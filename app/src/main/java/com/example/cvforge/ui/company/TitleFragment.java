package com.example.cvforge.ui.company;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cvforge.databinding.FragmentCompanyTabBinding;
import com.example.cvforge.ui.auth.LogoutHelper;
import com.example.cvforge.ui.post.MyPostsActivity;
import com.example.cvforge.util.AnimUtils;

/**
 * Placeholder tab for the company shell: a title and, on the Profile tab, a Log out button.
 * The real Postings and Applicants screens come in later checkpoints.
 */
public class TitleFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_LOGOUT = "logout";
    private static final String ARG_POST = "post";

    private FragmentCompanyTabBinding binding;

    public static TitleFragment newInstance(int titleRes, boolean showLogout, boolean showPost) {
        TitleFragment fragment = new TitleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE, titleRes);
        args.putBoolean(ARG_LOGOUT, showLogout);
        args.putBoolean(ARG_POST, showPost);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCompanyTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textTitle.setText(requireArguments().getInt(ARG_TITLE));

        boolean showLogout = requireArguments().getBoolean(ARG_LOGOUT);
        binding.btnLogout.setVisibility(showLogout ? View.VISIBLE : View.GONE);
        binding.btnLogout.setOnClickListener(v -> LogoutHelper.logOut(requireActivity()));

        // the Postings tab has a button that opens the poster's own postings
        binding.btnPost.setVisibility(requireArguments().getBoolean(ARG_POST) ? View.VISIBLE : View.GONE);
        binding.btnPost.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MyPostsActivity.class));
            AnimUtils.fadeTransition(requireActivity());
        });

        AnimUtils.slideUpIn(binding.textTitle, 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
