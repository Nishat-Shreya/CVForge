package com.example.cvforge.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MatchScoreTest {

    private static final List<String> REQUIRED = Arrays.asList("Java", "SQL", "Firebase", "Git", "UI/UX");

    @Test
    public void percentage_isMatchedDividedByRequired() {
        MatchScore.Result result = MatchScore.compare(Arrays.asList("java", "GIT"), REQUIRED);

        assertEquals(40, result.percent);
        assertEquals(Arrays.asList("Java", "Git"), result.matched);
        assertEquals(Arrays.asList("SQL", "Firebase", "UI/UX"), result.missing);
    }

    @Test
    public void allSkills_giveHundredPercent() {
        assertEquals(100, MatchScore.compare(REQUIRED, REQUIRED).percent);
    }

    @Test
    public void noStudentSkills_giveZero() {
        MatchScore.Result result = MatchScore.compare(Collections.<String>emptyList(), REQUIRED);
        assertEquals(0, result.percent);
        assertEquals(5, result.missing.size());
    }

    @Test
    public void noRequiredSkills_giveZeroWithoutCrash() {
        assertEquals(0, MatchScore.compare(Arrays.asList("Java"), null).percent);
        assertEquals(0, MatchScore.compare(Arrays.asList("Java"), Collections.<String>emptyList()).percent);
    }

    @Test
    public void longerSkillNames_stillMatch() {
        MatchScore.Result result = MatchScore.compare(Arrays.asList("Android SDK"), Arrays.asList("Android"));
        assertEquals(100, result.percent);
    }

    @Test
    public void shortNames_needAnExactMatch() {
        // "C" must not match "C++" or "SQL" just because the letter is inside
        MatchScore.Result result = MatchScore.compare(Arrays.asList("C"), Arrays.asList("C++"));
        assertEquals(0, result.percent);
    }

    @Test
    public void learningOneMoreSkill_raisesThePercent() {
        MatchScore.Result result = MatchScore.compare(Arrays.asList("Java", "Git"), REQUIRED);
        assertEquals(60, MatchScore.percentWithOneMore(result));
        assertTrue(MatchScore.percentWithOneMore(result) > result.percent);
    }
}
