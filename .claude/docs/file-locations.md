# File Locations

Quick reference for where to find and create files in the codebase.

## Backend (Grails)

### Controllers
- `rundeckapp/grails-app/controllers/`

### Services
- `rundeckapp/grails-app/services/`

### Domain Models
- `rundeckapp/grails-app/domain/`

### Core Library
- `core/src/main/java/` — Plugin interfaces and shared utilities
- `core/src/main/groovy/`

## Frontend (Vue)

### Components
- **Library (atomic, no domain)**: `rundeckapp/grails-spa/packages/ui-trellis/src/library/components/`
- **App (feature, domain-aware)**: `rundeckapp/grails-spa/packages/ui-trellis/src/app/`

### Tests
- `rundeckapp/grails-spa/packages/ui-trellis/src/**/*.spec.ts`

## Internationalization (i18n)

### Message Files
- **Base**: `rundeckapp/grails-app/i18n/messages.properties`
- **Localized**: `messages_<locale>.properties` (e.g., `messages_es.properties`)

## Database

### Migrations
- **Location**: `rundeckapp/grails-app/migrations/`
- **Note**: See `.claude/rules/database-migrations.md` for migration rules

## Functional Tests

- **API tests**: `functional-test/src/test/groovy/`
- **Selenium page objects**: `functional-test/src/test/groovy/pages/`

## Configuration

### Build Files
- **Root**: `build.gradle`, `gradle.properties`, `settings.gradle`
- **App**: `rundeckapp/build.gradle`

### Application Config
- `rundeckapp/grails-app/conf/`
