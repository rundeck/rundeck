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

### Use GitHub Actions

Easiest integration with GitHub

### build.yml

- Define build process
- Upload CI artifacts

### release.yml

**Triggers**: On tags `v*`

**Example**: [sshj-plugin release.yml](https://github.com/rundeck-plugins/sshj-plugin/blob/main/.github/workflows/release.yml)

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
   - Visit GitHub org Settings > GitHub Apps
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

#### PackageCloud (Preferred)

**Example**: [sshj-plugin](https://github.com/rundeck-plugins/sshj-plugin) (pilot migration)

**Setup Steps**:

1. **Update build.gradle** — remove any `nexusPublish`/`nexusPublishing` block and configure the PackageCloud
   Maven repository instead:
   ```groovy
   def pkgcldRepoUrl = (System.getenv("PKGCLD_REPO_URL") ?: "https://packagecloud.io/pagerduty/rundeck-plugins/maven2").trim()
   publishing {
       repositories {
           maven {
               name = "PackageCloud"
               url = uri(pkgcldRepoUrl)
               authentication { header(HttpHeaderAuthentication) }
               credentials(HttpHeaderCredentials) {
                   name = "Authorization"
                   value = "Bearer " + (System.getenv("PKGCLD_WRITE_TOKEN") ?: project.findProperty("pkgcldWriteToken"))
               }
           }
       }
   }
   ```
   The `PKGCLD_REPO_URL` fallback default keeps unrelated CI jobs (e.g. `gradlew build` in `gradle.yml`) working
   even when the env var isn't set — the URL is evaluated eagerly at configuration time, so it can't be null.

2. **Add release process to CI (GitHub Actions)**:
   ```bash
   ./gradlew publishAllPublicationsToPackageCloudRepository
   ```
   (Task name is derived from the `name = "PackageCloud"` repository above — verify the actual task name with
   `./gradlew tasks --all | grep -i packagecloud` for each repo, since it can differ for zip-based plugins using
   a `mavenZip(MavenPublication)` publication.)

3. **Sign artifacts with real `gpg`, not Gradle's `signing` plugin**: PackageCloud rejects the checksum file
   Gradle generates for the `.asc` signature (`*.jar.asc.sha1` → HTTP 422). Sign with the `gpg` CLI and upload
   only the resulting `.asc` via `curl`, bypassing Gradle's checksum machinery for that artifact. See the
   `migrate-plugin-to-packagecloud` skill (or an already-migrated repo's `release.yml`) for the exact step.

4. **Apply publishing.gradle**:
   - Define project extension properties
   - Specify: `group = 'org.rundeck.plugins'`
   - Update `java` configuration:
     - Add `withJavadocJar()`
     - Add `withSourcesJar()`

5. **Fix javadoc issues**:
   - Required before publishing (javadoc step will fail otherwise)

6. **Configure GitHub Org Secrets**:
   - [rundeck-plugins secrets](https://github.com/organizations/rundeck-plugins/settings/secrets/actions)
   - Required: `PKGCLD_WRITE_TOKEN`, `SIGNING_KEY_B64`, `SIGNING_PASSWORD`
   - `PKGCLD_REPO_URL` is set as a GitHub Actions **variable** (not secret) at the org level

7. **Verify the GPG signing key hasn't expired**: `gpg --show-keys --with-colons <key>` — Gradle's
   `useInMemoryPgpKeys` (Bouncy Castle) does not enforce key expiration, but real `gpg` does, so an expired key
   fails only when the manual signing step runs.

**Maven Central (deprecated for `rundeck-plugins`)**: still applicable to other orgs/repos that haven't hit the
publishing limit. If reintroducing it elsewhere, it required the `nexusPublish` gradle plugin, a `nexusPublishing`
block pointing at `https://ossrh-staging-api.central.sonatype.com/service/local/`, and
`SIGNING_KEY_B64`/`SIGNING_PASSWORD`/`SONATYPE_USERNAME`/`SONATYPE_PASSWORD` secrets.

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
