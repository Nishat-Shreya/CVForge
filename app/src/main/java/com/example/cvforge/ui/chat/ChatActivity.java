package com.example.cvforge.ui.chat;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.ChatRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.ChatMessage;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityChatBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.ui.cv.SimpleTextWatcher;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat between two people about an application or a mentor slot (feature 16).
 * The messages arrive live: the Firestore listeners start in onStart and stop in onStop (W8).
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chat_id";
    public static final String EXTRA_OTHER_ID = "other_id";
    public static final String EXTRA_OTHER_NAME = "other_name";
    /** The role of the other person. Company, teacher and alumni accounts are verified, so they get the chip. */
    public static final String EXTRA_OTHER_ROLE = "other_role";
    /** What the chat is about, for example "Android Intern · Interview tomorrow". */
    public static final String EXTRA_CONTEXT = "context";

    /** The typing message is sent at most this often. */
    private static final long TYPING_SEND_MS = 2000;

    private ActivityChatBinding binding;
    private final ChatRepository repository = new ChatRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MessageAdapter adapter;

    private String chatId;
    private String myUid;
    private String otherId;
    private String otherName;

    private boolean chatReady;
    private boolean started;
    private ListenerRegistration messagesRegistration;
    private ListenerRegistration typingRegistration;
    private long lastTypingSent;
    private final List<ObjectAnimator> dotAnimators = new ArrayList<>();
    private final Runnable hideDots = () -> showTyping(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        otherId = getIntent().getStringExtra(EXTRA_OTHER_ID);
        otherName = getIntent().getStringExtra(EXTRA_OTHER_NAME);
        String context = getIntent().getStringExtra(EXTRA_CONTEXT);
        myUid = new UserRepository().getCurrentUid();

        showHeader(context);
        setUpList();
        setUpInput();
        setUpQuickReplies();
        openChat(context);
    }

    private void showHeader(String context) {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.textName.setText(otherName);
        binding.textInitials.setText(initials(otherName));
        binding.editMessage.setHint(getString(R.string.chat_hint_name, otherName));
        binding.chipVerified.setVisibility(
                isVerifiedRole(getIntent().getStringExtra(EXTRA_OTHER_ROLE)) ? View.VISIBLE : View.GONE);
        if (context != null && !context.isEmpty()) {
            binding.textBanner.setText(context);
            binding.textBanner.setVisibility(View.VISIBLE);
        }
    }

    private void setUpList() {
        adapter = new MessageAdapter(myUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.listMessages.setLayoutManager(layoutManager);
        binding.listMessages.setAdapter(adapter);
    }

    private void setUpInput() {
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                send(text);
                binding.editMessage.setText("");
            }
        });
        // tell the other person that I am typing, but not on every key
        SimpleTextWatcher.watch(binding.editMessage, text -> {
            long now = System.currentTimeMillis();
            if (chatReady && !text.isEmpty() && now - lastTypingSent > TYPING_SEND_MS) {
                lastTypingSent = now;
                repository.setTyping(chatId, myUid);
            }
        });
    }

    /** Short answers with one tap. */
    private void setUpQuickReplies() {
        int[] labels = {R.string.quick_confirm, R.string.quick_thanks, R.string.quick_move};
        int[] texts = {R.string.quick_confirm_text, R.string.quick_thanks_text, R.string.quick_move_text};
        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            chip.setText(labels[i]);
            chip.setChipMinHeight(44 * getResources().getDisplayMetrics().density);
            chip.setTextColor(getColor(R.color.violet_text));
            chip.setChipBackgroundColorResource(R.color.card);
            chip.setChipStrokeColorResource(R.color.border);
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            int textRes = texts[i];
            chip.setOnClickListener(v -> send(getString(textRes)));
            binding.groupQuick.addView(chip);
        }
    }

    // ---------- Firestore ----------

    /** The chat document must exist before the listeners can read it. */
    private void openChat(String context) {
        // my own name and role are saved in the chat, so the other person sees them in the chat list
        new UserRepository().loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                ChatRepository.Person me = new ChatRepository.Person(myUid, user.getName(), user.getRole());
                ChatRepository.Person other = new ChatRepository.Person(otherId, otherName,
                        getIntent().getStringExtra(EXTRA_OTHER_ROLE));
                repository.openChat(chatId, me, other, context, new ResultListener() {
                    @Override
                    public void onSuccess() {
                        chatReady = true;
                        if (started && !isDestroyed()) {
                            startListening();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        showError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private static boolean isVerifiedRole(String role) {
        return role != null && !role.isEmpty() && !User.ROLE_STUDENT.equals(role);
    }

    /** The Intent that opens a chat. Used by every screen that has a chat button. */
    public static Intent intent(Context context, String chatId, String otherId, String otherName,
                                String otherRole, String contextTitle) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        intent.putExtra(EXTRA_OTHER_ID, otherId);
        intent.putExtra(EXTRA_OTHER_NAME, otherName);
        intent.putExtra(EXTRA_OTHER_ROLE, otherRole);
        intent.putExtra(EXTRA_CONTEXT, contextTitle);
        return intent;
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
        if (chatReady) {
            startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        started = false;
        stopListening();
    }

    private void startListening() {
        stopListening();
        messagesRegistration = repository.listenMessages(chatId, new ChatRepository.MessagesListener() {
            @Override
            public void onMessages(List<ChatMessage> messages) {
                binding.textError.setVisibility(View.GONE);
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    binding.listMessages.scrollToPosition(messages.size() - 1);
                }
                // the chat is open, so everything in it counts as read
                repository.markRead(chatId, myUid);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
        typingRegistration = repository.listenTyping(chatId, myUid, this::showTyping);
    }

    private void stopListening() {
        if (messagesRegistration != null) {
            messagesRegistration.remove();
            messagesRegistration = null;
        }
        if (typingRegistration != null) {
            typingRegistration.remove();
            typingRegistration = null;
        }
        handler.removeCallbacks(hideDots);
        stopDots();
    }

    private void send(String text) {
        if (!chatReady) {
            return;
        }
        repository.sendMessage(chatId, myUid, otherId, text, ChatMessage.TYPE_TEXT, new ResultListener() {
            @Override
            public void onSuccess() {
                // the new message arrives through the listener
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ChatActivity.this, R.string.chat_send_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showError(String message) {
        if (!isDestroyed()) {
            binding.textError.setText(getString(R.string.chat_error, message));
            binding.textError.setVisibility(View.VISIBLE);
        }
    }

    // ---------- typing dots (A11) ----------

    private void showTyping(boolean typing) {
        handler.removeCallbacks(hideDots);
        if (binding == null) {
            return;
        }
        binding.typingDots.setVisibility(typing ? View.VISIBLE : View.GONE);
        if (typing) {
            startDots();
            // the dots go away if no new typing message arrives
            handler.postDelayed(hideDots, ChatRepository.TYPING_MS);
        } else {
            stopDots();
        }
    }

    /** The three dots blink one after another. */
    private void startDots() {
        if (!dotAnimators.isEmpty() || !AnimUtils.animationsEnabled(this)) {
            return;
        }
        View[] dots = {binding.dot1, binding.dot2, binding.dot3};
        for (int i = 0; i < dots.length; i++) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(dots[i], View.ALPHA, 1f, 0.25f);
            animator.setDuration(450);
            animator.setStartDelay(i * 150L);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.start();
            dotAnimators.add(animator);
        }
    }

    private void stopDots() {
        for (ObjectAnimator animator : dotAnimators) {
            animator.cancel();
        }
        dotAnimators.clear();
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }
}
