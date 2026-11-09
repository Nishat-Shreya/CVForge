package com.example.cvforge.ui.profile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** For a student: open mentor slots of alumni to book, and the slots they already booked. */
public class BookSlotActivity extends AppCompatActivity {

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
        binding.textHint.setText(R.string.slots_hint_student);

        adapter = new SlotAdapter(true, uid, this::onAction);
        binding.listSlots.setLayoutManager(new LinearLayoutManager(this));
        binding.listSlots.setAdapter(adapter);

        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                name = user.getName();
            }

            @Override
            public void onError(String message) {
                // the name stays empty, the booking still works
            }
        });
        load();
    }

    /** Two lists are merged: the open slots, and the slots this student booked. */
    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        List<MentorSlot> merged = new ArrayList<>();
        repository.loadOpen(new MentorRepository.ListListener() {
            @Override
            public void onLoaded(List<MentorSlot> open) {
                merged.addAll(open);
                loadBooked(merged);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void loadBooked(List<MentorSlot> merged) {
        repository.loadBookedBy(uid, new MentorRepository.ListListener() {
            @Override
            public void onLoaded(List<MentorSlot> booked) {
                if (isDestroyed()) {
                    return;
                }
                merged.addAll(booked);
                Collections.sort(merged, (a, b) -> Long.compare(a.startAt, b.startAt));
                binding.progress.setVisibility(View.GONE);
                adapter.setSlots(merged);
                showEmpty(getString(R.string.slots_none_student), getString(R.string.slots_none_student_message));
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void showError(String message) {
        if (isDestroyed()) {
            return;
        }
        binding.progress.setVisibility(View.GONE);
        showEmpty(getString(R.string.error_loading), message);
    }

    private void showEmpty(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Open slot: book it. A slot this student booked: choose the chat or the calendar. */
    private void onAction(MentorSlot slot) {
        if (slot.isOpen()) {
            confirmBooking(slot);
        } else {
            showBookedOptions(slot);
        }
    }

    private void showBookedOptions(MentorSlot slot) {
        String[] options = {getString(R.string.slot_open_chat), getString(R.string.slot_add_calendar)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.slot_options_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openChat(slot);
                    } else {
                        addToCalendar(slot);
                    }
                })
                .show();
    }

    /** Mentor Chat: the student talks to the alumni about this booking. */
    private void openChat(MentorSlot slot) {
        String time = DateFormat.format("EEE d MMM, HH:mm", new Date(slot.startAt)).toString();
        startActivity(ChatActivity.intent(this, ChatRepository.slotChatId(slot.id), slot.mentorId,
                slot.mentorName, User.ROLE_ALUMNI, getString(R.string.chat_context_mentor, time)));
    }

    private void confirmBooking(MentorSlot slot) {
        String time = DateFormat.format("EEE d MMM, HH:mm", new Date(slot.startAt)).toString();
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.slot_book_ask, slot.mentorName, time))
                .setPositiveButton(R.string.slot_book, (dialog, which) -> book(slot))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void book(MentorSlot slot) {
        repository.book(slot, uid, name, new ResultListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(BookSlotActivity.this, R.string.slot_booked_done, Toast.LENGTH_SHORT).show();
                load();
                // the chat opens right after booking, to say hello and agree on how to meet
                openChat(slot);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BookSlotActivity.this, message, Toast.LENGTH_LONG).show();
                load();
            }
        });
    }

    /** Implicit Intent: the calendar app opens with the session filled in. */
    private void addToCalendar(MentorSlot slot) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, getString(R.string.slot_calendar_title, slot.mentorName))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, slot.startAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        slot.startAt + MentorSlot.LENGTH_MINUTES * 60L * 1000L);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_calendar_app, Toast.LENGTH_SHORT).show();
        }
    }
}
