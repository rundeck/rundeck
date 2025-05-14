/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.rundeck.repository.client.manifest

import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestSource


class MemoryManifestSource implements ManifestSource {

    ArtifactManifest manifest = new ArtifactManifest()

    @Override
    ArtifactManifest getManifest() {
        return manifest
    }

    @Override
    void saveManifest(final ArtifactManifest manifest) {
        this.manifest = manifest
    }
}
