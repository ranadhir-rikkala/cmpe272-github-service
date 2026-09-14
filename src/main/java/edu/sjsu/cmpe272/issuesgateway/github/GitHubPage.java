package edu.sjsu.cmpe272.issuesgateway.github;



import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;

public record GitHubPage(
        JsonNode body,
        HttpHeaders headers
) {
}