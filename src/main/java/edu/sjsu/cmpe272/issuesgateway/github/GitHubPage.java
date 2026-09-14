package edu.sjsu.cmpe272.issuesgateway.github;

/*
 * Author: Shravani Naikoti
 * Contribution: Holder for a page of results and its pagination metadata
 */



import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;

public record GitHubPage(
        JsonNode body,
        HttpHeaders headers
) {
}