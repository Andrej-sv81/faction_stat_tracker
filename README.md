# D&D Dynamic Ecosystem & Faction Network Simulator

A pragmatic full-stack starter for evolving fantasy worlds. Spring Boot 3 / Java 21 provides JWT-protected PostgreSQL APIs; the Vite React dashboard visualizes worlds, factions, events, and simulation logs.

## Run locally

1. Start PostgreSQL (`docker compose up db -d`) or set the variables in `.env.example`.
2. Run the API: `cd backend && mvn spring-boot:run`.
3. Run the UI: `cd frontend && npm install && npm run dev`.

The UI is at http://localhost:5173 and the API at http://localhost:8080. Docker Compose runs the complete stack with `docker compose up --build`.

Register an account, create a world, then run simulations. `GET /api/worlds/{id}/export` provides a JSON snapshot suitable for backups/import pipelines. The API validates request payloads and returns consistent JSON errors.
