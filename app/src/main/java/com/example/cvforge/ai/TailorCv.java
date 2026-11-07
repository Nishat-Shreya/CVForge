package com.example.cvforge.ai;

import android.content.Context;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.util.ListText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Tailor my CV" (feature 9): Gemini adapts the headline, the summary and the skill order
 * of a CV to one internship. The result is a NEW version of the CV, the original is not
 * changed. Facts are never added: skills that are not in the original CV are dropped.
 */
public class TailorCv {

    public interface Callback {
        void onTailored(Cv tailored);

        void onError(String message);
    }

    private final AiRepository ai;

    public TailorCv(Context context) {
        ai = new AiRepository(context);
    }

    public void tailor(Cv original, Internship job, Callback callback) {
        String skills = job.skills == null ? "" : ListText.join(job.skills, ", ");
        String prompt = Prompts.tailorCv(cvAsJson(original), job.title, job.description, skills);

        ai.generate(prompt, new AiRepository.Callback() {
            @Override
            public void onResult(String text) {
                try {
                    callback.onTailored(buildVersion(original, job, text));
                } catch (Exception e) {
                    callback.onError("The AI answer could not be read.");
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Only the parts the AI needs. Contact details are not sent. */
    private String cvAsJson(Cv cv) {
        JsonObject json = new JsonObject();
        json.addProperty("headline", cv.headline);
        json.addProperty("summary", cv.summary);
        Gson gson = new Gson();
        json.add("skills", gson.toJsonTree(cv.skills));
        json.add("education", gson.toJsonTree(cv.education));
        json.add("projects", gson.toJsonTree(cv.projects));
        json.add("experience", gson.toJsonTree(cv.experience));
        return json.toString();
    }

    /** Copies the original CV and puts the AI text into the copy. */
    private Cv buildVersion(Cv original, Internship job, String answer) {
        JsonObject json = new Gson().fromJson(extractJson(answer), JsonObject.class);

        Cv copy = new Gson().fromJson(new Gson().toJson(original), Cv.class);
        copy.id = UUID.randomUUID().toString();
        copy.title = "Tailored: " + job.title;
        copy.updatedAt = System.currentTimeMillis();

        if (json.has("headline") && !json.get("headline").getAsString().trim().isEmpty()) {
            copy.headline = json.get("headline").getAsString().trim();
        }
        if (json.has("summary") && !json.get("summary").getAsString().trim().isEmpty()) {
            copy.summary = json.get("summary").getAsString().trim();
        }
        if (json.has("skills")) {
            copy.skills = orderedSkills(original.skills, json.getAsJsonArray("skills"));
        }
        return copy;
    }

    /** Keeps only skills that the student really has, in the order the AI chose. */
    private List<String> orderedSkills(List<String> original, com.google.gson.JsonArray suggested) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < suggested.size(); i++) {
            String skill = suggested.get(i).getAsString().trim();
            String match = findIgnoreCase(original, skill);
            if (match != null && !result.contains(match)) {
                result.add(match);
            }
        }
        // skills the AI left out stay at the end
        for (String skill : original) {
            if (!result.contains(skill)) {
                result.add(skill);
            }
        }
        return result;
    }

    private String findIgnoreCase(List<String> list, String value) {
        for (String item : list) {
            if (item.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    /** The AI sometimes wraps the JSON in text or code fences, so cut out the braces. */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }
}
