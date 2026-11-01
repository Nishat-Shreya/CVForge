package com.example.cvforge.network;

import com.example.cvforge.data.model.Internship;
import com.example.cvforge.network.model.ArbeitnowResponse;
import com.example.cvforge.network.model.RemotiveResponse;
import com.example.cvforge.util.TextCleaner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Turns jobs from the web APIs into the Internship objects the app already shows. */
public class JobMapper {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final int MAX_DESCRIPTION = 800;
    private static final int MAX_SKILLS = 6;

    /** Arbeitnow has all kinds of jobs. Only titles with one of these words are kept. */
    private static final List<String> INTERNSHIP_WORDS = Arrays.asList(
            "intern", "trainee", "working student", "werkstudent", "praktik", "junior", "apprentice");

    private JobMapper() {
    }

    public static Internship fromRemotive(RemotiveResponse.Job job, long now) {
        Internship post = new Internship();
        post.id = Internship.SOURCE_REMOTIVE + "_" + job.id;
        post.source = Internship.SOURCE_REMOTIVE;
        post.posterName = job.companyName;
        post.title = job.title;
        post.description = TextCleaner.htmlToText(job.description, MAX_DESCRIPTION);
        post.skills = firstOf(job.tags);
        post.location = job.location == null || job.location.isEmpty() ? "Anywhere" : job.location;
        post.type = Internship.TYPE_REMOTE;
        post.url = job.url;
        post.createdAt = parseDate(job.publicationDate, now);
        // jobs from other websites have no deadline, so the list keeps them for 30 days
        post.deadline = now + 30 * DAY_MS;
        return post;
    }

    public static Internship fromArbeitnow(ArbeitnowResponse.Job job, long now) {
        Internship post = new Internship();
        post.id = Internship.SOURCE_ARBEITNOW + "_" + job.slug;
        post.source = Internship.SOURCE_ARBEITNOW;
        post.posterName = job.companyName;
        post.title = job.title;
        post.description = TextCleaner.htmlToText(job.description, MAX_DESCRIPTION);
        post.skills = firstOf(job.tags);
        post.location = job.location == null || job.location.isEmpty() ? "Anywhere" : job.location;
        post.type = job.remote ? Internship.TYPE_REMOTE : Internship.TYPE_ONSITE;
        post.url = job.url;
        post.createdAt = job.createdAt > 0 ? job.createdAt * 1000 : now;
        post.deadline = now + 30 * DAY_MS;
        return post;
    }

    /**
     * A job from the We Work Remotely RSS feed. The feed title is "Company: Job title".
     * The id comes from the last part of the job link.
     */
    public static Internship fromRss(RssJobParser.RssItem item, long now) {
        Internship post = new Internship();
        String link = item.link == null ? "" : item.link.trim();
        post.id = Internship.SOURCE_WWR + "_" + link.substring(link.lastIndexOf('/') + 1);
        post.source = Internship.SOURCE_WWR;

        String title = item.title == null ? "" : item.title.trim();
        int colon = title.indexOf(": ");
        post.posterName = colon > 0 ? title.substring(0, colon) : "We Work Remotely";
        post.title = colon > 0 ? title.substring(colon + 2) : title;

        post.description = TextCleaner.htmlToText(item.description, MAX_DESCRIPTION);
        post.skills = new java.util.ArrayList<>();
        if (item.category != null && !item.category.trim().isEmpty()) {
            post.skills.add(item.category.trim());
        }
        post.location = item.region == null || item.region.trim().isEmpty() ? "Anywhere" : item.region.trim();
        post.type = Internship.TYPE_REMOTE;
        post.url = link;
        post.createdAt = parseRssDate(item.pubDate, now);
        post.deadline = now + 30 * DAY_MS;
        return post;
    }

    private static long parseRssDate(String text, long fallback) {
        try {
            return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(text.trim()).getTime();
        } catch (ParseException | RuntimeException e) {
            return fallback;
        }
    }

    /** True when the title looks like an internship or a first job. */
    public static boolean looksLikeInternship(String title) {
        if (title == null) {
            return false;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        for (String word : INTERNSHIP_WORDS) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> firstOf(List<String> tags) {
        if (tags == null) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(tags.subList(0, Math.min(tags.size(), MAX_SKILLS)));
    }

    private static long parseDate(String text, long fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(text).getTime();
        } catch (ParseException e) {
            return fallback;
        }
    }
}
