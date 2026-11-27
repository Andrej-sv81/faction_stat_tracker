# D&D Dynamic Ecosystem & Faction Network Simulator

A pragmatic full-stack starter for evolving fantasy worlds. Spring Boot 3 / Java 21 provides JWT-protected PostgreSQL APIs; the Vite React dashboard visualizes worlds, factions, events, and simulation logs.

## Run locally

1. Start PostgreSQL (`docker compose up db -d`) or set the variables in `.env.example`.
2. Run the API: `cd backend && mvn spring-boot:run`.
3. Run the UI: `cd frontend && npm install && npm run dev`.

The UI is at http://localhost:5173 and the API at http://localhost:8080. Docker Compose runs the complete stack with `docker compose up --build`.

Register an account, create a world, then run simulations. `GET /api/worlds/{id}/export` provides a JSON snapshot suitable for backups/import pipelines. The API validates request payloads and returns consistent JSON errors.

### World event types

Event types are stored in the `event_types` table and seeded from `backend/src/main/resources/faction_event_categories.csv` when the API starts. Existing catalog entries are not overwritten by startup seeding. `GET /api/event-types` returns the enabled types and their metadata. Event requests send only the stable `type` code; the event's category/domain is derived from that catalog entry. Targeting is inferred from faction IDs: no target IDs applies the event to every faction, one ID targets one faction, and two distinct IDs target both factions. Random simulation chooses an enabled catalog type, and imported events must reference an enabled type code.

Generated random events use the selected type's impact profile instead of assigning a random sign to a random stat. For example, alliances improve manpower and influence, monetary gains increase wealth, and conflicts reduce manpower, wealth, and power. Ambiguous events such as migration can represent either an influx or an exodus. This applies to both the backend random-turn action and the Go runner; manually authored events continue to use their explicitly supplied deltas.

Faction and event responses include a `version`. Include that value in faction and event `PUT` requests. Updates with a stale version return `409 Conflict`; reload the resource before retrying.

### Dashboard periodic simulation

The dashboard's periodic simulation controls start one scheduled random turn for the selected world. Choose an interval from 1 to 1440 minutes and a severity from 1 to 5; the first turn runs after the selected interval. The dashboard refreshes the world snapshot while the schedule is active so generated events appear in the chronicle. Stop the schedule from the same controls; a turn already in progress may still finish.

Schedules run in the Java API process, are scoped to the authenticated owner and world, and are not persisted. They stop if the API restarts, and a multi-instance deployment runs schedules only in the instance that received the start request. Users can run schedules independently for worlds they own.

Example event request:

```json
{
  "title": "A pact is signed",
  "type": "ALLIANCE",
  "targetFactionId": "faction-id",
  "secondTargetFactionId": null,
  "powerDelta": 0,
  "wealthDelta": 5,
  "manpowerDelta": 0,
  "influenceDelta": 3,
  "territoryDelta": 0,
  "details": "The factions agree to mutual defense."
}
```

### Go world simulation runner

The Go runner creates random events through the authenticated Java API, so faction changes and chronicle logs are persisted by the backend. It starts paused; use `play`, `pause`, `interval <minutes>`, and `severity <1-5>` in its console. Severity scales generated stat deltas; manually submitted event deltas remain exact. The interval and play state live in the runner process and reset when it exits.

```powershell
cd tools\world-simulation
$env:FACTION_STAT_TRACKER_TOKEN = "<access-token>"
go run . -world <world-id> -interval-minutes 5 -severity 3
```

The runner supports intervals from 1 to 1440 minutes and requires an access token with permission to read and update the selected world.

### Password reset

Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, and any required SMTP credentials in `.env.development` to deliver reset links. Reset requests return the same `202 Accepted` response whether or not the email is registered. The reset token expires after `PASSWORD_RESET_EXPIRATION_MS` (15 minutes by default), can only be used once, and successful password changes revoke the user's refresh-token sessions.

- `POST /api/auth/password-reset/request` with `{"email":"user@example.com"}`
- `POST /api/auth/password-reset/confirm` with `{"token":"...","newPassword":"..."}`

The browser should obtain a CSRF token from `GET /api/auth/csrf` before sending these state-changing requests. The reset link targets `/reset-password#token=...` on `FRONTEND_URL`, keeping the token in the URL fragment so it is not sent in HTTP requests or referrer headers; the frontend page is responsible for reading it and submitting the token and new password.
