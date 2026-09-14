package edu.sjsu.cmpe272.issuesgateway.service;

/*
 * Author: Shivani Naikoti
 * Contribution: GitHub-backed implementation of IssueService.
 */

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Primary;
import edu.sjsu.cmpe272.issuesgateway.dto.CommentResponse;
import edu.sjsu.cmpe272.issuesgateway.dto.CreateCommentRequest;
import edu.sjsu.cmpe272.issuesgateway.dto.CreateIssueRequest;
import edu.sjsu.cmpe272.issuesgateway.dto.IssueResponse;
import edu.sjsu.cmpe272.issuesgateway.dto.UpdateIssueRequest;
import edu.sjsu.cmpe272.issuesgateway.github.GitHubClient;
import edu.sjsu.cmpe272.issuesgateway.github.GitHubPage;
import edu.sjsu.cmpe272.issuesgateway.util.LinkHeaderUtil;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Primary
@Service
public class GitHubIssueService implements IssueService {

    private final GitHubClient githubClient;

    public GitHubIssueService(GitHubClient githubClient) {
        this.githubClient = githubClient;
    }

    @Override
    public IssueResponse createIssue(CreateIssueRequest request) {
        JsonNode issue = githubClient.createIssue(
                request.title(),
                request.body(),
                request.labels()
        );

        return toIssueResponse(issue);
    }

    @Override
    public List<IssueResponse> listIssues(
            String state,
            String labels,
            int page,
            int perPage
    ) {
        GitHubPage result =
                githubClient.listIssues(
                        state,
                        labels,
                        page,
                        perPage
                );

        List<IssueResponse> issues = new ArrayList<>();

        if (result.body() != null
                && result.body().isArray()) {

            result.body().forEach(issue ->
                    issues.add(toIssueResponse(issue))
            );
        }

        LinkHeaderUtil.parse(
                result.headers().getFirst("Link")
        );

        return issues;
    }

    @Override
    public IssueResponse getIssue(int number) {
        return toIssueResponse(
                githubClient.getIssue(number)
        );
    }

    @Override
    public IssueResponse updateIssue(
            int number,
            UpdateIssueRequest request
    ) {
        String state = request.state() == null
                ? null
                : request.state().name();

        JsonNode issue = githubClient.updateIssue(
                number,
                request.title(),
                request.body(),
                state
        );

        return toIssueResponse(issue);
    }

    @Override
    public CommentResponse createComment(
            int number,
            CreateCommentRequest request
    ) {
        return toCommentResponse(
                githubClient.createComment(
                        number,
                        request.body()
                )
        );
    }

    @Override
    public List<CommentResponse> listComments(int number) {
        return githubClient.listComments(number)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private IssueResponse toIssueResponse(JsonNode issue) {
        List<String> labels = new ArrayList<>();

        JsonNode labelArray = issue.path("labels");

        if (labelArray.isArray()) {
            labelArray.forEach(label ->
                    labels.add(label.path("name").asText())
            );
        }

        return new IssueResponse(
                issue.path("number").asInt(),
                issue.path("html_url").asText(),
                issue.path("state").asText(),
                issue.path("title").asText(),
                nullableText(issue, "body"),
                labels,
                parseTime(issue, "created_at"),
                parseTime(issue, "updated_at")
        );
    }

    private CommentResponse toCommentResponse(JsonNode comment) {
        return new CommentResponse(
                comment.path("id").asLong(),
                nullableText(comment, "body"),
                comment.path("user"),
                parseTime(comment, "created_at"),
                comment.path("html_url").asText()
        );
    }

    private String nullableText(
            JsonNode node,
            String field
    ) {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private OffsetDateTime parseTime(
            JsonNode node,
            String field
    ) {
        String value = nullableText(node, field);

        return value == null
                ? null
                : OffsetDateTime.parse(value);
    }
}