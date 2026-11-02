package com.example.cvforge.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.cvforge.data.model.Internship;
import com.example.cvforge.network.model.ArbeitnowResponse;
import com.example.cvforge.network.model.RemotiveResponse;

import org.junit.Test;

public class JobMapperTest {

    private static final long NOW = 1_000_000_000_000L;

    @Test
    public void remotiveJob_becomesRemoteInternship() {
        RemotiveResponse.Job job = new RemotiveResponse.Job();
        job.id = 42;
        job.title = "Backend Intern";
        job.companyName = "Mitre Media";
        job.description = "<p>Build <b>APIs</b></p>";
        job.url = "https://remotive.com/remote-jobs/42";
        job.publicationDate = "2026-10-14T20:33:44";
        job.tags.add("java");

        Internship post = JobMapper.fromRemotive(job, NOW);

        assertEquals("remotive_42", post.id);
        assertEquals(Internship.SOURCE_REMOTIVE, post.source);
        assertTrue(post.isExternal());
        assertEquals(Internship.TYPE_REMOTE, post.type);
        assertEquals("Build APIs", post.description);
        assertEquals("Anywhere", post.location);
        assertEquals(1, post.skills.size());
        assertTrue(post.deadline > NOW);
    }

    @Test
    public void arbeitnowJob_usesRemoteFlagAndSeconds() {
        ArbeitnowResponse.Job job = new ArbeitnowResponse.Job();
        job.slug = "dev-intern-1";
        job.title = "Dev Intern";
        job.companyName = "Acme";
        job.description = "&lt;p&gt;Hello&lt;/p&gt;";
        job.location = "Berlin";
        job.remote = false;
        job.createdAt = 1_700_000_000L;

        Internship post = JobMapper.fromArbeitnow(job, NOW);

        assertEquals("arbeitnow_dev-intern-1", post.id);
        assertEquals(Internship.TYPE_ONSITE, post.type);
        assertEquals("Hello", post.description);
        assertEquals(1_700_000_000_000L, post.createdAt);
    }

    @Test
    public void internshipWords_areFoundInTitles() {
        assertTrue(JobMapper.looksLikeInternship("Working Student Piping (m/f/d)"));
        assertTrue(JobMapper.looksLikeInternship("Praktikum Marketing"));
        assertFalse(JobMapper.looksLikeInternship("Senior Engineer"));
        assertFalse(JobMapper.looksLikeInternship(null));
    }
}
