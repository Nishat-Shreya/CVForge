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

    /** Rewrites headline, summary and skill order of a CV for one internship (feature 9). */
    public static String tailorCv(String cvJson, String jobTitle, String jobDescription, String jobSkills) {
        return "You adapt a student's CV for one internship. Here is the CV as JSON:\n" + cvJson
                + "\n\nInternship: " + jobTitle + "\nRequired skills: " + jobSkills
                + "\nDescription: " + jobDescription + "\n\n"
                + "Return ONLY a JSON object with these keys: "
                + "\"headline\" (string, max 8 words), \"summary\" (string, max 50 words), "
                + "\"skills\" (array of strings).\n"
                + "Rules:\n"
                + "- Use only facts that are in the CV. Do not invent skills, experience, numbers or degrees.\n"
                + "- \"skills\" must only contain skills from the CV skills, with the ones that fit this "
                + "internship first.\n"
                + "- Do not use markdown and do not add any text outside the JSON.";
    }

    /** A short learning plan for the skills a student is missing for an internship (feature 11). */
    public static String roadmap(String jobTitle, String missingSkills) {
        return "A student wants the internship \"" + jobTitle + "\" but does not have these skills yet: "
                + missingSkills + ".\n"
                + "Write a learning roadmap with one step for each skill, at most 5 steps.\n"
                + "Rules:\n"
                + "- Each step is one line in this format: Skill | one concrete thing to do | time needed\n"
                + "- Example: SQL | Build a small library database and write 10 queries | 1 week\n"
                + "- Use only free, beginner-friendly practice. Keep each line under 25 words.\n"
                + "- Return only the lines, without numbers, dashes or extra text.";
    }

    /**
     * One turn of the interview coach (feature 12). The chat so far is in "transcript".
     * On the last turn the coach gives a short summary instead of a new question.
     */
    public static String interviewTurn(String role, String company, String transcript, boolean first, boolean last) {
        String task;
        if (first) {
            task = "Start the interview. Ask the first question only.";
        } else if (last) {
            task = "Give short feedback on the last answer, then a final summary: two strengths and "
                    + "two things to improve. Do not ask another question.";
        } else {
            task = "Give 1 or 2 sentences of kind, concrete feedback on the last answer, then ask the "
                    + "next question.";
        }
        return "You are a friendly interview coach. You practise an interview with a student for the "
                + "internship \"" + role + "\" at " + company + ".\n"
                + (transcript.isEmpty() ? "" : "Chat so far:\n" + transcript + "\n")
                + task + "\n"
                + "Rules: plain text only, no markdown, maximum 70 words. Mix a question about motivation, "
                + "a technical or role question and a teamwork question across the interview.";
    }

    /** Second opinion on a post that the rule checks flagged (feature 14). */
    public static String scamReview(String title, String description, String poster, String posterRole,
                                    int stipend, String ruleReasons) {
        return "You help an admin of a student internship app to find fake internship posts.\n"
                + "Post title: " + title + "\nPosted by: " + poster + " (" + posterRole + ")\n"
                + "Monthly stipend: " + stipend + "\nDescription: " + description + "\n"
                + "The simple rule checks found: " + ruleReasons + "\n\n"
                + "Is this post likely a scam (asks for money, moves chat to WhatsApp, promises too much)?\n"
                + "Answer in exactly two lines:\n"
                + "VERDICT: SAFE or SUSPICIOUS or SCAM\n"
                + "REASON: one short sentence (max 25 words)\n"
                + "Be careful: the rule checks can be wrong, so judge the text yourself.";
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
