package com.example.cvforge.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.cvforge.data.model.Internship;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RssJobParserTest {

    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void sampleFeed_hasEightJobsWithAllFields() throws Exception {
        try (InputStream input = new FileInputStream("src/main/res/raw/sample_jobs.xml")) {
            List<RssJobParser.RssItem> items = RssJobParser.parse(input);

            assertEquals(8, items.size());
            for (RssJobParser.RssItem item : items) {
                assertFalse(item.title.isEmpty());
                assertTrue(item.link.startsWith("https://weworkremotely.com/remote-jobs/"));
                assertFalse(item.description.isEmpty());
            }
        }
    }

    @Test
    public void rssItem_becomesAnInternshipWithLinkBack() throws Exception {
        String xml = "<rss><channel><title>Feed</title><item>"
                + "<title>Acme: Junior Android Developer</title><region>Anywhere</region>"
                + "<category>Mobile</category>"
                + "<description>&lt;p&gt;Build &lt;b&gt;apps&lt;/b&gt;&lt;/p&gt;</description>"
                + "<pubDate>Thu, 01 Oct 2026 07:30:56 +0000</pubDate>"
                + "<link>https://weworkremotely.com/remote-jobs/acme-junior-android</link>"
                + "</item></channel></rss>";

        List<RssJobParser.RssItem> items = RssJobParser.parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Internship post = JobMapper.fromRss(items.get(0), NOW);

        assertEquals("wwr_acme-junior-android", post.id);
        assertEquals("Acme", post.posterName);
        assertEquals("Junior Android Developer", post.title);
        assertEquals("Build apps", post.description);
        assertEquals("https://weworkremotely.com/remote-jobs/acme-junior-android", post.url);
        assertEquals(Internship.SOURCE_WWR, post.source);
        assertTrue(post.isExternal());
        assertTrue(Internship.isExternalId(post.id));
    }

    @Test(expected = Exception.class)
    public void brokenXml_throwsInsteadOfCrashing() throws Exception {
        RssJobParser.parse(new ByteArrayInputStream("<rss><item><title>x".getBytes(StandardCharsets.UTF_8)));
    }
}
