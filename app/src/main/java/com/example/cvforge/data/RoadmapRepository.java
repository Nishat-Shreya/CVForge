package com.example.cvforge.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cvforge.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves the skill gap roadmap of one student for one internship on the phone
 * (SharedPreferences, as JSON), so the ticked steps are still there next time.
 */
public class RoadmapRepository {

    /** One learning step. */
    public static class Step {
        public String skill;
        public String task;
        public String time;
        public boolean done;
        /** True for the simple plan that the app makes itself when the AI does not answer. */
        public boolean builtIn;
    }

    private static final String PREFS = "roadmaps";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public RoadmapRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The saved steps, or an empty list when there is no roadmap yet. */
    public List<Step> load(String uid, String internshipId) {
        String json = prefs.getString(key(uid, internshipId), null);
        if (json == null) {
            return new ArrayList<>();
        }
        List<Step> steps = gson.fromJson(json, new TypeToken<List<Step>>() { }.getType());
        return steps == null ? new ArrayList<>() : steps;
    }

    public void save(String uid, String internshipId, List<Step> steps) {
        prefs.edit().putString(key(uid, internshipId), gson.toJson(steps)).apply();
    }

    public void delete(String uid, String internshipId) {
        prefs.edit().remove(key(uid, internshipId)).apply();
    }

    /**
     * Turns the AI answer into steps. Every line looks like "Skill | what to do | time".
     * Lines that do not fit are skipped.
     */
    public static List<Step> parse(String answer) {
        List<Step> steps = new ArrayList<>();
        for (String line : answer.split("\n")) {
            String[] parts = line.replaceFirst("^[-*•\\d.\\s]+", "").split("\\|");
            if (parts.length >= 2) {
                Step step = new Step();
                step.skill = parts[0].trim();
                step.task = parts[1].trim();
                step.time = parts.length > 2 ? parts[2].trim() : "";
                if (!step.skill.isEmpty() && !step.task.isEmpty()) {
                    steps.add(step);
                }
            }
        }
        return steps;
    }

    /**
     * A simple plan without the AI: one step for each missing skill. It is used when Gemini does
     * not answer, so the roadmap screen is never empty.
     */
    public static List<Step> builtIn(Context context, List<String> missingSkills) {
        List<Step> steps = new ArrayList<>();
        for (String skill : missingSkills) {
            Step step = new Step();
            step.skill = skill;
            step.task = context.getString(R.string.roadmap_builtin_task, skill);
            step.time = context.getString(R.string.roadmap_builtin_time);
            step.builtIn = true;
            steps.add(step);
        }
        return steps;
    }

    private String key(String uid, String internshipId) {
        return uid + "_" + internshipId;
    }
}
