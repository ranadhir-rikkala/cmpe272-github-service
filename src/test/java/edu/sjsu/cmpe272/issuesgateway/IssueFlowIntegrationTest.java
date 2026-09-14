package edu.sjsu.cmpe272.issuesgateway;

/*
 * Author: Ranadhir Reddy Rikkala
 * Contribution: End-to-end integration test exercising the full issue
 * lifecycle against the real GitHub API. Skipped automatically when
 * GITHUB_TOKEN is not present, so CI passes without credentials.
 */

import edu.sjsu.cmpe272.issuesgateway.dto.CommentResponse;
import edu.sjsu.cmpe272.issuesgateway.dto.IssueResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "GITHUB_TOKEN", matches = ".+")
@TestPropertySource(properties = {
        "github.api-url=https://api.github.com",
        "github.token=${GITHUB_TOKEN}",
        "github.owner=${GITHUB_OWNER}",
        "github.repo=${GITHUB_REPO}"
})
class IssueFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private static final List<Integer> created = new ArrayList<>();

    private static int issueNumber;

    @BeforeAll
    void useHttpClientThatSupportsPatch() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    @Order(1)
    void createIssueThenReadItBack() {
        ResponseEntity<IssueResponse> created201 = rest.postForEntity(
                "/issues",
                json("{\"title\":\"integration test issue\",\"body\":\"created by test\"}"),
                IssueResponse.class
        );

        assertEquals(HttpStatus.CREATED, created201.getStatusCode());
        assertNotNull(created201.getBody());

        issueNumber = created201.getBody().number();
        created.add(issueNumber);

        assertEquals(
                "/issues/" + issueNumber,
                created201.getHeaders().getFirst(HttpHeaders.LOCATION)
        );

        ResponseEntity<IssueResponse> fetched = rest.getForEntity(
                "/issues/" + issueNumber, IssueResponse.class);

        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertNotNull(fetched.getBody());
        assertEquals(issueNumber, fetched.getBody().number());
        assertEquals("integration test issue", fetched.getBody().title());
    }

    @Test
    @Order(2)
    void updateTitleAndBody() {
        ResponseEntity<IssueResponse> updated = rest.exchange(
                "/issues/" + issueNumber,
                HttpMethod.PATCH,
                json("{\"title\":\"integration test issue (renamed)\",\"body\":\"edited by test\"}"),
                IssueResponse.class
        );

        assertEquals(HttpStatus.OK, updated.getStatusCode());
        assertNotNull(updated.getBody());
        assertEquals("integration test issue (renamed)", updated.getBody().title());
        assertEquals("edited by test", updated.getBody().body());
    }

    @Test
    @Order(3)
    void closeThenReopen() {
        ResponseEntity<IssueResponse> closed = rest.exchange(
                "/issues/" + issueNumber,
                HttpMethod.PATCH,
                json("{\"state\":\"closed\"}"),
                IssueResponse.class
        );

        assertEquals(HttpStatus.OK, closed.getStatusCode());
        assertNotNull(closed.getBody());
        assertEquals("closed", closed.getBody().state());

        ResponseEntity<IssueResponse> reopened = rest.exchange(
                "/issues/" + issueNumber,
                HttpMethod.PATCH,
                json("{\"state\":\"open\"}"),
                IssueResponse.class
        );

        assertEquals(HttpStatus.OK, reopened.getStatusCode());
        assertNotNull(reopened.getBody());
        assertEquals("open", reopened.getBody().state());
    }

    @Test
    @Order(4)
    void createCommentThenListComments() {
        ResponseEntity<CommentResponse> comment = rest.postForEntity(
                "/issues/" + issueNumber + "/comments",
                json("{\"body\":\"comment from integration test\"}"),
                CommentResponse.class
        );

        assertEquals(HttpStatus.CREATED, comment.getStatusCode());
        assertNotNull(comment.getBody());
        assertEquals("comment from integration test", comment.getBody().body());

        ResponseEntity<CommentResponse[]> listed = rest.getForEntity(
                "/issues/" + issueNumber + "/comments", CommentResponse[].class);

        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertNotNull(listed.getBody());
        assertTrue(listed.getBody().length >= 1);
    }

    @Test
    @Order(5)
    void listIssuesHonoursPagination() {
        ResponseEntity<IssueResponse[]> listed = rest.getForEntity(
                "/issues?state=all&page=1&per_page=2", IssueResponse[].class);

        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertNotNull(listed.getBody());
        assertTrue(listed.getBody().length <= 2);
    }

    @AfterAll
    void closeCreatedIssues() {
        for (Integer number : created) {
            try {
                rest.exchange(
                        "/issues/" + number,
                        HttpMethod.PATCH,
                        json("{\"state\":\"closed\"}"),
                        IssueResponse.class
                );
            } catch (Exception ignored) {
                // best effort cleanup
            }
        }
    }
}
