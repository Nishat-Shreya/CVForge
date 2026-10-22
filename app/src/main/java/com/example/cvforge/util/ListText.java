package com.example.cvforge.util;

import java.util.ArrayList;
import java.util.List;

/** Turns a list into one line of text and back, for the CV form fields. */
public class ListText {

    private ListText() {
    }

    /** "Java, SQL, Firebase" becomes [Java, SQL, Firebase]. Empty parts are dropped. */
    public static List<String> split(String text, String separator) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        for (String part : text.split(separator)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public static String join(List<String> items, String separator) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(items.get(i));
        }
        return builder.toString();
    }
}
