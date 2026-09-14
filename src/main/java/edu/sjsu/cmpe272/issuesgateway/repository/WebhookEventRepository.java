package edu.sjsu.cmpe272.issuesgateway.repository;

/*
 * Author: Mukesh Singh
 * Contribution: Repository for webhook delivery storage and dedupe lookup
 */

import edu.sjsu.cmpe272.issuesgateway.model.WebhookEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByDeliveryIdAndAction(String deliveryId, String action);

    @Query("SELECT e FROM WebhookEvent e ORDER BY e.receivedAt DESC")
    List<WebhookEvent> findLatestEvents(Pageable pageable);
}
