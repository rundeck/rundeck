/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rundeck.repository.manifest


class RundeckManifestEntry extends ManifestEntry {
    Map record = [:]

    String getId() {
        return record.post_id
    }

    String getInstallId() {
        return record.object_id
    }

    String getName() {
        return record.post_slug
    }

    String getDisplay() {
        return record.post_title
    }

    String getAuthor() {
        return record.plugin_author
    }

    String getDescription() {
        return record.post_excerpt
    }

    String getOrganization() {
        return record.organization
    }

    String getSourceLink() {
        return record.source_link
    }

    String getArtifactType() {
        return record.artifact_type
    }

    String getSupport() {
        return record.taxonomies?.plugin_support_type?.join(" ")
    }

    String getCurrentVersion() {
        return record.current_version
    }

    String getRundeckCompatibility() {
        return record.rundeck_compatibility
    }

    String getTargetHostCompatibility() {
        return record.target_host_compatibility
    }

    String getBinaryLink() {
        return record.binary_link
    }

    boolean getInstallable() { return record.object_id }

    String getLastRelease() {
        return record.last_release
    }

    Collection<String> getProvidesServices() {
        return record.taxonomies.plugin_type
    }

}
