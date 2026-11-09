package com.example.cvforge.ui.alumni;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.ChatRepository;
import com.example.cvforge.data.MentorRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.MentorSlot;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivitySlotsBinding;
import com.example.cvforge.ui.chat.ChatActivity;
import com.example.cvforge.ui.profile.SlotAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** For an alumni: the mentor slots they offer. Add a time, delete a slot nobody booked. */
public class SlotsActivity extends AppCompatActivity {

    private ActivitySlotsBinding binding;
    private final MentorRepository repository = new MentorRepository();
    private final UserRepository userRepository = new UserRepository();
    private SlotAdapter adapter;
    private String uid;
    private String name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySlotsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        uid = userRepository.getCurrentUid();

        binding.toolbar.textTitle.setText(R.string.slots_title);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.textHint.setText(R.string.slots_hint_alumni);
        binding.btnAddSlot.setVisibility(View.VISIBLE);
        binding.btnAddSlot.setOnClickListener(v -> pickTime());

        adapter = new SlotAdapter(false, uid, this::onSlotAction);
        binding.listSlots.setLayoutManager(new LinearLayoutManager(this));
        binding.listSlots.setAdapter(adapter);

        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                name = user.getName();
            }

            @Override
            public void onError(String message) {
                // the name stays empty, the slot still works
            }
        });
        load();
    }

    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        repository.loadByMentor(uid, new MentorRepository.ListListener() {
            @Override
            public void onLoaded(List<MentorSlot> slots) {
                if (isDestroyed()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                adapter.setSlots(slots);
                showEmpty(getString(R.string.slots_none_alumni), getString(R.string.slots_none_alumni_message));
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                showEmpty(getString(R.string.error_loading), message);
            }
        });
    }

    private void showEmpty(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Date first, then time. A time in the past is not accepted. */
    private void pickTime() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) ->
                new TimePickerDialog(this, (timePicker, hour, minute) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day, hour, minute, 0);
                    addSlot(chosen.getTimeInMillis());
                }, 17, 0, false).show(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void addSlot(long startAt) {
        if (startAt < System.currentTimeMillis()) {
            Toast.makeText(this, R.string.slot_in_past, Toast.LENGTH_SHORT).show();
            return;
        }
        repository.addSlot(uid, name, startAt, new ResultListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(SlotsActivity.this, R.string.slot_added, Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SlotsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Open slot: delete it. Booked slot: open the Mentor Chat with the student. */
    private void onSlotAction(MentorSlot slot) {
        if (slot.isOpen()) {
            confirmDelete(slot);
            return;
        }
        String time = DateFormat.format("EEE d MMM, HH:mm", new Date(slot.startAt)).toString();
        startActivity(ChatActivity.intent(this, ChatRepository.slotChatId(slot.id), slot.bookedById,
                slot.bookedByName, User.ROLE_STUDENT, getString(R.string.chat_context_mentor, time)));
    }

    private void confirmDelete(MentorSlot slot) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.slot_delete_ask)
                .setPositiveButton(R.string.slot_delete, (dialog, which) ->
                        repository.deleteSlot(slot.id, new ResultListener() {
                            @Override
                            public void onSuccess() {
                                load();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(SlotsActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
