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
    /**
     * Job IDs whose reverse mapping was just cleared by a different job claiming their path,
     * so they may still hold another forward mapping elsewhere that needs cleanup next time
     * they're tracked again. Lets trackJobAtPath tell that case apart from ordinary first-time
     * registration (also previousPath == null) without scanning trackedJobIds on every call.
     */
    private final Set<String> orphanedJobIds = new HashSet<>()

    /**
     * Return a stable snapshot of all tracked repository paths.
     *
     * <p>Iterates under this tracker's own lock - synchronizedMap's key-set views aren't
     * iteration-safe, and every external access to these maps goes through this class's own
     * synchronized methods, so that single lock is sufficient.
     *
     * @return tracked repository paths
     */
    public Set<String> trackedPaths() {
        synchronized (this) {
            Set<String> result = new LinkedHashSet<>(trackedCommits.keySet())
            result.addAll(trackedJobIds.keySet())
            return result
        }
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
        synchronized (this) {
            if (oldpath == newpath) {
                def originalPath = originalValue(newpath)
                if (originalPath) {
                    renamedTrackedItems.trackItem(originalPath, originalPath)
                    untrackPath(originalPath)
                }
            } else {
                //forward mappings only - trackItem() below needs the rename chain intact to detect a revert
                untrackJobPaths(job.id)
                trackJobAtPath(job, newpath)
                renamedTrackedItems.trackItem(oldpath, newpath)
            }
        }
    }

    void trackJobAtPath(JobScmReference job, String path) {
        synchronized (this) {
            def previousJobId = trackedJobIds[path]
            if (previousJobId != null && previousJobId != job.id) {
                //path claimed by a different job: clear its stale reverse and rename mappings,
                //and flag it, since it may still hold another forward mapping elsewhere
                trackedPathsMap.remove(previousJobId, path)
                renamedTrackedItems.untrack(path)
                orphanedJobIds.add(previousJobId)
            }
            def previousPath = trackedPathsMap[job.id]
            //consume this job's own orphan flag (if any) regardless of the outcome below, so a
            //later call for the same job doesn't re-trigger the scan on stale information
            boolean wasOrphaned = orphanedJobIds.remove(job.id)
            if (previousPath != path &&
                    (wasOrphaned || (previousPath != null && renamedTrackedItems.originalValue(previousPath) != path))) {
                //direct move (e.g. path-template change) bypassing jobRenamed, or cleaning up after
                //a prior orphaning (see above): clear every forward mapping the job holds, not just
                //previousPath, since a prior rename re-assertion can leave two. The orphan-flag check
                //avoids scanning trackedJobIds (in untrackJobPaths) on every first-time registration,
                //which previousPath == null alone can't distinguish from a real orphan. skipped when
                //`path` re-asserts the canonical path right after a rename.
                untrackJobPaths(job.id).each { String stalePath ->
                    renamedTrackedItems.untrack(stalePath)
                }
            }
            trackedCommits[path] = job.scmImportMetadata?.commitId
            trackedJobIds[path] = job.id
            trackedPathsMap[job.id] = path
        }
    }

    /**
     * Stop tracking a repository path and its reverse job-to-path mapping.
     *
     * @param path repository path
     * @return job ID previously associated with the path
     */
    String untrackPath(String path) {
        synchronized (this) {
            trackedCommits.remove(path)
            String jobId = trackedJobIds.remove(path)
            if (jobId) {
                trackedPathsMap.remove(jobId)
            }
            jobId
        }
    }

    /**
     * Stop tracking a job when only its ID is available.
     *
     * @param jobId Rundeck job ID
     * @return repository path previously associated with the job
     */
    String untrackJob(String jobId) {
        synchronized (this) {
            Set<String> pathsToRemove = untrackJobPaths(jobId)
            pathsToRemove.each { String path ->
                //clear any rename mapping too, or wasRenamed() keeps reporting true for a dead job
                renamedTrackedItems.untrack(path)
            }
            //the job is gone, so any pending orphan flag for it would never be consumed otherwise
            orphanedJobIds.remove(jobId)
            pathsToRemove ? pathsToRemove.iterator().next() : null
        }
    }

    /**
     * Remove every forward mapping for a job, without touching rename tracking (used by
     * {@link #jobRenamed}, which needs the rename chain left intact).
     *
     * @param jobId Rundeck job ID
     * @return paths that were forward-mapped to this job, canonical path first
     */
    private Set<String> untrackJobPaths(String jobId) {
        synchronized (this) {
            String canonicalPath = trackedPathsMap.remove(jobId)
            Set<String> pathsToRemove = new LinkedHashSet<>()
            if (canonicalPath) {
                pathsToRemove.add(canonicalPath)
            }
            pathsToRemove.addAll(
                    trackedJobIds.findAll { String ignored, String id -> id == jobId }.keySet()
            )
            pathsToRemove.each { String path ->
                trackedCommits.remove(path)
                trackedJobIds.remove(path)
            }
            pathsToRemove
        }
    }

    /**
     * Return a stable snapshot of all tracked job IDs.
     *
     * @return tracked job IDs
     */
    Set<String> trackedJobIdSet() {
        synchronized (this) {
            Set<String> result = new HashSet<>(trackedPathsMap.keySet())
            result.addAll(trackedJobIds.values())
            result
        }
    }

    String trackedCommit(String path) {
        synchronized (this) {
            trackedCommits[path]
        }
    }

    String trackedJob(String path) {
        synchronized (this) {
            trackedJobIds[path]
        }
    }

    String trackedPath(String jobId) {
        synchronized (this) {
            trackedPathsMap[jobId]
        }
    }

    Map<String, String> trackedDetail(String path) {
        synchronized (this) {
            [id: trackedJobIds[path], commitId: trackedCommits[path]]
        }
    }

    @Override
    public String toString() {
        return "ImportTracker{" +
                "renamedTrackedItems=" + renamedTrackedItems +
                ", trackedCommits=" + trackedCommits +
                ", trackedJobIds=" + trackedJobIds +
                ", trackedPathsMap=" + trackedPathsMap +
                ", orphanedJobIds=" + orphanedJobIds +
                '}'
    }
}
