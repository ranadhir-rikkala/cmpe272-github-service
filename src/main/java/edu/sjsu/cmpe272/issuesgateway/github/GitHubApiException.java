package edu.sjsu.cmpe272.issuesgateway.github;



public class GitHubApiException extends RuntimeException {

    private final int statusCode;
    private final String retryAfter;

    public GitHubApiException(
            int statusCode,
            String message,
            String retryAfter
    ) {
        super(message);
        this.statusCode = statusCode;
        this.retryAfter = retryAfter;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}