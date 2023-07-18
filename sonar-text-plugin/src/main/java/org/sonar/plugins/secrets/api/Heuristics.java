package org.sonar.plugins.secrets.api;

import java.util.List;
import java.util.regex.Pattern;

public class Heuristics {
    private Heuristics() {}

    private static final Pattern uriPattern = Pattern.compile("^(https?|ftps?|file|smtp|imap)://.*$");

    public static boolean matchesHeuristics(String candidateSecret, List<String> heuristics) {
        return heuristics.stream().anyMatch(h -> {
            switch (h) {
                case "path": return isPath(candidateSecret);
                case "uri": return isUri(candidateSecret);
                default: return false;
            }
        });
    }

    public static boolean isPath(String input) {
        long fileSeparatorCount = input.chars().filter(c -> c == '/' || c == '\\').count();
        return fileSeparatorCount >= 2 && fileSeparatorCount * 1. / input.length() > 0.15;
    }

    public static boolean isUri(String input) {
        return uriPattern.matcher(input).matches();
    }
}
