package com.example.cvforge.ui.post;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.databinding.ActivityApplicantsBinding;

/**
 * Hosts the applicants list for Teacher and Alumni, who have no bottom navigation.
 * A Company sees the same list in its Applicants tab.
 */
public class ApplicantsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityApplicantsBinding.inflate(getLayoutInflater()).getRoot());
    }
}
