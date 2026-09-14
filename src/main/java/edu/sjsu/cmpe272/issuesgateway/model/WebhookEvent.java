


package edu.sjsu.cmpe272.issuesgateway.model;

/*
 * Author: Mukesh Singh
 * Contribution: Persisted webhook delivery entity
 */

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "webhook_events",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_delivery_action", columnNames = {"delivery_id", "action"})
    }
)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private String deliveryId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "action")
    private String action;

    @Column(name = "payload", columnDefinition = "CLOB")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public WebhookEvent() {}

    public WebhookEvent(String deliveryId, String eventType, String action, String payload) {
        this.deliveryId = deliveryId;
        this.eventType = eventType;
        this.action = action;
        this.payload = payload;
        this.receivedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getDeliveryId() { return deliveryId; }
    public String getEventType() { return eventType; }
    public String getAction() { return action; }
    public String getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
}
