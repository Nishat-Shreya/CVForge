package com.example.cvforge.ui.coach;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.ai.AiRepository;
import com.example.cvforge.ai.Prompts;
import com.example.cvforge.databinding.ActivityCoachBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;

/**
 * Interview coach (feature 12). Gemini asks questions for one internship, the student answers
 * by voice (SpeechRecognizer) or by typing, and the coach gives feedback after each answer.
 * The practice is not saved. Nothing is recorded: the phone turns the voice into text.
 */
public class CoachActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "role";
    public static final String EXTRA_COMPANY = "company";

    /** The coach asks this many questions, then gives a summary. */
    private static final int QUESTIONS = 4;

    private ActivityCoachBinding binding;
    private CoachAdapter adapter;
    private AiRepository ai;
    private String role;
    private String company;

    private final ArrayList<String> transcript = new ArrayList<>();
    private int answers;
    private boolean waitingForCoach;
    private boolean finished;
    /** True after a failed AI call: the next tap on Send asks the coach again. */
    private boolean retryNeeded;
    private boolean retryFirst;

    private SpeechRecognizer recognizer;
    private boolean listening;
    private ObjectAnimator micPulse;

    private final ActivityResultLauncher<String> micPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else {
                    Toast.makeText(this, R.string.coach_mic_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCoachBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        role = getIntent().getStringExtra(EXTRA_ROLE);
        company = getIntent().getStringExtra(EXTRA_COMPANY);
        ai = new AiRepository(this);

        binding.toolbar.textTitle.setText(R.string.coach_title);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        adapter = new CoachAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.listChat.setLayoutManager(layoutManager);
        binding.listChat.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> sendAnswer());
        binding.btnMic.setOnClickListener(v -> onMicClicked());
        // phones without a speech service: typing still works
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            binding.btnMic.setVisibility(View.GONE);
        }

        askCoach(true);
    }

    // ---------- the chat with Gemini ----------

    private void sendAnswer() {
        if (retryNeeded && !waitingForCoach) {
            retryNeeded = false;
            askCoach(retryFirst);
            return;
        }
        String answer = binding.editAnswer.getText().toString().trim();
        if (answer.isEmpty() || waitingForCoach || finished) {
            return;
        }
        stopListening();
        binding.editAnswer.setText("");
        transcript.add("Student: " + answer);
        addBubble(answer, false);
        answers++;
        askCoach(false);
    }

    private void askCoach(boolean first) {
        waitingForCoach = true;
        binding.btnSend.setEnabled(false);
        addBubble(getString(R.string.coach_typing), true);

        boolean last = answers >= QUESTIONS;
        String prompt = Prompts.interviewTurn(role, company, android.text.TextUtils.join("\n", transcript), first, last);
        ai.generate(prompt, new AiRepository.Callback() {
            @Override
            public void onResult(String text) {
                if (isDestroyed()) {
                    return;
                }
                adapter.removeLast();
                transcript.add("Coach: " + text);
                addBubble(text, true);
                finishCoachTurn(last);
            }

            @Override
            public void onError(String message) {
                if (isDestroyed()) {
                    return;
                }
                adapter.removeLast();
                addBubble(getString(R.string.coach_failed, message), true);
                // Send now asks the coach again, without a new answer
                retryFirst = first;
                retryNeeded = true;
                finishCoachTurn(false);
            }
        });
    }

    private void finishCoachTurn(boolean last) {
        waitingForCoach = false;
        finished = last;
        binding.btnSend.setEnabled(!last);
        if (last) {
            binding.editAnswer.setEnabled(false);
            binding.btnMic.setEnabled(false);
        }
    }

    private void addBubble(String text, boolean fromCoach) {
        adapter.add(new CoachAdapter.Message(text, fromCoach));
        binding.listChat.scrollToPosition(adapter.getItemCount() - 1);
    }

    // ---------- speech to text ----------

    private void onMicClicked() {
        if (listening) {
            stopListening();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startListening() {
        if (waitingForCoach || finished) {
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onPartialResults(Bundle partialResults) {
                showSpeech(partialResults);
            }

            @Override
            public void onResults(Bundle results) {
                showSpeech(results);
                stopListening();
            }

            @Override
            public void onError(int error) {
                stopListening();
            }

            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizer.startListening(intent);

        listening = true;
        binding.textListening.setVisibility(View.VISIBLE);
        micPulse = AnimUtils.pulse(binding.btnMic); // A7
    }

    /** Puts the recognized words in the answer box, so the student can still edit them. */
    private void showSpeech(Bundle bundle) {
        ArrayList<String> words = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (words != null && !words.isEmpty()) {
            binding.editAnswer.setText(words.get(0));
            binding.editAnswer.setSelection(binding.editAnswer.getText().length());
        }
    }

    /** The recognizer holds the microphone, so it must always be released. */
    private void stopListening() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        listening = false;
        if (micPulse != null) {
            micPulse.cancel();
            micPulse = null;
        }
        binding.btnMic.setScaleX(1f);
        binding.btnMic.setScaleY(1f);
        binding.textListening.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListening();
    }
}
