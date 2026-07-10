package com.bricopro.verification;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CinFormatValidator {

    private static final Pattern CIN_PATTERN = Pattern.compile("\\b([A-Z]{1,2}[0-9]{5,6})\\b");

    private static final String[] MOROCCAN_ID_KEYWORDS = {
            "ROYAUME DU MAROC",
            "CARTE NATIONALE",
            "CARTE D'IDENTITE",
            "MOROCCO",
            "KINGDOM OF MOROCCO"
    };

    public static String extractCinNumber(String rawText) {
        if (rawText == null) return null;
        String normalized = rawText.toUpperCase().replaceAll("[^A-Z0-9\\s]", " ");
        Matcher matcher = CIN_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate.length() >= 6 && candidate.length() <= 8) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean looksLikeMoroccanId(String rawText) {
        if (rawText == null) return false;
        String normalized = rawText.toUpperCase();
        for (String keyword : MOROCCAN_ID_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}