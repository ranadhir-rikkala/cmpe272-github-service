package edu.sjsu.cmpe272.issuesgateway.dto;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: Response model for a created or listed comment
 */

import java.time.OffsetDateTime;

public record CommentResponse(
        long id,
        String body,
        Object user,
        OffsetDateTime createdAt,
        String htmlUrl
) {
}