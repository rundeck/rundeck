/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.JobScmReference

/**
 * Internal tracker for matching repo paths with imported jobs and commits
 */
class ImportTracker {
    RenameTracker<String> renamedTrackedItems = new RenameTracker<>()

    Map<String, String> trackedCommits = Collections.synchronizedMap([:])
    Map<String, String> trackedJobIds = Collections.synchronizedMap([:])
    /**
     * job ID -> path
     */
    Map<String, String> trackedPathsMap = Collections.synchronizedMap([:])

    public Set<String> trackedPaths() {
        return trackedCommits.keySet() + trackedJobIds.keySet()
    }
    /**
     * Return true if the path has not been imported
     * @param path
     * @return true if path is not imported
     */
    boolean trackedItemIsUnknown(String path) {
        !trackedCommit(path) && !wasRenamed(path)
    }

    boolean wasRenamed(String path) {
        renamedTrackedItems.wasRenamed(path)
    }

    String renamedValue(String path) {
        renamedTrackedItems.renamedValue(path)
    }

    String originalValue(String path) {
        renamedTrackedItems.originalValue(path)
    }

    void jobRenamed(JobScmReference job, String oldpath, String newpath) {
        untrackPath(oldpath)
        trackJobAtPath(job, newpath)
        renamedTrackedItems.trackItem(oldpath, newpath)
    }

    void trackJobAtPath(JobScmReference job, String path) {
        trackedCommits[path] = job.scmImportMetadata?.commitId
        trackedJobIds[path] = job.id
        trackedPathsMap[job.id] = path
    }

    String untrackPath(String path) {
        trackedCommits.remove(path)
        trackedJobIds.remove(path)
    }

    String trackedCommit(String path) {
        trackedCommits[path]
    }

    String trackedJob(String path) {
        trackedJobIds[path]
    }

    String trackedPath(String jobId) {
        trackedPathsMap[jobId]
    }

    Map<String, String> trackedDetail(String path) {
        [id: trackedJobIds[path], commitId: trackedCommits[path]]
    }

    @Override
    public String toString() {
        return "ImportTracker{" +
                "renamedTrackedItems=" + renamedTrackedItems +
                ", trackedCommits=" + trackedCommits +
                ", trackedJobIds=" + trackedJobIds +
                ", trackedPathsMap=" + trackedPathsMap +
                '}';
    }
}
