---
name: create-plugin
description: Create bundled Rundeck plugins following development standards. Auto-loads plugin development guidelines, build configuration, and release processes.
---

# Create Plugin Skill

## When to Use

- Creating new bundled plugins for Rundeck
- Adding plugin functionality to existing plugins
- Implementing Workflow Steps, Node Steps, Resource Model Sources
- Creating notification or logging plugins

## Process

### Phase 1: Load Context

**Automatically read:**
- `.claude/docs/plugin-development.md` - Complete plugin guide
- `.claude/docs/development-guidelines.md` - Code standards (@CompileStatic, Git, etc.)
- `.claude/docs/testing-guidelines.md` - Plugin testing requirements

### Phase 2: Design Plugin

**Determine**:

**1. Plugin Type**
- **WorkflowStep** - Job step executed in workflow
- **NodeStep** - Step executed on specific nodes
- **ResourceModelSource** - Node source provider
- **Notification** - Job notification handler
- **LogFilter** - Log processing plugin
- **StorageConverter** - Storage backend plugin
- **WorkflowNodeStep** - Workflow step that runs on nodes

**2. Service Name**
Use appropriate service name from `ServiceNameConstants`:
- `WorkflowStep` - "WorkflowStep"
- `WorkflowNodeStep` - "WorkflowNodeStep"  
- `NodeExecutor` - "NodeExecutor"
- `ResourceModelSource` - "ResourceModelSource"
- `Notification` - "Notification"
- `LogFilter` - "LogFilterPlugin"
- `StorageConverter` - "StorageConverter"

**3. Plugin Properties**
- What configuration does the plugin need?
- Which properties are required vs optional?
- What are the default values?

### Phase 3: Create Plugin Structure

#### Step 1: Create build.gradle

**Basic Plugin build.gradle**:
```groovy
plugins {
    id 'groovy'
    id 'java-library'
}

// Apply shared plugin configuration
apply from: "${rootProject.projectDir}/gradle/java-plugin.gradle"

group = 'com.rundeck.plugins'
version = '1.0.0'

dependencies {
    // Rundeck core dependencies
    compileOnly "org.rundeck:rundeck-core:${rundeckVersion}"
    
    // Plugin-specific dependencies (use version properties from gradle.properties)
    implementation "com.example:library:${exampleLibraryVersion}"
    
    // Testing
    testImplementation "org.spockframework:spock-core:${spockVersion}"
}

jar {
    manifest {
        attributes(
            'Rundeck-Plugin-Version': '1.2',
            'Rundeck-Plugin-Archive': 'true',
            'Rundeck-Plugin-Classnames': 'com.example.MyPlugin',
            'Rundeck-Plugin-Name': 'my-plugin',
            'Rundeck-Plugin-Description': 'My awesome plugin'
        )
    }
}

// Copy plugin jar to output directory
task copyToLib(type: Copy) {
    from jar
    into "${rootProject.projectDir}/rundeckapp/src/main/resources/WEB-INF/rundeck/plugins"
}

build.finalizedBy copyToLib
```

**Gradle Version Catalog**:
```toml
# gradle/libs.versions.toml
[versions]
rundeck = "5.4.0"
aws-sdk = "1.12.572"
spock = "2.3-groovy-3.0"

[libraries]
rundeck-core = { module = "org.rundeck:rundeck-core", version.ref = "rundeck" }
aws-s3 = { module = "com.amazonaws:aws-java-sdk-s3", version.ref = "aws-sdk" }
spock-core = { module = "org.spockframework:spock-core", version.ref = "spock" }
```

#### Step 2: Create Plugin Class

**Annotate with @Plugin**:
```groovy
import com.dtolabs.rundeck.plugins.descriptions.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope

@Plugin(name = "my-plugin", service = "WorkflowStep")
@PluginDescription(title = "My Plugin", description = "Does awesome things")
class MyPlugin implements StepPlugin {
    
    @PluginProperty(
        title = "API Key",
        description = "Your API key",
        required = true,
        scope = PropertyScope.Instance
    )
    String apiKey
    
    @PluginProperty(
        title = "Timeout",
        description = "Request timeout in seconds",
        defaultValue = "30"
    )
    Integer timeout
    
    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) {
        // Plugin implementation
        def result = doWork(apiKey, timeout)
        context.getLogger().log(2, "Result: ${result}")
    }
}
```

**Common Annotations**:
```groovy
// Basic plugin definition
@Plugin(name = "plugin-name", service = "ServiceName")

// Description
@PluginDescription(title = "Display Title", description = "What it does")

// Properties
@PluginProperty(
    title = "Property Title",
    description = "What this property does",
    required = false,
    defaultValue = "default",
    scope = PropertyScope.Instance
)

// Rendering options (UI)
@RenderingOptions({
    @RenderingOption(key = "displayType", value = "PASSWORD"),
    @RenderingOption(key = "selectionAccessor", value = "STORAGE_PATH")
})

// Select values
@SelectValues(values = ["option1", "option2", "option3"])
```

### Phase 4: Write Plugin Tests

**Test plugin properties**:
```groovy
class MyPluginSpec extends Specification {
    
    def "should have correct plugin annotations"() {
        given:
        def plugin = MyPlugin.class.getAnnotation(Plugin)
        
        expect:
        plugin.name() == "my-plugin"
        plugin.service() == "WorkflowStep"
    }
    
    def "should have required properties"() {
        given:
        def fields = MyPlugin.declaredFields
        def apiKeyField = fields.find { it.name == "apiKey" }
        def property = apiKeyField.getAnnotation(PluginProperty)
        
        expect:
        property.required() == true
        property.title() == "API Key"
    }
    
    def "should execute successfully with valid config"() {
        given:
        def plugin = new MyPlugin()
        def context = Mock(PluginStepContext)
        def config = [apiKey: "test-key", timeout: 30]
        
        when:
        plugin.executeStep(context, config)
        
        then:
        notThrown(Exception)
        1 * context.getLogger() >> Mock(PluginLogger)
    }
}
```

### Phase 5: Build and Test Plugin

```bash
# Build plugin
./gradlew :plugins:my-plugin:build

# Run tests
./gradlew :plugins:my-plugin:test

# Copy to rundeck plugins directory
./gradlew :plugins:my-plugin:copyToLib

# Check plugin was copied
ls rundeckapp/src/main/resources/WEB-INF/rundeck/plugins/
```

### Phase 6: Documentation

**README.md** following [Howto CLI](https://github.com/rundeck/howto-cli-tool) conventions:

```markdown
# My Plugin

Description of what the plugin does

## How to Build

\`\`\`bash
./gradlew build
\`\`\`

## How to Test

\`\`\`bash
./gradlew test
\`\`\`

## How to Install

\`\`\`bash
cp build/libs/my-plugin-1.0.0.jar $RDECK_BASE/libext/
\`\`\`

## How to Use

1. Create a job
2. Add "My Plugin" step
3. Configure API Key
4. Run the job

## Configuration

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| API Key  | Yes      | -       | Your API key |
| Timeout  | No       | 30      | Timeout in seconds |
```

### Phase 7: Setup CI/CD (Optional, for standalone repos)

**GitHub Actions**: `./github/workflows/build.yml`
```yaml
name: Build

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Upload artifact
      uses: actions/upload-artifact@v3
      with:
        name: plugin-jar
        path: build/libs/*.jar
```

**Release Workflow**: `.github/workflows/release.yml`
```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Create Release
      run: |
        gh release create \
          --generate-notes \
          --title 'Release ${{ github.ref_name }}' \
          ${{ github.ref_name }} \
          build/libs/*.jar
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Phase 8: Publishing (Optional, for open source)

**Maven Central** (Preferred over JitPack):

**Requirements**:
1. Apply `nexusPublish` gradle plugin
2. Configure `nexusPublishing` block
3. Add `withJavadocJar()` and `withSourcesJar()`
4. Fix javadoc issues
5. Configure Github secrets (SIGNING_KEY_B64, SIGNING_PASSWORD, SONATYPE_USERNAME, SONATYPE_PASSWORD)

**See**: `.claude/docs/plugin-development.md` for complete publishing setup

---

## Checklist

### Plugin Development Checklist

Before considering plugin complete:

**Plugin Design:**
- [ ] Plugin type selected and service name correct
- [ ] Plugin properties identified (required vs optional)

**Build Configuration:**
- [ ] build.gradle configured with correct dependencies
- [ ] Version properties used from gradle.properties (no hardcoded versions)
- [ ] Manifest attributes set correctly in jar task
- [ ] copyToLib task configured (if internal plugin)
- [ ] Gradle Version Catalog used for dependencies (if standalone)

**Plugin Implementation:**
- [ ] Plugin class created with @Plugin annotation
- [ ] Plugin properties defined with @PluginProperty
- [ ] @CompileStatic applied to plugin class
- [ ] Javadoc/Groovydoc comments added
- [ ] Error handling implemented properly
- [ ] Logging added using PluginLogger

**Testing:**
- [ ] Tests written for plugin functionality
- [ ] Tests written for plugin properties/annotations
- [ ] Tests written for error conditions
- [ ] All tests pass (`./gradlew test`)
- [ ] Plugin builds successfully (`./gradlew build`)
- [ ] Plugin jar copied to correct location

**Documentation:**
- [ ] README documentation created (Howto CLI format)
- [ ] Configuration properties documented in table
- [ ] Usage examples provided
- [ ] Build/Test/Install instructions included

**Repository Setup (if standalone repo):**
- [ ] Gradle with wrapper
- [ ] Java 17
- [ ] GitHub Actions (build.yml + release.yml)
- [ ] Gradle Version Catalog for dependencies
- [ ] Renovate bot enabled
- [ ] Howto documentation in README

**Publishing (if standalone, open source):**
- [ ] GitHub secrets configured (SIGNING_KEY_B64, SIGNING_PASSWORD, SONATYPE_USERNAME, SONATYPE_PASSWORD)
- [ ] Javadoc fixed (no errors)
- [ ] Release workflow tested
- [ ] Maven Central publishing verified

---

## Examples

Real-world plugin examples:
- [rundeck-ec2-nodes-plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin) - Resource Model Source
- [attribute-match-node-enhancer](https://github.com/rundeck-plugins/attribute-match-node-enhancer) - Node Enhancer
- [multiline-regex-datacapture-filter](https://github.com/rundeck-plugins/multiline-regex-datacapture-filter) - Log Filter

---

## Common Mistakes to Avoid

❌ **Don't**:
- Forget @Plugin annotation (plugin won't be discovered!)
- Skip manifest attributes in jar task
- Forget to copy plugin to plugins directory
- Use wrong service name (plugin won't appear in UI)
- Skip tests for plugin properties
- Hard-code configuration (use @PluginProperty)

✅ **Do**:
- Use @Plugin with correct name and service
- Set all manifest attributes correctly
- Use Gradle Version Catalog for dependencies
- Test plugin properties and behavior
- Document configuration in README
- Follow Howto CLI conventions for documentation
- Use @CompileStatic for better performance

---

## Plugin Types Reference

| Type | Service Name | Interface | Use Case |
|------|--------------|-----------|----------|
| Workflow Step | WorkflowStep | StepPlugin | Job step in workflow |
| Node Step | WorkflowNodeStep | NodeStepPlugin | Step on specific nodes |
| Resource Model | ResourceModelSource | ResourceModelSource | Node source provider |
| Notification | Notification | NotificationPlugin | Job notifications |
| Log Filter | LogFilterPlugin | LogFilterPlugin | Process log output |
| Node Executor | NodeExecutor | NodeExecutor | Execute commands on nodes |
| File Copier | FileCopier | FileCopier | Copy files to nodes |
| Storage | StorageConverter | StorageConverter | Storage backend |

---

## Integration with Other Skills

- **create-code**: Use for plugin implementation following code standards
- **create-test**: Use for comprehensive plugin testing
- **cve-remediation**: Check plugin dependencies for CVEs

---

## Resources

- `.claude/docs/plugin-development.md` - Complete plugin development guide
- `.claude/docs/development-guidelines.md` - Code standards
- [Rundeck Plugin Development](https://docs.rundeck.com/docs/developer/) - Official docs
- [Howto CLI Tool](https://github.com/rundeck/howto-cli-tool) - Documentation conventions
- [EC2 Nodes Plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin) - Full example
- [Gradle Version Catalog](https://docs.gradle.org/current/userguide/platforms.html) - Dependency management
- [Axion Release](https://axion-release-plugin.readthedocs.io/) - Versioning
