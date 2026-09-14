package edu.sjsu.cmpe272.issuesgateway.dto;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: Request model and validation for issue creation
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateIssueRequest(

        @NotBlank(message = "title is required")
        @Size(max = 256, message = "title must not exceed 256 characters")
        String title,

        String body,

        List<@NotBlank(message = "label must not be blank") String> labels
) {
}