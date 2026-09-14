package edu.sjsu.cmpe272.issuesgateway.controller;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: Unit tests for route validation and error responses
 */

import edu.sjsu.cmpe272.issuesgateway.dto.IssueResponse;
import edu.sjsu.cmpe272.issuesgateway.service.IssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class IssueControllerTest {

    private MockMvc mockMvc;
    private IssueService issueService;

    @BeforeEach
    void setUp() {
        issueService = mock(IssueService.class);

        IssueController controller =
                new IssueController(issueService);

        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(
                        new edu.sjsu.cmpe272.issuesgateway.exception
                                .GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void missingTitleReturns400() throws Exception {
        mockMvc.perform(
                        post("/issues")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "body": "Missing title"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidStateReturns400() throws Exception {
        mockMvc.perform(
                        patch("/issues/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "state": "invalid"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIssueReturns201AndLocation() throws Exception {
        IssueResponse response = new IssueResponse(
                42,
                "https://github.com/example/repo/issues/42",
                "open",
                "Test issue",
                "Test body",
                List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(issueService.createIssue(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/issues")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "title": "Test issue",
                                          "body": "Test body"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                "/issues/42"
                        )
                );
    }
}