package com.example.cvforge.util;

/** Turns the HTML text of a job post into plain text. */
public class TextCleaner {

    private TextCleaner() {
    }

    /**
     * Some APIs send HTML, and some send HTML that is itself escaped (&lt;p&gt;).
     * Decoding twice and removing the tags handles both. Returns at most maxLength characters.
     */
    public static String htmlToText(String html, int maxLength) {
        if (html == null) {
            return "";
        }
        String text = unescape(html);
        text = unescape(text);
        text = text.replaceAll("(?i)<br\\s*/?>|</p>|</li>|</h[1-6]>", "\n");
        text = text.replaceAll("<[^>]*>", "");
        text = text.replace(' ', ' ').replace(" ", " ");
        text = text.replaceAll("[ \t]+", " ").replaceAll(" ?\n ?", "\n").replaceAll("\n{3,}", "\n\n").trim();
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength).trim() + "…";
        }
        return text;
    }

    private static String unescape(String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ").replace("&amp;", "&");
    }
}
