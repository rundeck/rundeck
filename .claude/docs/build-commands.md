# Build Commands Reference

Quick reference for common build and test commands in the rundeck OSS repo.

## Common Build Tasks

```bash
# Full build
./gradlew build

# Build without tests
./gradlew build -x test

# Build without tests or quality checks (fastest — 4-8 min)
./gradlew build -x check

# Run in dev mode
./gradlew bootRun

# Clean build artifacts
./gradlew clean

# Code formatting
./gradlew spotlessCheck        # Check formatting
./gradlew spotlessApply        # Auto-fix formatting
```

## Backend Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.MySpec"

# Run tests for specific module
./gradlew :rundeckapp:test
```

## Functional Tests

```bash
# API functional tests
./gradlew :functional-test:apiTest

# Selenium functional tests
./gradlew :functional-test:seleniumTest
```

## Frontend Tests

```bash
# Core UI tests
UI=rundeckapp/grails-spa/packages/ui-trellis
npm run --prefix "$UI" ci:test:unit           # Run tests
npm run --prefix "$UI" dev:test:watch         # Watch mode
```

## Build Verification

```bash
# Quick build verification (4-8 minutes)
./gradlew build -x check

# Compiles code and builds artifacts without running:
# - Test suite (can take 1+ hour)
# - Code quality checks (spotless, checkstyle)
# CI will run the full test suite
```

## Troubleshooting

### Build Fails with "Cannot find Java 17"

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew build -x check
```

### Frontend Build Fails

```bash
rm -rf node_modules package-lock.json
npm install
```

### Gradle Daemon Issues

```bash
./gradlew --stop
rm -rf ~/.gradle/caches
```