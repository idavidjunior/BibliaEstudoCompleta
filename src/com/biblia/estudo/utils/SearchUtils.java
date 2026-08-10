package com.biblia.estudo.utils;

import java.text.Normalizer;
import java.util.Locale;

public class SearchUtils {

    private SearchUtils() {}

    public static String normalize(String text) {
        if (text == null) return "";
        String s = Normalizer.normalize(text, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return s;
    }

    public static String wordMatch(String query) {
        String normalized = normalize(query);
        String[] tokens = normalized.split("[^\\p{L}\\p{N}]+");
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        return sb.toString();
    }

    public static String phraseMatch(String query) {
        String normalized = normalize(query).replace("\"", " ").trim();
        if (normalized.isEmpty()) return "";
        return "\"" + normalized + "\"";
    }
}
