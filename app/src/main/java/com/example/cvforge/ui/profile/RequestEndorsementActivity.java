package com.example.cvforge.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.EndorsementRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Endorsement;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityRequestEndorsementBinding;
import com.example.cvforge.databinding.ItemPickPersonBinding;

import java.util.ArrayList;
import java.util.List;

/** The student asks a Teacher or Alumni to endorse one skill, and sees the state of the requests. */
public class RequestEndorsementActivity extends AppCompatActivity {

    private ActivityRequestEndorsementBinding binding;
    private final EndorsementRepository repository = new EndorsementRepository();
    private final UserRepository userRepository = new UserRepository();
    private final EndorsementAdapter adapter = new EndorsementAdapter();

    private List<String> skills = new ArrayList<>();
    private List<User> endorsers = new ArrayList<>();
    private String chosenSkill;
    private User chosenEndorser;
    private User student;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestEndorsementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(R.string.request_title);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.listRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.listRequests.setAdapter(adapter);

        binding.btnPickSkill.setOnClickListener(v -> pickSkill());
        binding.btnPickEndorser.setOnClickListener(v -> pickEndorser());
        binding.btnSend.setOnClickListener(v -> send());

        loadData();
    }

    private void loadData() {
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                student = user;
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RequestEndorsementActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        // the skills come from the newest CV
        new CvRepository(this).loadAll(userRepository.getCurrentUid(), cvs -> {
            if (!isDestroyed() && !cvs.isEmpty()) {
                skills = cvs.get(0).skills == null ? new ArrayList<>() : cvs.get(0).skills;
            }
        });
        userRepository.loadEndorsers(new UserListListener() {
            @Override
            public void onUsers(List<User> users) {
                endorsers = users;
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RequestEndorsementActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        loadRequests();
    }

    private void loadRequests() {
        repository.loadForStudent(userRepository.getCurrentUid(), new EndorsementRepository.ListListener() {
            @Override
            public void onLoaded(List<Endorsement> endorsements) {
                if (isDestroyed()) {
                    return;
                }
                adapter.setItems(endorsements);
                binding.textNoRequests.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.textNoRequests.setText(message);
                binding.textNoRequests.setVisibility(View.VISIBLE);
            }
        });
    }

    private void pickSkill() {
        if (skills.isEmpty()) {
            Toast.makeText(this, R.string.request_no_skills, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = skills.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.request_pick_skill)
                .setItems(names, (dialog, which) -> {
                    chosenSkill = names[which];
                    binding.btnPickSkill.setText(chosenSkill);
                })
                .show();
    }

    private void pickEndorser() {
        if (endorsers.isEmpty()) {
            Toast.makeText(this, R.string.request_no_endorsers, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[endorsers.size()];
        for (int i = 0; i < names.length; i++) {
            User user = endorsers.get(i);
            names[i] = user.getName() + " (" + getString("alumni".equals(user.getRole())
                    ? R.string.role_alumni : R.string.role_teacher) + ")";
        }
        // each row shows the round avatar of the teacher or alumni, with the letter as fallback
        android.widget.ListAdapter rows = new android.widget.BaseAdapter() {
            @Override
            public int getCount() {
                return names.length;
            }

            @Override
            public Object getItem(int position) {
                return names[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                ItemPickPersonBinding row = convertView == null
                        ? ItemPickPersonBinding.inflate(getLayoutInflater(), parent, false)
                        : ItemPickPersonBinding.bind(convertView);
                User user = endorsers.get(position);
                row.textPerson.setText(names[position]);
                row.avatar.textInitials.setText(initials(user.getName()));
                return row.getRoot();
            }
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.request_pick_endorser)
                .setAdapter(rows, (dialog, which) -> {
                    chosenEndorser = endorsers.get(which);
                    binding.btnPickEndorser.setText(names[which]);
                })
                .show();
    }

    private void send() {
        if (chosenSkill == null || chosenEndorser == null || student == null) {
            Toast.makeText(this, R.string.request_choose_both, Toast.LENGTH_SHORT).show();
            return;
        }
        Endorsement endorsement = new Endorsement();
        endorsement.studentId = userRepository.getCurrentUid();
        endorsement.studentName = student.getName();
        endorsement.skill = chosenSkill;
        endorsement.endorserId = chosenEndorser.getUid();
        endorsement.endorserName = chosenEndorser.getName();
        endorsement.endorserRole = chosenEndorser.getRole();

        binding.btnSend.setEnabled(false);
        repository.request(endorsement, new ResultListener() {
            @Override
            public void onSuccess() {
                binding.btnSend.setEnabled(true);
                Toast.makeText(RequestEndorsementActivity.this, R.string.request_sent, Toast.LENGTH_SHORT).show();
                chosenSkill = null;
                chosenEndorser = null;
                binding.btnPickSkill.setText(R.string.request_pick_skill);
                binding.btnPickEndorser.setText(R.string.request_pick_endorser);
                loadRequests();
            }

            @Override
            public void onError(String message) {
                binding.btnSend.setEnabled(true);
                Toast.makeText(RequestEndorsementActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
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
