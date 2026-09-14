package edu.sjsu.cmpe272.issuesgateway.dto;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: Request model for adding a comment
 */

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(

        @NotBlank(message = "comment body is required")
        String body
) {
}