# Makefile

# .PHONY declares these as commands, not file targets.
# Without this, if a file named "up" ever exists, make would
# skip the target thinking it's already "built".
.PHONY: up down restart-app clean logs logs-app \
        redis-cli status test build shell

# ── Variables ─────────────────────────────────────────────────────
APP_NAME     = flowforge
COMPOSE_FILE = docker-compose.yml

# ── Primary Commands ──────────────────────────────────────────────

## Start the full stack (Redis + App), rebuild app image if code changed
up:
	docker-compose -f $(COMPOSE_FILE) up --build -d
	@echo ""
	@echo "  FlowForge is running"
	@echo "  App  → http://localhost:8080"
	@echo "  Redis→ localhost:6379"
	@echo ""

## Stop all containers, keep volumes (data survives)
down:
	docker-compose -f $(COMPOSE_FILE) down

## Rebuild and restart ONLY the app (Redis keeps running, data preserved)
## Use this during active development to avoid full restart
restart-app:
	docker-compose -f $(COMPOSE_FILE) up --build -d --no-deps flowforge

## Stop everything AND delete all volumes (full reset, data gone)
## Use when you want a completely clean slate
clean:
	docker-compose -f $(COMPOSE_FILE) down -v --remove-orphans
	@echo "All containers and volumes removed."

# ── Logs ──────────────────────────────────────────────────────────

## Tail logs from ALL services
logs:
	docker-compose -f $(COMPOSE_FILE) logs -f

## Tail logs from the app only
logs-app:
	docker-compose -f $(COMPOSE_FILE) logs -f flowforge

# ── Debugging ─────────────────────────────────────────────────────

## Drop into an interactive Redis CLI session
## Use this to inspect keys, check rate limit state, read metrics
redis-cli:
	docker exec -it flowforge-redis redis-cli

## Drop into a shell inside the running app container
## Use this to inspect the filesystem, check env vars, debug
shell:
	docker exec -it flowforge-app sh

## Show container status and health
status:
	docker-compose -f $(COMPOSE_FILE) ps

# ── Build & Test ──────────────────────────────────────────────────

## Run all tests (uses Testcontainers — spins up real Redis for integration tests)
test:
	./mvnw test

## Build the JAR without running tests
build:
	./mvnw clean package -DskipTests -q
	@echo "JAR built → target/$(APP_NAME)-*.jar"

# ── Help ──────────────────────────────────────────────────────────

## Print available commands
help:
	@echo ""
	@echo "FlowForge commands:"
	@grep -E '^##' Makefile | sed 's/## /  /'
	@echo ""