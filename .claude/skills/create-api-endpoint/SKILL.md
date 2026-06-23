---
name: create-api-endpoint
description: Create REST API endpoints with proper OpenAPI annotations, API versioning, and testing following Rundeck standards.
---

# Create API Endpoint Skill

## When to Use

- Creating new REST API endpoints
- Modifying existing API endpoints  
- Need OpenAPI spec annotations
- API versioning requirements

## Process

### Phase 1: Load Context

**Automatically read:**
- `.claude/docs/api-guidelines.md` - Complete annotation guide
- `.claude/docs/development-guidelines.md` - API versioning, documentation
- `.claude/docs/testing-guidelines.md` - API testing requirements

### Phase 2: Design API

**Define**:
- HTTP method (GET/POST/PUT/DELETE)
- URL path (must start with `/api/$VERSION/...`)
- Request/Response DTOs
- **API version required** (increment Current Version if new behavior)
- Authentication requirements

**API Versioning Rules**:
- New functionality → New API version
- New endpoint → New API version
- Modified endpoint → New API version
- Bug fix → Same API version

### Phase 3: Write API Tests First

**Test Requirements**:
1. ✅ New behavior works with NEW API version
2. ❌ New behavior does NOT work with OLD API version
3. ✅ Error conditions return correct responses
4. ✅ Success conditions work in all call patterns

**Example Test**:
```groovy
def "should return projects for API v44"() {
    when:
    def response = client.get("/api/44/projects")
    
    then:
    response.status == 200
    response.json.size() > 0
}

def "should reject for API v43"() {
    when:
    def response = client.get("/api/43/projects")
    
    then:
    response.status == 404 // or appropriate error
}
```

### Phase 4: Create/Update DTOs

**Annotate Data Classes**:
```groovy
@Schema(description = "Project response")
class ProjectResponse {
    
    @Schema(description = "Project name", example = "MyProject")
    String name
    
    @Schema(description = "Project description")
    String description
    
    @Schema(description = "Creation date", example = "2025-01-01T00:00:00Z")
    String created
}
```

### Phase 5: Create/Update Controller

**Step 1: Add @Controller Annotation**:
```groovy
@Controller(value = "/api/44")
class ProjectController {
    // ...
}
```

**Step 2: Annotate Method**:
```groovy
@Operation(
    method = "GET",
    summary = "List Projects",
    description = "Returns a list of all projects accessible to the user"
)
@Tag(name = "Project")
@ApiResponse(
    responseCode = "200",
    description = "Project list successfully retrieved",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProjectListResponse.class)
    )
)
@Get(uri = "/projects", produces = MediaType.APPLICATION_JSON)
def listProjects() {
    // implementation
}
```

**For POST/PUT with Request Body**:
```groovy
@Operation(
    method = "POST",
    summary = "Create Project",
    description = "Creates a new project with the specified configuration",
    requestBody = @RequestBody(
        description = "Project configuration",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProjectCreateRequest.class)
        ),
        required = true
    )
)
@ApiResponse(
    responseCode = "201",
    description = "Project created successfully",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProjectResponse.class)
    )
)
@Post(uri = "/projects")
def createProject() {
    // implementation
}
```

**For Query/Path Parameters**:
```groovy
@Parameters([
    @Parameter(
        name = "project",
        in = ParameterIn.PATH,
        description = "Project name",
        required = true,
        schema = @Schema(type = "string")
    ),
    @Parameter(
        name = "includeArchived",
        in = ParameterIn.QUERY,
        description = "Include archived items",
        schema = @Schema(type = "boolean", defaultValue = "false")
    )
])
@Get(uri = "/project/{project}/jobs")
def getProjectJobs() {
    // implementation
}
```

### Phase 6: Update build.gradle (If New Plugin)

```groovy
// Add dependencies using versions centralized in gradle.properties
compileOnly "io.micronaut.openapi:micronaut-openapi:${micronautOpenapiVersion}"
implementation "io.swagger.core.v3:swagger-annotations:${swaggerVersion}"

// Set target file
tasks.withType(GroovyCompile) {
    def target = new File(
        project.rootDir, 
        "rundeckapp/build/openapi/${project.name}.yml"
    ).absolutePath
    configure(groovyOptions) {
        forkOptions.jvmArgs = [
            '-Xmx1024m',
            "-Dmicronaut.openapi.target.file=${target}".toString()
        ]
    }
}
```

### Phase 7: Run Tests and Verify

```bash
# API tests
./gradlew :functional-test:apiTest

# Verify OpenAPI spec generated
ls rundeckapp/build/classes/groovy/main/META-INF/swagger/rundeck-*.yml

# Full test suite
./gradlew test
```

### Phase 8: Update Documentation

1. **Rundeck Docs**: Update [API Reference](https://docs.rundeck.com/docs/api/)
2. **API Version History**: Document changes in [API Version History](https://docs.rundeck.com/docs/api/rundeck-api-versions.html)
3. **If new API version**: Document what was added/changed

---

## Required Annotations Summary

### Controller Level
```groovy
@Controller(value = "/api/44/base-path")  // Required
```

### Method Level
```groovy
@Get|@Post|@Put|@Delete(uri = "/path")    // Required - HTTP method
@Operation(...)                             // Required - OpenAPI operation
@Tag(name = "Category")                     // Required - Grouping
@ApiResponse(...)                           // Required - Response definition
@Parameters([...])                          // Optional - Query/path params
```

### Data Types
```groovy
@Schema(...)                                // Required on DTO classes
```

---

## Checklist

**Design:**
- [ ] API design reviewed (HTTP method, path, versioning)
- [ ] API version incremented for new functionality

**build.gradle:**
- [ ] Micronaut/Swagger dependencies added
- [ ] Target file configured for plugin (if new plugin)

**Controller:**
- [ ] Annotated with `@Controller`
- [ ] Each method has `@Get/@Post/@Put/@Delete`
- [ ] Each method has `@Operation` with detailed description
- [ ] Each method has `@Tag`
- [ ] Each method has `@ApiResponse`
- [ ] Request body specified for POST/PUT
- [ ] Parameters specified if needed

**Data Types:**
- [ ] DTO classes annotated with `@Schema`
- [ ] DTO fields annotated with `@Schema`

**Testing:**
- [ ] API tests written first (TDD)
- [ ] Tests verify new API version works
- [ ] Tests verify old API version does NOT support new behavior
- [ ] API tests pass

**Verification:**
- [ ] Build and verify YAML generated in `rundeckapp/build/classes/groovy/main/META-INF/swagger/`
- [ ] OpenAPI spec includes new endpoints
- [ ] Rundeck documentation updated

---

## Common Mistakes to Avoid

❌ **Don't**:
- Forget `@Controller` annotation (methods won't appear in spec!)
- Use old API version for new functionality
- Skip API version tests
- Omit `@Schema` on DTOs
- Forget to update Rundeck docs

✅ **Do**:
- Always use `@Controller`
- Increment API version for new behavior
- Test both old and new API versions
- Annotate all DTOs completely
- Update official documentation

---

## Resources

- `.claude/docs/api-guidelines.md` - Complete annotation guide
- `.claude/docs/development-guidelines.md` - API versioning
- **Reference Plugin**: [rundeck-ec2-nodes-plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin)
- [Micronaut OpenAPI](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/)
- [Swagger Annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
