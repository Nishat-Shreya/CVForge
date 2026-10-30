package com.example.cvforge.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class ScamCheckerTest {

    @Test
    public void normalPost_hasNoProblems() {
        List<ScamChecker.Problem> problems = ScamChecker.check("Android Intern",
                "Build features for our app with the team.", 20000, "company", "hr@nexalabs.com");
        assertTrue(problems.isEmpty());
    }

    @Test
    public void asksForMoney_isFlagged() {
        List<ScamChecker.Problem> problems = ScamChecker.check("Data Entry",
                "A small Registration Fee is needed to start.", 0, "company", "hr@nexalabs.com");
        assertEquals(1, problems.size());
        assertEquals(ScamChecker.Problem.PAYMENT_REQUEST, problems.get(0));
    }

    @Test
    public void whatsappOnly_isFlagged() {
        List<ScamChecker.Problem> problems = ScamChecker.check("Intern",
                "Please contact on WhatsApp only.", 0, "alumni", "a@b.com");
        assertTrue(problems.contains(ScamChecker.Problem.PERSONAL_CHAT));
    }

    @Test
    public void companyWithFreeEmail_isFlagged() {
        List<ScamChecker.Problem> problems = ScamChecker.check("Intern", "Work with us.", 0,
                "company", "boss@Gmail.com");
        assertTrue(problems.contains(ScamChecker.Problem.FREE_EMAIL));
    }

    @Test
    public void teacherWithFreeEmail_isNotFlagged() {
        List<ScamChecker.Problem> problems = ScamChecker.check("Intern", "Work with us.", 0,
                "teacher", "teacher@gmail.com");
        assertTrue(problems.isEmpty());
    }

    @Test
    public void veryHighStipend_isFlagged() {
        List<ScamChecker.Problem> problems = ScamChecker.check("Intern", "Work with us.",
                ScamChecker.HIGH_STIPEND + 1, "company", "hr@nexalabs.com");
        assertTrue(problems.contains(ScamChecker.Problem.HIGH_STIPEND));
    }

    @Test
    public void emptyText_doesNotCrash() {
        assertTrue(ScamChecker.check(null, null, 0, null, null).isEmpty());
    }
}
