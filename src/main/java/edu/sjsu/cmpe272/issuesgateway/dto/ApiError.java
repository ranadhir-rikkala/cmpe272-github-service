package edu.sjsu.cmpe272.issuesgateway.dto;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: Error response model returned by all failure paths
 */

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiError(
        String message,
        int status,
        OffsetDateTime timestamp,
        Map<String, String> details
) {
}