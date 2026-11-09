package com.example.cvforge.ui.chat;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.ChatRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Chat;
import com.example.cvforge.databinding.ActivityChatsBinding;
import com.example.cvforge.util.AnimUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/** The list of all chats of the user. A tap opens one chat. The list is live while the screen is visible. */
public class ChatsActivity extends AppCompatActivity {

    private ActivityChatsBinding binding;
    private ChatListAdapter adapter;
    private ListenerRegistration registration;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        myUid = new UserRepository().getCurrentUid();

        binding.toolbar.textTitle.setText(R.string.chats_title);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        adapter = new ChatListAdapter(myUid, this::openChat);
        binding.listChats.setLayoutManager(new LinearLayoutManager(this));
        binding.listChats.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.progress.setVisibility(View.VISIBLE);
        registration = new ChatRepository().listenMyChats(myUid, new ChatRepository.ChatsListener() {
            @Override
            public void onChats(List<Chat> chats) {
                binding.progress.setVisibility(View.GONE);
                adapter.setChats(chats);
                showEmpty(getString(R.string.chats_none), getString(R.string.chats_none_message));
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                showEmpty(getString(R.string.error_loading), message);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void openChat(Chat chat) {
        startActivity(ChatActivity.intent(this, chat.id, chat.otherId(myUid), chat.otherName(myUid),
                chat.otherRole(myUid), chat.contextTitle));
        AnimUtils.fadeTransition(this);
    }

    private void showEmpty(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
