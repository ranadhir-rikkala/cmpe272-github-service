package edu.sjsu.cmpe272.issuesgateway.dto;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: Response model for an issue
 */

import java.time.OffsetDateTime;
import java.util.List;

public record IssueResponse(
        int number,
        String htmlUrl,
        String state,
        String title,
        String body,
        List<String> labels,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}