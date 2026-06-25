# API Development Guidelines

Complete guide for developing and documenting APIs in Rundeck.

## Overview

All API endpoints must follow OpenAPI documentation standards. This guide covers:
- How to annotate Grails controllers for OpenAPI spec generation
- Best practices for API design (tags, descriptions, DTOs)
- Step-by-step implementation

### How It Works

**Rundeckapp**: The Rundeck Grails `Application` class is annotated with `@OpenAPIDefinition` as the root definition.

**Controllers**: Each public method in a Grails Controller = URL endpoint. API endpoints MUST start with `/api/$api_version` path.

**Compilation**: The `compileGroovy` task runs Micronaut annotation processor to generate OpenAPI Spec YAML.

**Multiple Plugins**: Each plugin declares a unique "target file" in build.gradle. Rundeckapp combines all plugin specs using the "additional files" flag.

**Output**: After building rundeckapp, specs are in:
```
rundeckapp/build/classes/groovy/main/META-INF/swagger/rundeck-44.yml
```

## Tag Organization

- Use properly capitalized tags defined consistently across the codebase
- Standard tags include: `ACL`, `Calendars`, `Configuration`, `Health`, `Jobs`, `System`, `User`, `Webhook`, etc.
- **Never use lowercase tags** (e.g., use `Jobs` not `jobs`)
- **Only 1 tag per endpoint** - Assign a single, appropriate tag to each endpoint
  - ❌ **Incorrect**: Using multiple tags like `tags = ["Project", "Configuration"]`
  - ✅ **Correct**: Single tag per method — either `@Tag(name = "Project")` or `@Operation(tags = ["Project"])`

### Tag Reference

Use these standardized tags consistently:
- **ACL**: Access Control List operations
- **Calendars**: Calendar management operations
- **Configuration**: Configuration management
- **Health**: Health check operations
- **Jobs**: Job management operations
- **System**: System operations
- **User**: User management operations
- **Webhook**: Webhook operations

## Operation Descriptions

- Provide detailed, developer-friendly descriptions that explain:
  - What the endpoint does and its business purpose
  - Expected input/output behavior
  - Integration context (how it fits into workflows)
  - Any side effects or important considerations
- **Avoid generic descriptions** like "Get data" or "Update resource"
- **Include authorization requirements** and API version information

**Example:**
```groovy
@Operation(
    method = "POST",
    summary = "Generate Job Definition",
    description = """Creates an asynchronous job generation task.
    This endpoint creates job definitions based on provided requirements or templates.
    The task operates asynchronously, allowing clients to poll for completion status.

    Authorization required: `create` for `job` resource type

    Since: v46"""
)
@Tag(name = "Jobs")
```

## Data Transfer Objects (DTOs)

**Use proper Java/Groovy classes (DTOs)** to represent request and response data types instead of inline schema definitions.

### Benefits
- **Type safety**: Compile-time checking of data structures
- **Automatic spec generation**: OpenAPI specs generated directly from code structure
- **Maintainability**: Changes to DTOs automatically update the spec
- **Code quality**: Prevents schema/code drift

### Creating DTO Classes

```groovy
package com.example.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema(description = "Request to create a new user")
class CreateUserRequest {
    @Schema(description = "Username", required = true, example = "johndoe")
    String username

    @Schema(description = "Password", required = true)
    String password

    @Schema(description = "User roles", required = true)
    List<String> roles
}
```

### Referencing DTOs

Use `@Schema(implementation = ClassName)`:

```groovy
@ApiResponse(
    responseCode = '201',
    description = 'User created successfully',
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(implementation = UserCreatedResponse)
    )
)
```

### Best Practices

- ❌ **Avoid**: `schema = @Schema(type = 'object', requiredProperties = ['username', 'password'])`
- ✅ **Use**: `schema = @Schema(implementation = CreateUserRequest)`
- Use `@Body` annotation in controller methods for type-safe binding:
  ```groovy
  def apiCreate(@Body CreateUserRequest request) {
      // Access properties with type safety: request.username, request.password
  }
  ```

## Implementation Guide

### Step 1: build.gradle Dependencies

Add required dependencies using versions centralized in gradle.properties:

```groovy
compileOnly "io.micronaut.openapi:micronaut-openapi:${micronautOpenapiVersion}"
implementation "io.swagger.core.v3:swagger-annotations:${swaggerVersion}"
implementation "io.micronaut:micronaut-http-server:${micronautVersion}"
implementation "io.micronaut:micronaut-inject:${micronautVersion}"
implementation "io.micronaut:micronaut-inject-java:${micronautVersion}"
implementation "io.micronaut:micronaut-inject-groovy:${micronautVersion}"
implementation "io.micronaut:micronaut-core:${micronautVersion}"
```

**For new Grails Plugins**: Specify "target file" for spec generation:

```groovy
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

### Step 2: Controller Annotation

**MUST** annotate Controller class with `@Controller`:

```groovy
@Controller()
class LicenseController {
    // ...
}
```

**Optional**: Specify base path for all endpoints:

```groovy
@Controller(value = "/api/44/projects")
class ProjectController {
    // ...
}
```

**Important**: Without `@Controller`, method annotations won't appear in generated spec!

### Step 3: Method Annotations

Each API endpoint method needs:

#### HTTP Method Annotation (Micronaut)

```groovy
@Get(uri = "/projects", produces = MediaType.APPLICATION_JSON)
@Post(uri = "/projects", produces = MediaType.APPLICATION_JSON)
@Put(uri = "/users/{id}", produces = MediaType.APPLICATION_JSON)
@Delete(uri = "/users/{id}")
```

#### OpenAPI Annotations (Swagger)

```groovy
@Operation(
    method = "GET",
    summary = "View License",
    description = "Returns metadata about the current License"
)
@Tag(name = "System")
@ApiResponse(
    responseCode = "200",
    description = "License info response",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(implementation = LicenseInfoResponse.class)
    )
)
```

### Step 4: Request Body (POST/PUT)

For endpoints with request body:

```groovy
@Operation(
    method = "POST",
    summary = "Create Project",
    description = "Creates a new project",
    requestBody = @RequestBody(
        description = "Project configuration",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ProjectCreateRequest.class)
        ),
        required = true
    )
)
@Tag(name = "Project")
@ApiResponse(
    responseCode = "201",
    description = "Project created",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(implementation = ProjectResponse.class)
    )
)
@Post(uri = "/projects", produces = MediaType.APPLICATION_JSON)
def create(@Body ProjectCreateRequest request) {
    // implementation
}
```

### Step 5: Query and Path Parameters

Use `@Parameters` annotation:

```groovy
@Operation(
    method = "GET",
    summary = "Get Project Info"
)
@Tag(name = "Project")
@Parameters([
    @Parameter(
        name = "project",
        in = ParameterIn.PATH,
        description = "Project name",
        required = true,
        schema = @Schema(type = "string")
    ),
    @Parameter(
        name = "includeDetails",
        in = ParameterIn.QUERY,
        description = "Include detailed information",
        schema = @Schema(type = "boolean", defaultValue = "false")
    )
])
@Get(uri = "/project/{project}")
def getProject() {
    // implementation
}
```

## Complete Example

```groovy
@Controller(value = "/api/44")
class ProjectController {
    
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
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ProjectListResponse.class)
        )
    )
    @Get(uri = "/projects", produces = MediaType.APPLICATION_JSON)
    def list() {
        // implementation
    }
    
    @Operation(
        method = "POST",
        summary = "Create Project",
        description = "Creates a new project with the specified configuration",
        requestBody = @RequestBody(
            description = "Project configuration",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ProjectCreateRequest.class)
            ),
            required = true
        )
    )
    @Tag(name = "Project")
    @ApiResponse(
        responseCode = "201",
        description = "Project created successfully",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ProjectResponse.class)
        )
    )
    @Post(uri = "/projects", produces = MediaType.APPLICATION_JSON)
    def create(@Body ProjectCreateRequest request) {
        // implementation
    }
}
```

## Annotations Reference

### Controller Level
- `@Controller(value = "/base/path")` - Micronaut controller with optional base path

### Method Level
- `@Get(uri = "/path")` - GET endpoint
- `@Post(uri = "/path")` - POST endpoint
- `@Put(uri = "/path")` - PUT endpoint
- `@Delete(uri = "/path")` - DELETE endpoint
- `@Operation(...)` - OpenAPI operation details
- `@Tag(name = "category")` - Group endpoints
- `@ApiResponse(...)` - Response definition
- `@Parameters([...])` - Query/path parameters

### Data Types
- `@Schema(...)` - Define data structure
- `@Parameter(...)` - Define parameter
- `@RequestBody(...)` - Define request body
- `@Content(...)` - Define content type
- `@ExampleObject(...)` - Example value

## Implementation Checklist

For the complete API endpoint checklist, see **`.claude/skills/create-api-endpoint/SKILL.md`**.

## Internationalization

Where possible, use i18n message code references instead of raw English text.

### Vue UIs

Use the vue-i18n plugin, and include localized text in appropriate files:
- `en_US.js` (English)
- `es_419.js` (Spanish)
- Other locale files as needed

### Grails Code

For Controllers, Services, etc., use Spring `messageSource` bean to look up i18n messages.

Define messages in:
- `messages.properties` (default English)
- `messages_es_419.properties` (Spanish)
- Other localized variants

## Resources

- **Reference Plugin**: [rundeck-ec2-nodes-plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin)
- **Micronaut OpenAPI**: [Documentation](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/)
- **Swagger Annotations**: [Wiki](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
- **Testing**: `.claude/docs/testing-guidelines.md` - API testing requirements
- **Code Conventions**: See CLAUDE.md for compilation mode, documentation requirements
