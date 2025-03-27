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


import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestService
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestSource
import com.rundeck.repository.manifest.search.ManifestSearch
import com.rundeck.repository.manifest.search.SearchTerm
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class MemoryManifestService implements ManifestService {
    private static Logger LOG = LoggerFactory.getLogger(MemoryManifestService)
    private final URL manifestDatasource
    private final ManifestSource manifestSource
    private List<ManifestEntry> artifacts = []

    MemoryManifestService(ManifestSource mSource) {
        this.manifestSource = mSource
        if(LOG.traceEnabled) LOG.trace(mSource.toString())
    }

    @Override
    synchronized Collection<ManifestEntry> listArtifacts(Integer offset = 0, Integer max = -1) {
        if(!offset) offset = 0
        if(!max) max = -1
        if(offset == 0 && max == -1) return artifacts.asImmutable()

        int lmax = offset+ max > artifacts.size()- 1 ? artifacts.size()- 1 : offset+ max
        return artifacts.sort(artifactSorter).subList(offset, lmax).asImmutable()
    }

    private static Closure getArtifactSorter() {
        { a, b -> a.name <=> b.name }
    }

    @Override
    synchronized Collection<ManifestEntry> searchArtifacts(final ManifestSearch search) {
        Set<ManifestEntry> searchResuls = [] as Set
        if(!search) return searchResuls
        artifacts.each { artifact ->
            search.listSearchTerms().each { SearchTerm term ->
                if(term.matchChecker.matches(artifact[term.attributeName],term.searchValue)) {
                    searchResuls.add(artifact)
                }
            }
        }

        return searchResuls.sort(artifactSorter)
    }

    @Override
    synchronized ResponseMessage syncManifest() {
        try {
            ArtifactManifest newManifest = manifestSource.getManifest()
            artifacts.clear()
            newManifest.entries.each { artifacts.add(it) }
            artifacts.sort(artifactSorter)
            if(LOG.traceEnabled) LOG.trace("syncing manifest ${newManifest.entries.size()} - artifacts cached")
            return ResponseMessage.success()
        } catch(Exception ex) {
            ///log ex
            ex.printStackTrace()
            return new ResponseMessage(code:ResponseCodes.MANIFEST_SYNC_FAILURE,message: ex.message)
        }
    }

    @Override
    ManifestEntry getEntry(final String artifactId) {
        return artifacts.find { it.id == artifactId }
    }
}
