# Plugin Development Guidelines

## Overview

Best practices for developing Rundeck and Runbook Automation plugins that are _bundled_ with the application but have separate source repositories.

---

## Example Repositories

Updated plugin repositories that follow these guidelines:

- [attribute-match-node-enhancer](https://github.com/rundeck-plugins/attribute-match-node-enhancer)
- [multiline-regex-datacapture-filter](https://github.com/rundeck-plugins/multiline-regex-datacapture-filter)
- [rundeck-ec2-nodes-plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin)

---

## Building

### Requirements

- **Build Tool**: Gradle with wrapper script
- **Java Version**: Java 17
- **GitHub Actions**:
  - Gradle (CI)
  - Release

### Gradle Configuration

**Version**: Latest

**Recommended Plugins**:
- Axion release (for versioning/releasing)

---

## How To Documentation

Follow [Howto CLI Tool](https://github.com/rundeck/howto-cli-tool) conventions to describe:

- Build
- Test  
- Release
- Version check
- Any other utility actions

**Example**: [rundeck-ec2-nodes-plugin README](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin/blob/main/Readme.md)

---

## Dependencies

### Use Gradle Version Catalog

Define dependencies and versions using a Gradle Version Catalog

**Example**: [rundeck-ec2-nodes-plugin libs.versions.toml](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin/blob/main/gradle/libs.versions.toml)

---

## CI Configuration

### Use Github Actions

Easiest integration with Github

### build.yml

- Define build process
- Upload CI artifacts

### release.yml

**Triggers**: On tags `v*`

**Example**: [rundeck-ec2-nodes-plugin release.yml](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin/blob/main/.github/workflows/release.yml)

**Steps**:
- Same build steps as build.yml
- Use `gh release create` for release creation

```bash
gh release create \
    --generate-notes \
    --title 'Release ${{ steps.get_version.outputs.VERSION }}' \
    ${{ github.ref_name }} \
    path/to/jar
```

---

## Renovate Bot

Enable Renovate bot for automatic dependency updates

### Setup

1. **Add repo to Renovate**:
   - Visit Github org Settings > Github Apps
   - Mend Renovate - click "Configure"
   - Add the repository

2. **Configure**:
   - Visit [developer.mend.io](http://developer.mend.io) to configure

---

## Releasing

### Using Axion Release

Run the release command:

```bash
./gradlew release
```

**Note**: May fail to push created tag due to SSH Agent incompatibility

**Workaround**: Manually push the tag:

```bash
git push v1.2.3
```

**SSH Agent Issue**: Axion Release/Jsch library may have issues with SSH Agent (e.g., 1Password SSH Agent). Solutions:

- Manually push tags: `git push v1.2.3`
- Adjust `~/.ssh/config`:
  - Use `IdentityAgent` pointing to socket file
  - Avoid forcing specific `IdentityFile` with `IdentitiesOnly yes`

---

## Publishing

### Open Source Plugins

#### Maven Central (Preferred)

**Example**: [rundeck-ec2-nodes-plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin)

**Setup Steps**:

1. **Add release process to CI (Github Actions)**:
   ```bash
   ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
   ```
   Requires signing and sonatype secrets

2. **Update build.gradle**:
   - Add `nexusPublish` gradle plugin
   - Add `nexusPublishing` configuration:
   ```groovy
   nexusPublishing {
       repositories {
           sonatype {
               nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
               snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
           }
       }
   }
   ```

3. **Apply publishing.gradle**:
   - Define project extension properties
   - Specify: `group = 'org.rundeck.plugins'`
   - Update `java` configuration:
     - Add `withJavadocJar()`
     - Add `withSourcesJar()` (required by Maven Central)

4. **Fix javadoc issues**:
   - Required before publishing (javadoc step will fail otherwise)

5. **Configure Github Org Secrets**:
   - [rundeck-plugins secrets](https://github.com/organizations/rundeck-plugins/settings/secrets/actions)
   - Required: `SIGNING_KEY_B64`, `SIGNING_PASSWORD`, `SONATYPE_PASSWORD`, `SONATYPE_USERNAME`

#### JitPack (DEPRECATED - DO NOT USE)

- Format: `com.github.ORG:REPO:TAG`
- **Reason for deprecation**: Moving away from third-party service dependency

---

## Summary

For complete plugin development workflow including comprehensive checklist, use the **`create-plugin`** skill.

---

## Resources

- [Howto CLI Tool](https://github.com/rundeck/howto-cli-tool)
- [Example: EC2 Nodes Plugin](https://github.com/rundeck-plugins/rundeck-ec2-nodes-plugin)
- [Gradle Version Catalog](https://docs.gradle.org/current/userguide/platforms.html)
