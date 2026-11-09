package com.example.cvforge.ai;

import android.content.Context;

import com.example.cvforge.data.model.Internship;
import com.example.cvforge.util.ListText;

import java.util.Locale;

/**
 * AI second opinion for a flagged internship post (feature 14, part 2). The admin asks for it
 * from the admin screen. It is only advice: the admin decides to keep or remove the post.
 */
public class ScamReview {

    /** Result of the review. */
    public static class Result {
        /** SAFE, SUSPICIOUS or SCAM. */
        public final String verdict;
        public final String reason;

        Result(String verdict, String reason) {
            this.verdict = verdict;
            this.reason = reason;
        }
    }

    public interface Callback {
        void onResult(Result result);

        void onError(String message);
    }

    private final AiRepository ai;

    public ScamReview(Context context) {
        ai = new AiRepository(context);
    }

    public void review(Internship post, Callback callback) {
        String reasons = post.flagReasons == null ? "" : ListText.join(post.flagReasons, "; ");
        String prompt = Prompts.scamReview(post.title, post.description, post.posterName,
                post.posterRole, post.stipend, reasons);

        ai.generate(prompt, new AiRepository.Callback() {
            @Override
            public void onResult(String text) {
                callback.onResult(parse(text));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Reads "VERDICT: ..." and "REASON: ...". If the answer has another shape, it is SUSPICIOUS. */
    static Result parse(String answer) {
        String verdict = "SUSPICIOUS";
        String reason = answer;
        for (String line : answer.split("\n")) {
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.startsWith("VERDICT:")) {
                String value = upper.substring("VERDICT:".length()).trim();
                if (value.startsWith("SAFE")) {
                    verdict = "SAFE";
                } else if (value.startsWith("SCAM")) {
                    verdict = "SCAM";
                }
            } else if (upper.startsWith("REASON:")) {
                reason = line.substring("REASON:".length()).trim();
            }
        }
        return new Result(verdict, reason);
    }
}
