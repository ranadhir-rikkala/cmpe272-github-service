package edu.sjsu.cmpe272.issuesgateway.github;

/*
 * Author: Shivani Naikoti
 * Contribution: GitHub REST API client using Spring RestClient.
 */

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GitHubClient {

    private final RestClient restClient;
    private final GitHubProperties properties;

    public GitHubClient(
            RestClient.Builder restClientBuilder,
            GitHubProperties properties
    ) {
        this.properties = properties;

        this.restClient = restClientBuilder
                .baseUrl(properties.getApiUrl())
                .requestFactory(new JdkClientHttpRequestFactory())
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(properties.getToken());
                    headers.set(
                            HttpHeaders.ACCEPT,
                            "application/vnd.github+json"
                    );
                    headers.set(
                            "X-GitHub-Api-Version",
                            "2022-11-28"
                    );
                })
                .build();
    }

    public JsonNode createIssue(
            String title,
            String body,
            List<String> labels
    ) {
        Map<String, Object> request = new LinkedHashMap<>();

        request.put("title", title);

        if (body != null) {
            request.put("body", body);
        }

        if (labels != null) {
            request.put("labels", labels);
        }

        return restClient.post()
                .uri(
                        "/repos/{owner}/{repo}/issues",
                        properties.getOwner(),
                        properties.getRepo()
                )
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        (requestObject, response) -> handleError(response)
                )
                .body(JsonNode.class);
    }

    public GitHubPage listIssues(
            String state,
            String labels,
            int page,
            int perPage
    ) {
        ResponseEntity<JsonNode> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues")
                        .queryParam("state", state)
                        .queryParam("page", page)
                        .queryParam("per_page", perPage)
                        .queryParamIfPresent(
                                "labels",
                                labels == null || labels.isBlank()
                                        ? Optional.empty()
                                        : Optional.of(labels)
                        )
                        .build(
                                properties.getOwner(),
                                properties.getRepo()
                        ))
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        (requestObject, responseObject) ->
                                handleError(responseObject)
                )
                .toEntity(JsonNode.class);

        return new GitHubPage(
                response.getBody(),
                response.getHeaders()
        );
    }

    public JsonNode getIssue(int number) {
        return restClient.get()
                .uri(
                        "/repos/{owner}/{repo}/issues/{number}",
                        properties.getOwner(),
                        properties.getRepo(),
                        number
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        (requestObject, response) ->
                                handleError(response)
                )
                .body(JsonNode.class);
    }

    public JsonNode updateIssue(
            int number,
            String title,
            String body,
            String state
    ) {
        Map<String, Object> request = new LinkedHashMap<>();

        if (title != null) {
            request.put("title", title);
        }

        if (body != null) {
            request.put("body", body);
        }

        if (state != null) {
            request.put("state", state);
        }

        return restClient.patch()
                .uri(
                        "/repos/{owner}/{repo}/issues/{number}",
                        properties.getOwner(),
                        properties.getRepo(),
                        number
                )
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        (requestObject, response) ->
                                handleError(response)
                )
                .body(JsonNode.class);
    }

    public JsonNode createComment(
            int number,
            String body
    ) {
        Map<String, String> request =
                Map.of("body", body);

        return restClient.post()
                .uri(
                        "/repos/{owner}/{repo}/issues/{number}/comments",
                        properties.getOwner(),
                        properties.getRepo(),
                        number
                )
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        (requestObject, response) ->
                                handleError(response)
                )
                .body(JsonNode.class);
    }

    public List<JsonNode> listComments(int number) {
        JsonNode body = restClient.get()
                .uri(
                        "/repos/{owner}/{repo}/issues/{number}/comments",
                        properties.getOwner(),
                        properties.getRepo(),
                        number
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        (requestObject, response) ->
                                handleError(response)
                )
                .body(JsonNode.class);

        List<JsonNode> comments = new ArrayList<>();

        if (body != null && body.isArray()) {
            body.forEach(comments::add);
        }

        return comments;
    }

    private void handleError(
            org.springframework.http.client.ClientHttpResponse response
    ) throws java.io.IOException {

        int status = response.getStatusCode().value();

        String retryAfter =
                response.getHeaders().getFirst("Retry-After");

        String remaining =
                response.getHeaders().getFirst("X-RateLimit-Remaining");

        String message;

        if (status == 401) {
            message = "GitHub authentication failed";
        } else if (status == 403 && "0".equals(remaining)) {
            message = "GitHub API rate limit exceeded";
            status = 429;
        } else if (status == 403) {
            message = "GitHub access forbidden";
        } else if (status == 404) {
            message = "GitHub resource was not found";
        } else if (status == 429) {
            message = "GitHub API rate limit exceeded";
        } else if (status >= 500) {
            message = "GitHub service is temporarily unavailable";
            status = 503;
        } else {
            message = "GitHub API request failed";
        }

        throw new GitHubApiException(
                status,
                message,
                retryAfter
        );
    }
}