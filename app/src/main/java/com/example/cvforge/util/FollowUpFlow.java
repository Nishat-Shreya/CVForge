package com.example.cvforge.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.cvforge.R;
import com.example.cvforge.ai.AiRepository;
import com.example.cvforge.ai.Prompts;

/**
 * "Draft follow-up": asks Gemini for a polite email and opens the email app with it.
 * The email app shows the text, so the student can edit it before sending.
 * If the AI cannot answer (no internet, AI Logic not turned on), the ready template is used.
 */
public class FollowUpFlow {

    private FollowUpFlow() {
    }

    public static void start(Context context, String studentName, String company, String jobTitle) {
        AlertDialog waiting = new AlertDialog.Builder(context)
                .setMessage(R.string.ai_writing)
                .setCancelable(false)
                .show();

        new AiRepository(context).generate(Prompts.followUpEmail(studentName, company, jobTitle),
                new AiRepository.Callback() {
                    @Override
                    public void onResult(String text) {
                        waiting.dismiss();
                        openEmail(context, FollowUpEmail.intent(context, studentName, company, jobTitle, text));
                    }

                    @Override
                    public void onError(String message) {
                        waiting.dismiss();
                        Toast.makeText(context, context.getString(R.string.ai_error_with_template, message),
                                Toast.LENGTH_LONG).show();
                        openEmail(context, FollowUpEmail.intent(context, studentName, company, jobTitle));
                    }
                });
    }

    private static void openEmail(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_email_app, Toast.LENGTH_SHORT).show();
        }
    }
}
