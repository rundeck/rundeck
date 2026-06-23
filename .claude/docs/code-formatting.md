# Code Formatting

## Purpose

This document describes the code formatting and linting tools used in the Rundeck project.

---

## Formatter: Spotless

[Spotless](https://github.com/diffplug/spotless) is used for code formatting.

---

## Configuration

### Gradual Enforcement with Ratchet

Only enforces formatting on files changed since `origin/main`:

```groovy
spotless {
    ratchetFrom 'origin/main'
}
```

### Explicit Exclusions

```java
// spotless:off
public String uglyButNecessaryFormatting    =    "stays as is";
// spotless:on
```

---

## Language Configurations

### Java

```groovy
java {
    target '**/*.java'
    importOrder()
    formatAnnotations()
    removeUnusedImports()
    palantirJavaFormat().style('GOOGLE')
}
```

### Groovy

```groovy
groovy {
    target '**/*.groovy'
    greclipse().configFile('greclipse.properties')
}
```

### GroovyGradle

```groovy
groovyGradle {
    target '*.gradle', '**/*.gradle'
    greclipse().configFile('greclipse.properties')
}
```

---

## Gradle Integration

```bash
# Check formatting
./gradlew spotlessCheck

# Fix formatting
./gradlew spotlessApply
```

---

## IntelliJ IDEA Integration

**Install**: [Spotless Gradle Plugin](https://plugins.jetbrains.com/plugin/18321-spotless-gradle)

Provides "Reformat with Spotless" in the Code menu.

---

## Workflow

1. Write code
2. If `spotlessCheck` fails: run `./gradlew spotlessApply`
3. Commit formatted code

---

## Resources

- [Spotless Github](https://github.com/diffplug/spotless)
- [Spotless Gradle Plugin](https://plugins.jetbrains.com/plugin/18321-spotless-gradle)