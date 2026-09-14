package edu.sjsu.cmpe272.issuesgateway.controller;

/*
 * Author: Mukesh Singh
 * Contribution: Webhook receiver, event filtering, dedupe and events listing
 */

import edu.sjsu.cmpe272.issuesgateway.model.WebhookEvent;
import edu.sjsu.cmpe272.issuesgateway.repository.WebhookEventRepository;
import edu.sjsu.cmpe272.issuesgateway.service.HmacService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
public class WebhookController {

    private static final Set<String> ALLOWED_EVENTS = Set.of("issues", "issue_comment", "ping");

    private final HmacService hmacService;
    private final WebhookEventRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebhookController(HmacService hmacService, WebhookEventRepository repository) {
        this.hmacService = hmacService;
        this.repository = repository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody byte[] rawBody) {

        // 1. HMAC Verification
        if (!hmacService.verifySignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Validate Event
        if (eventType == null || !ALLOWED_EVENTS.contains(eventType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String bodyString = new String(rawBody, StandardCharsets.UTF_8);
        String action = null;
        try {
            JsonNode root = mapper.readTree(bodyString);
            if (root.has("action")) {
                action = root.get("action").asText();
            }
        } catch (Exception ignored) {}

        // 3. Deduplicate (Delivery ID + Action)
        String dedupeActionKey = (action != null) ? action : "N/A";
        if (repository.existsByDeliveryIdAndAction(deliveryId, dedupeActionKey)) {
            return ResponseEntity.noContent().build();
        }

        // Save
        WebhookEvent event = new WebhookEvent(deliveryId, eventType, dedupeActionKey, bodyString);
        repository.save(event);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events")
    public ResponseEntity<List<WebhookEvent>> getEvents(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        List<WebhookEvent> events = repository.findLatestEvents(PageRequest.of(0, limit));
        return ResponseEntity.ok(events);
    }
}
