package com.example.cvforge.ui.discover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.cvforge.data.model.Internship;

import org.junit.Before;
import org.junit.Test;

public class InternshipFilterTest {

    private static final long NOW = 1_000_000_000_000L;
    private static final long DAY = 24L * 60 * 60 * 1000;

    private Internship post;
    private InternshipFilter filter;

    @Before
    public void setUp() {
        post = new Internship();
        post.title = "Android Intern";
        post.posterName = "Nexa Labs";
        post.description = "Build features for our app.";
        post.skills.add("Java");
        post.skills.add("Firebase");
        post.location = "Dhaka";
        post.type = Internship.TYPE_HYBRID;
        post.stipend = 15000;
        post.deadline = NOW + 3 * DAY;
        filter = new InternshipFilter();
    }

    @Test
    public void emptyFilter_matchesEverything() {
        assertTrue(filter.isEmpty());
        assertTrue(filter.matches(post, NOW));
    }

    @Test
    public void search_looksInTitleCompanyAndSkills() {
        filter.query = "nexa";
        assertTrue(filter.matches(post, NOW));
        filter.query = "FIREBASE";
        assertTrue(filter.matches(post, NOW));
        filter.query = "python";
        assertFalse(filter.matches(post, NOW));
    }

    @Test
    public void skill_mustBeOneOfTheRequiredSkills() {
        filter.skill = "java";
        assertTrue(filter.matches(post, NOW));
        filter.skill = "kotlin";
        assertFalse(filter.matches(post, NOW));
    }

    @Test
    public void location_typeAndPaid() {
        filter.location = "dha";
        filter.type = Internship.TYPE_HYBRID;
        filter.paidOnly = true;
        assertTrue(filter.matches(post, NOW));

        filter.type = Internship.TYPE_REMOTE;
        assertFalse(filter.matches(post, NOW));

        filter.type = "";
        post.stipend = 0;
        assertFalse(filter.matches(post, NOW));
    }

    @Test
    public void deadline_onlyThoseClosingSoon() {
        filter.deadlineDays = 7;
        assertTrue(filter.matches(post, NOW));
        post.deadline = NOW + 20 * DAY;
        assertFalse(filter.matches(post, NOW));
    }

    @Test
    public void sheetFilterCount_countsOnlySheetFilters() {
        filter.query = "android";
        assertEquals(0, filter.sheetFilterCount());
        filter.skill = "java";
        filter.paidOnly = true;
        assertEquals(2, filter.sheetFilterCount());
    }
}
