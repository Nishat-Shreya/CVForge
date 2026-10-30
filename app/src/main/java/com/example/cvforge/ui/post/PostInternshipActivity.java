package com.example.cvforge.ui.post;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.R;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityPostInternshipBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.ListText;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/** Form for a Company, Teacher or Alumni to post an internship (feature 3). */
public class PostInternshipActivity extends AppCompatActivity {

    private ActivityPostInternshipBinding binding;
    private final UserRepository userRepository = new UserRepository();
    private final InternshipRepository internshipRepository = new InternshipRepository();

    /** The last day to apply. Starts 30 days from today. */
    private final Calendar deadline = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostInternshipBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(R.string.new_post);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        deadline.add(Calendar.DAY_OF_YEAR, 30);
        showDeadline();
        binding.btnDeadline.setOnClickListener(v -> pickDeadline());
        binding.btnPost.setOnClickListener(v -> submit());

        AnimUtils.slideUpIn(binding.cardForm, 0);
    }

    // ---------- deadline ----------

    private void pickDeadline() {
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) -> {
            deadline.set(year, month, day);
            showDeadline();
        }, deadline.get(Calendar.YEAR), deadline.get(Calendar.MONTH), deadline.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showDeadline() {
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(deadline.getTimeInMillis()));
        binding.btnDeadline.setText(getString(R.string.deadline_format, date));
    }

    /** The deadline counts until the end of the chosen day. */
    private long deadlineMillis() {
        Calendar endOfDay = (Calendar) deadline.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        return endOfDay.getTimeInMillis();
    }

    // ---------- posting ----------

    private void submit() {
        if (!isInputValid()) {
            return;
        }
        setLoading(true);
        // the post needs the poster's name and role from the users document
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                post(buildInternship(user));
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private Internship buildInternship(User user) {
        Internship internship = new Internship();
        internship.posterId = user.getUid();
        internship.posterName = user.getName();
        internship.posterRole = user.getRole();
        internship.title = binding.editTitle.getText().toString().trim();
        internship.description = binding.editDescription.getText().toString().trim();
        internship.skills = ListText.split(binding.editSkills.getText().toString(), ",");
        internship.stipend = readStipend();
        internship.location = binding.editLocation.getText().toString().trim();
        internship.type = selectedType();
        internship.deadline = deadlineMillis();
        return internship;
    }

    private void post(Internship internship) {
        internshipRepository.post(internship, new ResultListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(PostInternshipActivity.this,
                        R.string.post_done, Toast.LENGTH_SHORT).show();
                finish();
                AnimUtils.fadeTransition(PostInternshipActivity.this);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private int readStipend() {
        String text = binding.editStipend.getText().toString().trim();
        try {
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String selectedType() {
        View chip = binding.groupType.findViewById(binding.groupType.getCheckedChipId());
        return chip == null ? Internship.TYPE_ONSITE : (String) chip.getTag();
    }

    // ---------- checks and states ----------

    private boolean isInputValid() {
        binding.layoutTitle.setError(null);
        binding.layoutDescription.setError(null);
        binding.layoutSkills.setError(null);
        binding.layoutLocation.setError(null);
        binding.textError.setVisibility(View.GONE);

        boolean valid = true;
        if (binding.editTitle.getText().toString().trim().isEmpty()) {
            binding.layoutTitle.setError(getString(R.string.error_post_title));
            valid = false;
        }
        if (binding.editDescription.getText().toString().trim().isEmpty()) {
            binding.layoutDescription.setError(getString(R.string.error_post_description));
            valid = false;
        }
        if (ListText.split(binding.editSkills.getText().toString(), ",").isEmpty()) {
            binding.layoutSkills.setError(getString(R.string.error_post_skills));
            valid = false;
        }
        if (binding.editLocation.getText().toString().trim().isEmpty()) {
            binding.layoutLocation.setError(getString(R.string.error_post_location));
            valid = false;
        }
        return valid;
    }

    private void showError(String message) {
        setLoading(false);
        binding.textError.setText(message);
        binding.textError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.btnPost.setEnabled(!loading);
        binding.btnPost.setText(loading ? R.string.posting : R.string.post_internship);
    }
}
