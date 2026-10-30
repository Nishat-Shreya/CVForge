package com.example.cvforge.util;

import com.example.cvforge.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Simple rule checks that find suspicious internship posts (feature 14).
 * No AI is used here. An optional AI second opinion is added in checkpoint 55.
 */
public class ScamChecker {

    /** One thing that makes a post look suspicious, with the message shown to the poster. */
    public enum Problem {
        PAYMENT_REQUEST(R.string.scam_payment_request),
        PERSONAL_CHAT(R.string.scam_personal_chat),
        FREE_EMAIL(R.string.scam_free_email),
        HIGH_STIPEND(R.string.scam_high_stipend);

        public final int messageRes;

        Problem(int messageRes) {
            this.messageRes = messageRes;
        }
    }

    /** Words that ask the student for money. */
    private static final List<String> PAYMENT_WORDS = Arrays.asList(
            "registration fee", "security deposit", "refundable deposit", "processing fee",
            "training fee", "joining fee", "application fee", "send money", "pay to",
            "gift card", "western union", "bitcoin");

    /** Words that move the talk to a personal chat app. */
    private static final List<String> CHAT_WORDS = Arrays.asList(
            "whatsapp only", "contact on whatsapp", "contact us on whatsapp", "telegram only",
            "message on whatsapp", "dm on telegram", "whatsapp me");

    /** A company that uses one of these addresses is not using a company email. */
    private static final List<String> FREE_EMAIL_DOMAINS = Arrays.asList(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "live.com", "icloud.com");

    /** A monthly stipend above this is unusual for an internship. */
    public static final int HIGH_STIPEND = 100000;

    private ScamChecker() {
    }

    /**
     * Checks one post.
     *
     * @param posterRole  company, teacher or alumni
     * @param posterEmail the email the poster signed up with
     * @return the problems found. An empty list means the post looks normal.
     */
    public static List<Problem> check(String title, String description, int stipend,
                                      String posterRole, String posterEmail) {
        List<Problem> problems = new ArrayList<>();
        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);

        if (containsAny(text, PAYMENT_WORDS)) {
            problems.add(Problem.PAYMENT_REQUEST);
        }
        if (containsAny(text, CHAT_WORDS)) {
            problems.add(Problem.PERSONAL_CHAT);
        }
        if ("company".equals(posterRole) && usesFreeEmail(posterEmail)) {
            problems.add(Problem.FREE_EMAIL);
        }
        if (stipend > HIGH_STIPEND) {
            problems.add(Problem.HIGH_STIPEND);
        }
        return problems;
    }

    private static boolean containsAny(String text, List<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean usesFreeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        return FREE_EMAIL_DOMAINS.contains(domain);
    }
}
