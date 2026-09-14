package edu.sjsu.cmpe272.issuesgateway.util;



import java.util.LinkedHashMap;
import java.util.Map;

public final class LinkHeaderUtil {

    private LinkHeaderUtil() {
        // Utility class should not be instantiated.
    }

    public static Map<String, String> parse(String linkHeader) {
        Map<String, String> links = new LinkedHashMap<>();

        if (linkHeader == null || linkHeader.isBlank()) {
            return links;
        }

        String[] entries = linkHeader.split(",");

        for (String entry : entries) {
            String[] parts = entry.trim().split(";");

            if (parts.length < 2) {
                continue;
            }

            String url = parts[0].trim();

            if (url.startsWith("<") && url.endsWith(">")) {
                url = url.substring(1, url.length() - 1);
            }

            for (int index = 1; index < parts.length; index++) {
                String relation = parts[index].trim();

                if (relation.startsWith("rel=")) {
                    String rel = relation
                            .substring(4)
                            .replace("\"", "")
                            .trim();

                    links.put(rel, url);
                }
            }
        }

        return links;
    }
}