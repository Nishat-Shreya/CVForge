package com.example.cvforge.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextCleanerTest {

    @Test
    public void removesTagsAndKeepsParagraphs() {
        assertEquals("One\nTwo", TextCleaner.htmlToText("<p>One</p><p>Two</p>", 100));
    }

    @Test
    public void decodesEscapedHtml() {
        assertEquals("Fish & Chips", TextCleaner.htmlToText("&lt;p&gt;Fish &amp;amp; Chips&lt;/p&gt;", 100));
    }

    @Test
    public void cutsLongText() {
        String text = TextCleaner.htmlToText("<p>abcdefghij</p>", 5);
        assertTrue(text.startsWith("abcde"));
        assertTrue(text.endsWith("…"));
    }

    @Test
    public void nullGivesEmptyText() {
        assertEquals("", TextCleaner.htmlToText(null, 10));
    }
}
