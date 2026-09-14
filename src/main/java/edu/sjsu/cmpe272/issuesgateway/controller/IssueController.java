package edu.sjsu.cmpe272.issuesgateway.controller;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: REST controller for issue CRUD and comment routes
 */

import edu.sjsu.cmpe272.issuesgateway.dto.CommentResponse;
import edu.sjsu.cmpe272.issuesgateway.dto.CreateCommentRequest;
import edu.sjsu.cmpe272.issuesgateway.dto.CreateIssueRequest;
import edu.sjsu.cmpe272.issuesgateway.dto.IssueResponse;
import edu.sjsu.cmpe272.issuesgateway.dto.UpdateIssueRequest;
import edu.sjsu.cmpe272.issuesgateway.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request
    ) {
        IssueResponse created = issueService.createIssue(request);

        return ResponseEntity
                .created(URI.create("/issues/" + created.number()))
                .body(created);
    }

    @GetMapping
    public ResponseEntity<List<IssueResponse>> listIssues(
            @RequestParam(defaultValue = "open") String state,
            @RequestParam(required = false) String labels,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "30") int perPage
    ) {
        if (!state.equals("open")
                && !state.equals("closed")
                && !state.equals("all")) {
            throw new IllegalArgumentException(
                    "state must be open, closed, or all"
            );
        }

        if (page < 1) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 1"
            );
        }

        if (perPage < 1 || perPage > 100) {
            throw new IllegalArgumentException(
                    "per_page must be between 1 and 100"
            );
        }

        return ResponseEntity.ok(
                issueService.listIssues(
                        state,
                        labels,
                        page,
                        perPage
                )
        );
    }

    @GetMapping("/{number}")
    public ResponseEntity<IssueResponse> getIssue(
            @PathVariable int number
    ) {
        return ResponseEntity.ok(
                issueService.getIssue(number)
        );
    }

    @PatchMapping("/{number}")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable int number,
            @Valid @RequestBody UpdateIssueRequest request
    ) {
        return ResponseEntity.ok(
                issueService.updateIssue(number, request)
        );
    }

    @PostMapping("/{number}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable int number,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ResponseEntity
                .status(201)
                .body(issueService.createComment(number, request));
    }

    @GetMapping("/{number}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(
            @PathVariable int number
    ) {
        return ResponseEntity.ok(
                issueService.listComments(number)
        );
    }
}