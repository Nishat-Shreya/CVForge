package com.example.cvforge.ai;

/**
 * The texts that are sent to Gemini. Every prompt tells the AI not to invent facts, because
 * the result goes into a message or a CV that the student sends under their own name.
 * The other AI features add their prompts here in later checkpoints.
 */
public class Prompts {

    private Prompts() {
    }

    /** Three CV bullet points from a plain description of the work (feature 10). */
    public static String bulletPoints(String role, String work) {
        return "Write 3 CV bullet points in English for a student who worked as \"" + role
                + "\". What the student did, in their own words: \"" + work + "\".\n"
                + "Rules:\n"
                + "- Each bullet starts with a strong action verb and is at most 20 words.\n"
                + "- Use only facts from the description. Do not invent numbers, tools or results.\n"
                + "- Return exactly 3 lines, one bullet per line, without dashes, numbers or extra text.";
    }

    /** A short follow-up email for an application that got no reply for a week (feature 13). */
    public static String followUpEmail(String studentName, String company, String jobTitle) {
        return "Write a short, polite follow-up email in English from a student named " + studentName
                + " to the company " + company + ". The student applied for the internship \""
                + jobTitle + "\" a week ago and has not received a reply.\n"
                + "Rules:\n"
                + "- Maximum 90 words, friendly and professional.\n"
                + "- Say that the student is still interested and ask politely about the status.\n"
                + "- Do not invent any facts about the student or the company.\n"
                + "- Start with \"Dear " + company + " team,\" and end with \"Best regards,\" and the name.\n"
                + "- Return only the email text, without a subject line and without extra comments.";
    }
}
