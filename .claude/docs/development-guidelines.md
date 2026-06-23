# Development Guidelines

**Source**: [Rundeck OSS Github Guidelines](https://github.com/rundeck/rundeck/wiki/Github-Guidelines)

## Overview

Core development guidelines for Rundeck OSS development.

---

## General Best Practices

### Avoid Commented-Out Code

**Rule**: Do NOT commit commented-out code

**Why**: Version control preserves history — use it instead of comments
- Delete the code and describe why in the commit message

---

## Groovy Code Standards

### Use @CompileStatic

**Rule**: All Groovy classes MUST use `@CompileStatic`

```groovy
@CompileStatic
class MyService {
    // your code
}
```

**Exceptions**:
- Methods that can't use static typing: use `@CompileDynamic` on those specific methods only
- Grails components (Controllers, Services): use `@GrailsCompileStatic` instead

### Don't Implement Property Getters/Setters

**Rule**: Let Groovy auto-generate getters/setters

```groovy
// Good
class MySomething implements Something {
    String name
    List<String> flavors
    // No need to implement getName() and getFlavors()
}
```

### Use @Delegate

**Rule**: Use `@Delegate` when wrapping another implementation

```groovy
class MyService implements Something {
    @Delegate
    Something otherService
}
```

---

## Testing Standards

### Use Spock for All Tests

- ✅ Use Spock for new tests
- ❌ Do NOT create new JUnit tests
- 🔄 When updating JUnit tests: Convert to Spock, move to Spec file, DELETE old JUnit test

### Functional Test HTTP Client usage

Always clean up bare OkHttp Response objects to prevent connection leaks.

See [OkHttp Client Response Handling](../rules/okhttp-client-response.md) for details and examples.

---

## Git Commit Messages

### The Golden Rule

Your commit summary should complete this sentence:

> If applied, this commit will ___

**Resources**: [How to Write a Git Commit Message](https://cbea.ms/git-commit/)

---

## API Development

### API Versioning

Rundeck API uses single integer versioning: `$BASE_URL/api/$VERSION/...`

- New behaviors require a new API version
- Bug fixes do NOT require a newer API version

### API Documentation Requirements

Annotate all API methods for OpenAPI Spec. See `.claude/docs/api-guidelines.md` for complete annotation guide.

---

## System Configuration Keys

### Defining New Keys (2 Steps)

#### Step 1: Define Config Type

Modify `com.dtolabs.rundeck.core.config.RundeckConfigBase` in `core/`.

Dot-separated keys map to nested fields: `rundeck.a.b.mykey` → field `a` → field `b` → field `mykey`

#### Step 2: Implement SystemConfigurable

Find appropriate Spring bean and implement `SystemConfigurable` interface.

Return `SysConfigProp` objects in `getSystemConfigProps()` method using `org.rundeck.app.config.SystemConfig` builder.

---

## Summary

**Key Principles**:
- Write clean, documented code
- Use Groovy features effectively (`@CompileStatic`, `@Delegate`, no explicit getters/setters)
- Write tests for everything (Spock)
- Follow API versioning rules
- Write meaningful commit messages

**Resources**:
- [Rundeck Github Guidelines](https://github.com/rundeck/rundeck/wiki/Github-Guidelines)
- [Spock Framework](https://spockframework.org/)
- [Git Commit Guide](https://cbea.ms/git-commit/)