package com.example.cvforge.network;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads an RSS 2.0 job feed with XmlPullParser (W11 XML parsing).
 * The parser walks through the XML one tag at a time. For every {@code <item>} it
 * collects title, region, category, description, date and link.
 */
public class RssJobParser {

    /** The text values of one {@code <item>} in the feed. */
    public static class RssItem {
        public String title = "";
        public String region = "";
        public String category = "";
        public String description = "";
        public String pubDate = "";
        public String link = "";
    }

    private RssJobParser() {
    }

    /** Reads all items from the feed. Throws when the XML is broken. */
    public static List<RssItem> parse(InputStream input) throws XmlPullParserException, IOException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(input, null);

        List<RssItem> items = new ArrayList<>();
        RssItem current = null;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("item".equals(tag)) {
                    current = new RssItem();
                } else if (current != null) {
                    readField(parser, tag, current);
                }
            } else if (event == XmlPullParser.END_TAG && "item".equals(parser.getName())) {
                if (current != null) {
                    items.add(current);
                }
                current = null;
            }
            event = parser.next();
        }
        return items;
    }

    /** Copies the text of one tag into the item. Tags we do not need are skipped. */
    private static void readField(XmlPullParser parser, String tag, RssItem item)
            throws XmlPullParserException, IOException {
        switch (tag) {
            case "title":
                item.title = parser.nextText();
                break;
            case "region":
                item.region = parser.nextText();
                break;
            case "category":
                item.category = parser.nextText();
                break;
            case "description":
                item.description = parser.nextText();
                break;
            case "pubDate":
                item.pubDate = parser.nextText();
                break;
            case "link":
                item.link = parser.nextText();
                break;
            default:
                break;
        }
    }
}
