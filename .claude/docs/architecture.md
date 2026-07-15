# Architecture

## Project Structure

- **`rundeckapp/`** — Main Grails application (controllers, services, domain models, migrations, frontend)
- **`core/`** — Core Rundeck Java library (plugin interfaces, shared utilities)
- **`grails-webhooks/`** — Webhooks plugin
- **`grails-rundeck-data-shared/`** — Shared data models
- **`functional-test/`** — Selenium and API functional tests

## Build Model

Rundeck is a standard Grails/Spring Boot application built with Gradle. The frontend (Vue 3) is bundled via Webpack and embedded in the war.

See `.claude/docs/build-commands.md` for build commands.

## Technology Stack

Exact dependency and framework versions are managed centrally in `gradle.properties`. Use that file as the source of truth for current versions.

| Category | Technologies |
|----------|-------------|
| **Backend** | Grails 7, Spring Boot 3.x, Groovy 4, Java 17 |
| **ORM** | GORM / Hibernate |
| **Database Migrations** | Liquibase |
| **Frontend** | Vue 3, TypeScript, Webpack 5 |
| **Testing** | Spock (Groovy), Jest (JavaScript), Selenium (functional) |

## Database & Migrations

Database migration rules auto-load when editing files in `**/migrations/**`. See **`.claude/rules/database-migrations.md`**.

**Critical Rule:** NEVER modify database migrations without explicit approval from a team member.

## File Organization

For detailed file locations (controllers, services, components, etc.), see `.claude/docs/file-locations.md`.