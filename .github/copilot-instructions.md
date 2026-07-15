These best practices should be applied:

# Product Naming Guidelines

Always use the correct product names in descriptions, documentation, and API specifications:

- **Open Source Product**: "Rundeck"
- **Commercial Product**: "Runbook Automation"
- **Combined Reference**: "Rundeck / Runbook Automation" (preferred for API titles, descriptions, and user-facing text)

## Examples:
- ✅ **Correct**: "Rundeck / Runbook Automation API", "Rundeck / Runbook Automation Server"
- ❌ **Incorrect**: "Rundeck Pro API", "Rundeck Pro Server"

## Technical Elements (keep as-is):
- API headers: `X-Rundeck-Auth-Token` (maintain for compatibility)
- Internal identifiers: `rundeckApiToken`, `rundeckAuth` (technical references)
- Documentation URLs: `docs.rundeck.com` (legitimate technical URLs)

## API Documentation Specifics

### API Endpoint Descriptions:
- No need to include product names ("Rundeck" or "Runbook Automation") in API endpoint descriptions
- API consumers are already using the product API - product names are redundant and create unnecessary noise
- Focus descriptions on functionality, authorization requirements, and behavior
- Example: ✅ "Creates a native/local user with a password for use with the built-in authentication system"
- Example: ❌ "Creates a native/local Rundeck user..." or "Creates a Runbook Automation user..."

### Commercial-Only Features:
- Mark commercial-only endpoints with `[Enterprise]` tag in the summary
- Example: `summary = 'Create A Local User [Enterprise]'`

This ensures consistent branding across all user-facing elements while maintaining technical compatibility.

# All Java and Groovy Code (general instructions)

New or modified code should include appropriate Javadoc or Groovydoc comments. The comments should explain the purposes of the classes, methods, etc.

# Groovy code

Groovy code should use the `@CompileStatic` (or `@GrailsCompileStatic` for Grails aretefacts like Controllers, Services, etc.) on all classes.  
If some methods of a class require using Groovy dynamic typing, the `@CompileDynamic` annotation can be applied to those methods, but it is preferable if possible to convert them to use static typing.
If a class was already not using `@CompileStatic`, and the PR only changes/adds a few methods, then the `@CompileDynamic` should be added to those methods where possible.

Groovy classes do not need to explicitly implement getters and setters, like Java, so avoid that if possible.

# Testing

Changes to most groovy and Java code should be accompanied by updated or added Unit tests using the Spock testing framework.

If existing Junit tests need to be modified, it is preferred to convert them into Spock tests if possible.

Changes to front-end code (javascript, and typescript) should have accompanying Jest unit tests.

Changes that add or significantly change features of the application user interface, should have Selenium based tests added in the pro-functional-test module.

## Selenium Testing Guidelines

Selenium tests must follow the Page Object Model pattern and these strict guidelines:

### Wait Strategies
- **AVOID using `Thread.sleep()`** - Prefer explicit waits when possible. Only use `Thread.sleep()` with `WaitingTime` constants for special cases like external system initialization where explicit waits cannot be used.
- **Implicit waits are globally configured** - The framework sets a global implicit wait in the `BasePage` constructor and uses `implicitlyWait(2000)` in `go()` methods. This is an established pattern. However, for specific element interactions, **ALWAYS prefer explicit waits** to ensure reliability and avoid unpredictable behavior.
- **ALWAYS use explicit waits** provided by the Page Object base classes:
  - `waitForElementVisible()` - Wait for element to be present AND visible
  - `waitForElementToBeClickable()` - Wait for element to be visible AND enabled
  - `waitForElementAttributeToChange()` - Wait for specific attribute changes
  - `waitForNumberOfElementsToBeMoreThan()` - Wait for multiple elements to be present
  - `waitForNumberOfElementsToBe()` - Wait for the number of elements to match a specific count
  - `waitForTextToBePresentInElement()` - Wait for specific text to appear in an element
  - `waitForUrlToContain()` - Wait for the current URL to contain a specific substring
  - `waitForAttributeContains()` - Wait for an element's attribute to contain a specific value
  - `waitIgnoringForElementVisible()` - Wait for element to be visible, ignoring certain exceptions
  - `waitIgnoringForElementToBeClickable()` - Wait for element to be clickable, ignoring certain exceptions

### Page Object Model
- **NEVER put CSS/XPath selectors directly in test files** - All selectors belong in Page Object classes
- **Define selectors as `By` fields** at the top of Page Object classes:
  ```groovy
  By nodeDetailsTableBy = By.cssSelector(".popover-content .node-details-simple")
  By parameterKeyBy = By.cssSelector(".key")
  ```
- **Create getter methods** that include appropriate waits:
  ```groovy
  WebElement getNodeDetailsTable() {
      waitForPopoverToAppear()
      waitForElementVisible nodeDetailsTableBy
      el nodeDetailsTableBy
  }
  ```
- **Encapsulate interactions** - Complex operations should be methods in Page Objects, not inline in tests

### Test Structure
- Tests should read like documentation - clear when/then/expect blocks
- Document methods with Groovydoc explaining purpose and behavior

### Examples
✅ **Correct**:
```groovy
// In CommandPage.groovy
By nodeDetailsTableBy = By.cssSelector(".popover-content .node-details-simple")

WebElement getNodeDetailsTable() {
    waitForPopoverToAppear()
    waitForElementVisible nodeDetailsTableBy
    el nodeDetailsTableBy
}

// In CommandSpec.groovy
def nodeDetailsTable = commandPage.getNodeDetailsTable()
assert nodeDetailsTable.isDisplayed()
```

❌ **Incorrect**:
```groovy
// In test file - DON'T DO THIS
Thread.sleep(2000)  // Never use sleep!
def table = driver.findElement(By.cssSelector(".popover-content .node-details-simple"))  // Selector in test!
```

# APIs

Changes to APIs should be documented with appropriate OpenAPI Spec annotations on the Grails controller actions that implement the API endpoints.

## OpenAPI Specification Guidelines

All API endpoints must follow these OpenAPI documentation standards:

### Tag Organization
- Use properly capitalized tags from the global definitions in `rundeckpro-grails-common/grails-app/init/rundeckpro/common/Application.groovy`
- Standard tags include: `ACL`, `Calendars`, `Configuration`, `Enterprise`, `GenAI`, `Health`, `Jobs`, `Runner`, `System`, `User`, `Webhook`, etc.
- **Never use lowercase tags** (e.g., use `Jobs` not `jobs`, `GenAI` not `genai`)
- **Only 1 tag per endpoint** - Assign a single, appropriate tag to each endpoint for clean OpenAPI spec and SDK file generation
  - ❌ **Incorrect**: Using multiple tags like `tags = ["Project", "Configuration"]` or duplicate tag annotations
  - ✅ **Correct**: Single tag in the `@Operation` annotation: `tags = ["Project"]`
  - **Do not use separate `@Tag` annotations** - The tag should only be specified in the `@Operation` annotation

### Operation Descriptions
- Provide detailed, developer-friendly descriptions that explain:
  - What the endpoint does and its business purpose
  - Expected input/output behavior
  - Integration context (how it fits into workflows)
  - Any side effects or important considerations
- **Avoid generic descriptions** like "Get data" or "Update resource"
- **Include authorization requirements** and API version information

### Examples
```groovy
@Operation(
    method = "POST",
    summary = "Generate Job Using AI [Enterprise]",
    description = """Creates an asynchronous job generation task using AI/ML capabilities. 
    This endpoint leverages generative AI to automatically create job definitions based on 
    provided requirements, descriptions, or templates. The task operates asynchronously, 
    allowing clients to poll for completion status and retrieve the generated job 
    configuration when ready.
    
    Authorization required: `create` for `job` resource type
    
    Since: v46""",
    tags = ['GenAI']
)
```

### Tag Reference
Use these standardized tags consistently:
- **ACL**: Access Control List operations
- **Calendars**: Calendar management operations  
- **Configuration**: Configuration management
- **Enterprise**: Enterprise license and feature operations
- **GenAI**: Generative AI operations
- **Health**: Health check operations
- **Jobs**: Job management operations
- **Runner**: Runner management operations
- **System**: System operations
- **User**: User management operations
- **Webhook**: Webhook operations

### Data Transfer Objects (DTOs) for API Endpoints

When documenting API endpoints with OpenAPI annotations, **use proper Java/Groovy classes (DTOs)** to represent request and response data types instead of inline schema definitions. This provides:

- **Type safety**: Compile-time checking of data structures
- **Automatic spec generation**: OpenAPI specs generated directly from code structure
- **Maintainability**: Changes to DTOs automatically update the spec
- **Code quality**: Prevents schema/code drift

#### Best Practices

1. **Create DTO classes** for requests and responses:
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

2. **Reference DTOs in OpenAPI annotations** using `@Schema(implementation = ClassName)`:
   ```groovy
   @Put(uri='users/create')
   @Operation(
       method = 'PUT',
       summary = 'Create A User',
       description = '''Creates a new user account...''',
       tags = ['User'],
       requestBody = @RequestBody(
           required = true,
           content = @Content(
               mediaType = MediaType.APPLICATION_JSON,
               schema = @Schema(implementation = CreateUserRequest)  // Reference the DTO class
           )
       ),
       responses = [
           @ApiResponse(
               responseCode = '201',
               description = 'User created successfully',
               content = @Content(
                   mediaType = MediaType.APPLICATION_JSON,
                   schema = @Schema(implementation = UserCreatedResponse)  // Reference the DTO class
               )
           )
       ]
   )
   def apiCreate(@Body CreateUserRequest request) {  // Type-safe parameter
       // Controller logic here
   }
   ```

3. **Avoid inline schema definitions** when dealing with structured request/response data:
   - ❌ **Incorrect**: `schema = @Schema(type = 'object', requiredProperties = ['username', 'password'])`
   - ✅ **Correct**: `schema = @Schema(implementation = CreateUserRequest)`

4. **Use `@Body` annotation** in controller methods to bind DTOs:
   ```groovy
   def apiCreate(@Body CreateUserRequest request) {
       // Access properties with type safety: request.username, request.password
   }
   ```

5. **Convert DTOs to service layer formats** if services use different structures:
   ```groovy
   class CreateUserRequest {
       String username
       String password
       
       Map<String, Object> toMap() {
           [username: username, password: password]
       }
   }
   ```

This approach ensures that API documentation, controller logic, and data structures remain synchronized and maintainable.

# Internationalization

Where possible, instead of raw English text, appropriate i18n message code references should be used instead.

For Vue UIs, use the vue-i18n plugin, and include the localized text in the appropriate localization file, such as "en_US.js" or "es_419.js".

For Grails code, such as Controllers, Services, etc, the spring "messageSource" bean can be used to look up i18n messages. These messages should be defined in the "messages.properties" or localized equivalent such as "messages_es_419.properties"
