# CMPE-272 HW #2 — GitHub Service

A Spring Boot 3 service that wraps the GitHub REST API for Issues on one
repository, handles GitHub webhooks with HMAC verification, and ships an
OpenAPI 3.1 contract (`openapi.yaml`).

GitHub has no delete API for issues, so the **D in CRUD** is closing an issue:
`PATCH /issues/{number}` with `{"state":"closed"}`.

## Team

| Person | Contribution |
|---|---|
| Sai Vineetha Tirumalla | Core API routes, DTO validation, OpenAPI contract |
| Shravani Naikoti | GitHub integration, pagination, error handling |
| Mukesh Singh | Webhooks, HMAC verification, event persistence |
| Ranadhir Reddy Rikkala | Configuration, error handling, health check, Docker, CI, documentation |

Per-file authorship is recorded in source comments.

## Configuration

All configuration comes from environment variables.

| Variable | Description |
|---|---|
| `GITHUB_TOKEN` | Fine-grained personal access token |
| `GITHUB_OWNER` | Repository owner |
| `GITHUB_REPO` | Repository name |
| `WEBHOOK_SECRET` | Webhook HMAC secret, from `openssl rand -hex 32` |
| `PORT` | Port the service listens on |

```bash
cp .env.example .env
```

`.env` is git-ignored and is never committed.

**PAT scopes used** — fine-grained token, minimum permissions:

- Repository access: Only select repositories → the single target repo
- Issues: **Read and write**
- Metadata: Read-only (required by GitHub)
- All other permissions: No access

"Public repositories" access is read-only and causes `403` on writes.

## Running

Without Docker:

```bash
set -a && . ./.env && set +a
./mvnw spring-boot:run
```

With Docker:

```bash
docker build -t issues-gw .
docker run --rm -p 8080:8080 --env-file .env -e PORT=8080 issues-gw
```

Tests:

```bash
./mvnw test
```

## API

Base URL: `http://localhost:${PORT}`

**Create an issue** — `201` with `Location: /issues/{number}`

```bash
curl -isS -X POST localhost:8080/issues \
  -H 'Content-Type: application/json' \
  -d '{"title":"Login button broken","body":"Steps to reproduce...","labels":["bug"]}'
```

Missing `title` returns `400`:

```bash
curl -isS -X POST localhost:8080/issues \
  -H 'Content-Type: application/json' -d '{"body":"no title"}'
```

**List issues** — `state` (`open`|`closed`|`all`, default `open`), `labels`,
`page` (>= 1), `per_page` (1–100). Invalid values return `400`.

```bash
curl -isS 'localhost:8080/issues?state=all&page=1&per_page=5'
```

**Get one issue** — `200`, or `404` if absent

```bash
curl -isS localhost:8080/issues/9
```

**Update, close, reopen** — all `200`

```bash
curl -isS -X PATCH localhost:8080/issues/9 \
  -H 'Content-Type: application/json' -d '{"title":"Renamed","body":"Updated"}'

curl -isS -X PATCH localhost:8080/issues/9 \
  -H 'Content-Type: application/json' -d '{"state":"closed"}'

curl -isS -X PATCH localhost:8080/issues/9 \
  -H 'Content-Type: application/json' -d '{"state":"open"}'
```

**Add a comment** — `201`

```bash
curl -isS -X POST localhost:8080/issues/9/comments \
  -H 'Content-Type: application/json' -d '{"body":"Investigating."}'
```

**List comments** — `200`

```bash
curl -sS localhost:8080/issues/9/comments
```

**Webhook** — `204` valid, `401` bad signature, `400` unknown event,
`204` again on a duplicate delivery

```bash
set -a && . ./.env && set +a
BODY='{"action":"opened","issue":{"number":1}}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | awk '{print $2}')
curl -isS -X POST localhost:8080/webhook \
  -H 'Content-Type: application/json' \
  -H 'X-GitHub-Event: issues' \
  -H 'X-GitHub-Delivery: demo-1' \
  -H "X-Hub-Signature-256: sha256=$SIG" \
  -d "$BODY"
```

**Recent deliveries** and **health**

```bash
curl -sS 'localhost:8080/events?limit=10'
curl -sS localhost:8080/healthz
```

## Webhook setup

1. Start the service and open a tunnel:

   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```

2. Repository → **Settings → Webhooks → Add webhook**:

   | Field | Value |
   |---|---|
   | Payload URL | `https://<tunnel-host>/webhook` |
   | Content type | `application/json` |
   | Secret | the exact `WEBHOOK_SECRET` value |
   | Events | Issues, Issue comments |

   Content type must be `application/json` — form encoding alters the body and
   invalidates the signature.

3. Save. GitHub sends a `ping`; confirm a green check and `204` under
   **Recent Deliveries**.

**Redelivery.** Settings → Webhooks → the webhook → **Recent Deliveries** →
pick one → **Redeliver**. The response is still `204` and `GET /events` shows
the same row count, because deliveries are deduplicated on
`(X-GitHub-Delivery, action)`.

## Design note

See [DESIGN.md](DESIGN.md) for error mapping, pagination strategy, webhook
dedupe and security trade-offs.

## Credits

Project skeleton generated with Spring Initializr (https://start.spring.io).
All business logic, tests and the OpenAPI contract are our own work.
