package edu.sjsu.cmpe272.issuesgateway.controller;

/*
 * Author: Ranadhir Reddy Rikkala
 * Contribution: Endpoint tests for webhook signature handling, event
 * filtering and delivery idempotency.
 */

import edu.sjsu.cmpe272.issuesgateway.model.WebhookEvent;
import edu.sjsu.cmpe272.issuesgateway.repository.WebhookEventRepository;
import edu.sjsu.cmpe272.issuesgateway.service.HmacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WebhookControllerTest {

    private static final String BODY = "{\"action\":\"opened\",\"issue\":{\"number\":7}}";
    private static final String SECRET = "test-webhook-secret";

    private MockMvc mockMvc;
    private WebhookEventRepository repository;

    @BeforeEach
    void setUp() {
        HmacService hmacService = new HmacService();
        ReflectionTestUtils.setField(hmacService, "secret", SECRET);

        repository = mock(WebhookEventRepository.class);

        mockMvc = standaloneSetup(
                new WebhookController(hmacService, repository)
        ).build();
    }

    private String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(
                mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void invalidSignatureReturns401AndPersistsNothing() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType("application/json")
                        .header("X-GitHub-Event", "issues")
                        .header("X-GitHub-Delivery", "d-1")
                        .header("X-Hub-Signature-256", "sha256=bad")
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        verify(repository, never()).save(any());
    }

    @Test
    void validIssuesEventReturns204AndPersists() throws Exception {
        when(repository.existsByDeliveryIdAndAction(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/webhook")
                        .contentType("application/json")
                        .header("X-GitHub-Event", "issues")
                        .header("X-GitHub-Delivery", "d-2")
                        .header("X-Hub-Signature-256", sign(BODY))
                        .content(BODY))
                .andExpect(status().isNoContent());

        verify(repository, times(1)).save(any(WebhookEvent.class));
    }

    @Test
    void issueCommentEventIsAccepted() throws Exception {
        when(repository.existsByDeliveryIdAndAction(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/webhook")
                        .contentType("application/json")
                        .header("X-GitHub-Event", "issue_comment")
                        .header("X-GitHub-Delivery", "d-3")
                        .header("X-Hub-Signature-256", sign("{\"action\":\"created\",\"issue\":{\"number\":7}}"))
                        .content("{\"action\":\"created\",\"issue\":{\"number\":7}}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void pingEventIsAccepted() throws Exception {
        when(repository.existsByDeliveryIdAndAction(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/webhook")
                        .contentType("application/json")
                        .header("X-GitHub-Event", "ping")
                        .header("X-GitHub-Delivery", "d-4")
                        .header("X-Hub-Signature-256", sign("{\"zen\":\"Design for failure.\"}"))
                        .content("{\"zen\":\"Design for failure.\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownEventReturns400() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType("application/json")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", "d-5")
                        .header("X-Hub-Signature-256", sign(BODY))
                        .content(BODY))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any());
    }

    @Test
    void duplicateDeliveryIsAcknowledgedButNotStoredTwice() throws Exception {
        when(repository.existsByDeliveryIdAndAction("d-6", "opened")).thenReturn(true);

        mockMvc.perform(post("/webhook")
                        .contentType("application/json")
                        .header("X-GitHub-Event", "issues")
                        .header("X-GitHub-Delivery", "d-6")
                        .header("X-Hub-Signature-256", sign(BODY))
                        .content(BODY))
                .andExpect(status().isNoContent());

        verify(repository, never()).save(any());
    }

    @Test
    void eventsEndpointReturnsStoredDeliveries() throws Exception {
        List<WebhookEvent> stored = new ArrayList<>();
        stored.add(new WebhookEvent("d-7", "issues", "opened", BODY));
        when(repository.findLatestEvents(any(Pageable.class))).thenReturn(stored);

        mockMvc.perform(get("/events").param("limit", "5"))
                .andExpect(status().isOk());
    }
}
