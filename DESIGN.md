# Design Note — GitHub Issues Gateway

## Error mapping

GitHub error responses never reach the client unchanged. `GitHubClient`
inspects the upstream status and headers and throws a `GitHubApiException`
carrying our own status, message and optional `Retry-After`. A single
`@RestControllerAdvice` renders every failure as the same `ApiError` schema:
`{message, status, timestamp, details}`.

| GitHub | Ours | Rationale |
|---|---|---|
| 401 | 401 | Token invalid or expired |
| 403 with `X-RateLimit-Remaining: 0` | 429 | Rate limit, not authorization |
| 403 otherwise | 403 | Token lacks `issues=write` |
| 404 | 404 | Issue absent, or repo invisible to the token |
| 422 | 400 | Upstream validation failure is a client error here |
| 5xx / timeout | 503 | Upstream fault, not ours |

The 403 split matters. GitHub returns 403 both for insufficient permissions
and for exhausted rate limits, which are different problems for a caller: one
is permanent until the token changes, the other resolves on its own. We
disambiguate on `X-RateLimit-Remaining` and surface 429 with `Retry-After` so
clients can back off correctly.

A catch-all handler returns a generic 500 body for unexpected faults. Without
it Spring's default error view serialises the full stack trace into the
response, leaking package structure and file paths.

Validation failures are caught separately and return field-level detail —
`{"details":{"title":"title is required"}}` — so the caller learns which field
was wrong rather than only that something was.

## Pagination strategy

We forward `page` and `per_page` to GitHub rather than inventing our own
cursor scheme. GitHub's issue list is mutable and stable pagination is its
concern, not ours; reimplementing it would mean caching state we have no way
to invalidate.

`per_page` is validated at the boundary (1–100) and rejected with 400 rather
than silently clamped. Clamping would return a different page size than the
caller asked for with no indication anything happened.

`LinkHeaderUtil` parses GitHub's `Link` header into page numbers. The intent
is to re-emit it on our own responses with the URLs rewritten to point at this
service rather than `api.github.com`, so that clients are never handed upstream
URLs they cannot authenticate against. That re-emission is not yet wired into
the controller: the parser is implemented and unit-tested, but the list
endpoint currently returns the body without pagination headers. This is a known
gap rather than a design decision.

A second known gap: GitHub's issues endpoint also returns pull requests, which
carry a `pull_request` field. We do not currently filter them, so a caller
listing issues may receive PRs in the results.

## Webhook dedupe

Deliveries are deduplicated on the composite key
`(X-GitHub-Delivery, action)`, enforced by a primary key in the event store
with an insert-if-absent write.

The delivery id alone is insufficient. GitHub reuses a delivery id across
redelivery attempts of the same event, which is exactly what we want to
collapse — but including `action` keeps semantically distinct events
distinguishable if the same id is ever reused across actions, and makes the
key self-describing when debugging.

A duplicate delivery returns `204`, not `409`. GitHub treats any non-2xx as a
failure and will retry, so answering 409 on a duplicate would produce an
infinite retry loop over an event we have already processed correctly.
"Already handled" and "handled just now" are the same outcome from the
sender's point of view.

The handler parses and persists synchronously — both are sub-millisecond — and
acknowledges immediately. Any slower work added later should be dispatched
asynchronously only after the row commits, so an ack is never issued for an
event that was not durably recorded.

## Security trade-offs

**Secrets.** All five configuration values come from the environment.
`.env` is git-ignored and `.env.example` documents the shape without values.
An earlier commit contained a hard-coded webhook secret; it was removed and
the value rotated, since deleting a line does not remove it from git history.

**Token scope.** A fine-grained PAT limited to one repository with Issues:
Read and write and Metadata: Read-only. A classic PAT would have been simpler
— it works on repositories owned by others, which fine-grained tokens cannot
reach — but its `repo` scope grants access to every repository the user can
see. We accepted the narrower token and the constraint that each developer
points at their own test repository.

**Signature comparison.** HMAC-SHA256 is computed over the raw request bytes,
never over a re-serialised object, and compared with `MessageDigest.isEqual`.
Byte-for-byte equality matters: deserialising and re-serialising JSON changes
whitespace and key order, so the recomputed signature would never match. The
constant-time comparison prevents a timing side channel from revealing the
expected digest one byte at a time. Neither the secret nor the received
signature is ever logged.

**What we deliberately did not do.** Our own endpoints are unauthenticated.
Anyone who can reach the port can create issues in the configured repository.
This is acceptable for a locally-run service bound to localhost, and it keeps
the assignment's surface area focused on the GitHub integration. A production
deployment would need an API key, mTLS, or an OAuth layer in front — the
service currently relies entirely on network isolation, which is a deployment
property rather than an application one.

We also chose an in-memory event store. It is sufficient to demonstrate
dedupe and inspection, but events do not survive a restart. A file-backed or
disk-persisted database would be the first change for any real use, since the
dedupe guarantee is only as durable as the store behind it.
