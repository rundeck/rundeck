---
name: create-code
description: Generate Java/Groovy/TypeScript code following Rundeck development standards. Auto-loads development guidelines, formatting rules, and testing requirements.
---

# Create Code Skill

## When to Use

- Creating new Java classes or Groovy services
- Adding features to existing modules
- Writing TypeScript/Vue components
- Need to follow Rundeck code standards
- Implementing new functionality with proper tests

## Process

### Phase 1: Load Context

**Automatically read these documentation files:**
- `.claude/docs/development-guidelines.md` - Git commits, Groovy patterns, System Config, APIs
- `.claude/docs/code-formatting.md` - Spotless rules, IDE setup
- `.claude/docs/testing-guidelines.md` - Complete testing guide (Unit, API, UI, Functional)

### Phase 2: Understand Requirements

Ask yourself:
- **What is being created?** (Class, Service, Controller, Component)
- **What module does it belong to?** (core, plugin, frontend)
- **What are the dependencies?**
- **What behavior needs to be tested?**

### Phase 3: Write Test First

✅ **ALWAYS** write tests BEFORE implementation

**For Groovy/Java**:
- Use Spock framework
- Create `*Spec.groovy` test file
- Use `given/when/then` blocks
- Test should FAIL before implementation

**For TypeScript/Vue**:
- Use Jest + Vue Test Utils
- Create `Component.spec.ts` next to component
- Test component behavior and interactions

**Example Spock Test**:
```groovy
class UserServiceSpec extends Specification {
    
    def userService = new UserService()
    
    def "should return user when valid ID provided"() {
        given: "a valid user ID"
        def userId = 123
        
        when: "finding user by ID"
        def user = userService.findById(userId)
        
        then: "user is returned with correct ID"
        user != null
        user.id == userId
    }
    
    def "should throw exception when user not found"() {
        given: "an invalid user ID"
        def userId = 999
        
        when: "finding user by ID"
        userService.findById(userId)
        
        then: "exception is thrown"
        thrown(UserNotFoundException)
    }
}
```

### Phase 4: Implement Code

#### Groovy/Java Standards

**Use @CompileStatic**:
```groovy
@CompileStatic
class MyService {
    // your code
}
```

- Use `@GrailsCompileStatic` for Grails components (Controllers, Services)
- Use `@CompileDynamic` ONLY on specific methods that can't use static typing

**Don't Implement Property Getters/Setters**:
```groovy
// Good - Groovy auto-generates getters/setters
class User {
    String name
    int age
}

// Bad - unnecessary boilerplate
class User {
    private String name
    String getName() { return name }
    void setName(String n) { name = n }
}
```

**Use @Delegate When Appropriate**:
```groovy
class MyService implements SomeInterface {
    @Delegate
    SomeInterface wrapped
    // No need to implement SomeInterface methods
}
```

**Avoid Commented-Out Code**:
- Delete it, don't comment it
- Explain WHY in commit message
- Version control preserves history

#### TypeScript/Vue Standards

- Follow existing patterns in codebase
- Use TypeScript types (no `any`)
- Component props should be typed
- Emit events with proper typing

### Phase 5: Format and Validate

**Format Code**:
```bash
# Fix formatting
./gradlew spotlessApply
```

**Verify Build**:
```bash
# Quick verification (compilation + basic checks)
./gradlew verifyBuild

# Or full clean build (skips tests)
./gradlew clean && ./gradlew build -x check
```

**Frontend Tests** (if applicable):
```bash
# Core UI
CORE_UI=rundeckapp/grails-spa/packages/ui-trellis
npm run --prefix "$CORE_UI" ci:test:unit
```

**Note**: Run the full test suite (`./gradlew test`) locally before considering work complete. CI will also run the complete test suite as an additional verification step.

### Phase 6: Write Meaningful Commit Message

Your commit should complete: "If applied, this commit will ___"

**Structure**:
```
Short summary (50 chars or less)

Detailed explanation of what and why.
- Why this change was needed
- What the context was
- Any important details
```

**Resources**: [How to Write a Git Commit Message](https://cbea.ms/git-commit/)

---

## System Configuration Keys

If creating new System Configuration keys, follow **2-step process**:

### Step 1: Define Config Type

**Core**: `com.dtolabs.rundeck.core.config.RundeckConfigBase`

Add nested field structure for your key (e.g., `rundeck.a.b.mykey`)

### Step 2: Implement SystemConfigurable

Find appropriate Spring bean and implement `SystemConfigurable` interface

Return `SysConfigProp` objects in `getSystemConfigProps()` method

Use `org.rundeck.app.config.SystemConfig` builder:

```groovy
SystemConfig.builder()
    .key("rundeck.feature.myfeature.enabled")
    .label("My Feature Enabled")
    .datatype("Boolean")
    .defaultValue("false")
    .authRequired("app_admin")  // or "ops_admin"
    .build()
```

**See**: `.claude/docs/development-guidelines.md` for complete details

---

## Checklist

Before considering code complete:

- [ ] Tests written first
- [ ] Tests fail before implementation
- [ ] Implementation follows Groovy/Java standards
- [ ] `@CompileStatic` used (or `@GrailsCompileStatic` for Grails)
- [ ] No commented-out code
- [ ] Code formatted (`./gradlew spotlessApply`)
- [ ] Build verified (`./gradlew verifyBuild` or `./gradlew clean && ./gradlew build -x check`)
- [ ] Meaningful commit message written

---

## Examples

### Example 1: Simple Service Class

```groovy
@CompileStatic
class ProjectService {
    
    List<Project> listProjects() {
        // implementation
    }
    
    Project getProject(String name) {
        // implementation
    }
}
```

**Test**:
```groovy
class ProjectServiceSpec extends Specification {
    
    def projectService = new ProjectService()
    
    def "should list all projects"() {
        when:
        def projects = projectService.listProjects()
        
        then:
        projects != null
        projects.size() >= 0
    }
}
```

### Example 2: Grails Controller

```groovy
@GrailsCompileStatic
class ProjectController {
    
    ProjectService projectService
    
    def index() {
        def projects = projectService.listProjects()
        respond projects
    }
}
```

### Example 3: Using @Delegate

```groovy
@CompileStatic
class CachedProjectService implements ProjectService {
    
    @Delegate
    ProjectService delegate
    
    private Cache cache
    
    // Override specific methods that need caching
    @Override
    Project getProject(String name) {
        cache.get(name) ?: delegate.getProject(name)
    }
}
```

---

## Common Mistakes to Avoid

❌ **Don't**:
- Write implementation before tests
- Use `@CompileDynamic` on entire class
- Commit commented-out code
- Implement unnecessary getters/setters in Groovy
- Write vague commit messages

✅ **Do**:
- Write failing test first
- Use `@CompileStatic` by default
- Delete unused code (explain in commit)
- Use Groovy properties
- Write meaningful commit messages

---

## Integration with Other Skills

- **API Endpoints**: Use `create-api-endpoint` skill for REST controllers
- **Plugins**: Use `create-plugin` skill for bundled plugins
- **Testing Only**: Use `create-test` skill for comprehensive test scenarios

---

## Resources

- `.claude/docs/development-guidelines.md` - Complete guidelines
- `.claude/docs/code-formatting.md` - Spotless configuration
- `.claude/docs/testing-guidelines.md` - Testing guide
- [Spock Framework](https://spockframework.org/)
- [Git Commit Guide](https://cbea.ms/git-commit/)
