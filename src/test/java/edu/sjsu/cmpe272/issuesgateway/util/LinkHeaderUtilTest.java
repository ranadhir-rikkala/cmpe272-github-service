package edu.sjsu.cmpe272.issuesgateway.util;

/*
 * Author: Shravani Naikoti
 * Contribution: Unit tests for Link header parsing
 */



import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkHeaderUtilTest {

    @Test
    void parsesNextAndLastLinks() {
        String header =
                "<https://api.github.com/repos/test/issues?page=2>; rel=\"next\", " +
                "<https://api.github.com/repos/test/issues?page=5>; rel=\"last\"";

        Map<String, String> result =
                LinkHeaderUtil.parse(header);

        assertEquals(
                "https://api.github.com/repos/test/issues?page=2",
                result.get("next")
        );

        assertEquals(
                "https://api.github.com/repos/test/issues?page=5",
                result.get("last")
        );
    }

    @Test
    void returnsEmptyMapForNullHeader() {
        Map<String, String> result =
                LinkHeaderUtil.parse(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyMapForBlankHeader() {
        Map<String, String> result =
                LinkHeaderUtil.parse("");

        assertTrue(result.isEmpty());
    }

    @Test
    void ignoresInvalidEntry() {
        Map<String, String> result =
                LinkHeaderUtil.parse("invalid-entry");

        assertTrue(result.isEmpty());
    }
}