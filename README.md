# D&D Dynamic Ecosystem & Faction Network Simulator

A pragmatic full-stack starter for evolving fantasy worlds. Spring Boot 3 / Java 21 provides JWT-protected PostgreSQL APIs; the Vite React dashboard visualizes worlds, factions, events, and simulation logs.

## Run locally

1. Start PostgreSQL (`docker compose up db -d`) or set the variables in `.env.example`.
2. Run the API: `cd backend && mvn spring-boot:run`.
3. Run the UI: `cd frontend && npm install && npm run dev`.

The UI is at http://localhost:5173 and the API at http://localhost:8080. Docker Compose runs the complete stack with `docker compose up --build`.

Register an account, create a world, then run simulations. `GET /api/worlds/{id}/export` provides a JSON snapshot suitable for backups/import pipelines. The API validates request payloads and returns consistent JSON errors.

### Password reset

Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, and any required SMTP credentials in `.env.development` to deliver reset links. Reset requests return the same `202 Accepted` response whether or not the email is registered. The reset token expires after `PASSWORD_RESET_EXPIRATION_MS` (15 minutes by default), can only be used once, and successful password changes revoke the user's refresh-token sessions.

- `POST /api/auth/password-reset/request` with `{"email":"user@example.com"}`
- `POST /api/auth/password-reset/confirm` with `{"token":"...","newPassword":"..."}`

The browser should obtain a CSRF token from `GET /api/auth/csrf` before sending these state-changing requests. The reset link targets `/reset-password#token=...` on `FRONTEND_URL`, keeping the token in the URL fragment so it is not sent in HTTP requests or referrer headers; the frontend page is responsible for reading it and submitting the token and new password.
