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
import com.rundeck.repository.manifest.ManifestCreator
import com.rundeck.repository.manifest.ManifestEntry


abstract class AbstractManifestCreator implements ManifestCreator {

    void addEntryToManifest(ArtifactManifest manifest, ManifestEntry entry) {
        ManifestEntry currentEntry = manifest.entries.find { it.id == entry.id }
        if(!currentEntry) { manifest.entries.add(entry) }
        else if(currentEntry.lastRelease > entry.lastRelease) {
            currentEntry.oldVersions.add(entry.currentVersion)
        } else {
            currentEntry.lastRelease = entry.lastRelease
            currentEntry.oldVersions.add(currentEntry.currentVersion)
            currentEntry.currentVersion = entry.currentVersion
        }
    }
}
