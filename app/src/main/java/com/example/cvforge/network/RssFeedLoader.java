package com.example.cvforge.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Internship;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads the We Work Remotely job feed (RSS) and parses it with XmlPullParser.
 * The request and the parsing run on a background thread (an Executor). Nothing here can
 * crash the app: when the feed cannot be loaded, the saved sample file is parsed with the
 * same code, so the XML parsing always works.
 */
public class RssFeedLoader {

    public static final String FEED_URL = "https://weworkremotely.com/categories/remote-programming-jobs.rss";

    /** Sent with the request, because some websites refuse requests without a User-Agent. */
    private static final String USER_AGENT = "CVForge/1.0 (Android)";
    private static final int TIMEOUT_MS = 10_000;

    /** Callback. Runs on the main thread. */
    public interface Listener {
        /**
         * @param jobs     the jobs of the feed, or of the saved sample file
         * @param fromFeed false when the feed could not be loaded and the sample file was used
         */
        void onLoaded(List<Internship> jobs, boolean fromFeed);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Context context;

    public RssFeedLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    public void load(Listener listener) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            List<Internship> jobs;
            boolean fromFeed = true;
            try {
                jobs = download(now);
            } catch (Exception e) {
                // no internet, time out, a refused request or broken XML: use the saved sample
                fromFeed = false;
                jobs = readSample(now);
            }
            List<Internship> result = jobs;
            boolean ok = fromFeed;
            mainHandler.post(() -> listener.onLoaded(result, ok));
        });
    }

    /** Asks the website for the feed and parses the answer. */
    private List<Internship> download(long now) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(FEED_URL).openConnection();
        try {
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Feed answered with " + connection.getResponseCode());
            }
            try (InputStream input = connection.getInputStream()) {
                return toInternships(RssJobParser.parse(input), now);
            }
        } finally {
            connection.disconnect();
        }
    }

    /** Parses the copy of the feed that is saved inside the app (res/raw/sample_jobs.xml). */
    private List<Internship> readSample(long now) {
        try (InputStream input = context.getResources().openRawResource(R.raw.sample_jobs)) {
            return toInternships(RssJobParser.parse(input), now);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Internship> toInternships(List<RssJobParser.RssItem> items, long now) {
        List<Internship> jobs = new ArrayList<>();
        for (RssJobParser.RssItem item : items) {
            if (item.link != null && !item.link.trim().isEmpty()) {
                jobs.add(JobMapper.fromRss(item, now));
            }
        }
        return jobs;
    }
}
