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
     * Return a stable snapshot of all tracked repository paths.
     *
     * <p>The backing maps are synchronized wrappers whose key set views are not safe to iterate
     * without holding the map monitor: the SCM loader mutates them while request handling reads
     * them, which would surface as a ConcurrentModificationException on the request path.
     *
     * @return tracked repository paths
     */
    public Set<String> trackedPaths() {
        synchronized (this) {
            Set<String> result
            synchronized (trackedCommits) {
                result = new LinkedHashSet<>(trackedCommits.keySet())
            }
            synchronized (trackedJobIds) {
                result.addAll(trackedJobIds.keySet())
            }
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
                untrackJob(job.id)
                trackJobAtPath(job, newpath)
                renamedTrackedItems.trackItem(oldpath, newpath)
            }
        }
    }

    void trackJobAtPath(JobScmReference job, String path) {
        synchronized (this) {
            def previousJobId = trackedJobIds[path]
            if (previousJobId != null && previousJobId != job.id) {
                //path is being claimed by a different job than the one previously tracked there:
                //remove the stale reverse and rename mappings before assigning the new owner
                trackedPathsMap.remove(previousJobId, path)
                renamedTrackedItems.untrack(path)
            }
            def previousPath = trackedPathsMap[job.id]
            if (previousPath != null && previousPath != path && renamedTrackedItems.originalValue(previousPath) != path) {
                //job is moving to a new path (e.g. a path-template change) without going through
                //jobRenamed: remove its old forward mapping so it doesn't linger as a phantom tracked
                //path, which would otherwise surface as a false DELETE_NEEDED.
                //(skipped when `path` is the canonical path being re-asserted right after jobRenamed
                //recorded a rename to previousPath - that resync intentionally keeps both tracked)
                trackedCommits.remove(previousPath)
                trackedJobIds.remove(previousPath)
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
            String canonicalPath = trackedPathsMap.remove(jobId)
            Set<String> pathsToRemove = new LinkedHashSet<>()
            if (canonicalPath) {
                pathsToRemove.add(canonicalPath)
            }
            synchronized (trackedJobIds) {
                pathsToRemove.addAll(
                        trackedJobIds.findAll { String ignored, String id -> id == jobId }.keySet()
                )
            }
            pathsToRemove.each { String path ->
                trackedCommits.remove(path)
                trackedJobIds.remove(path)
                //the job is gone: any rename mapping still naming this path (as old or new name)
                //would otherwise leave wasRenamed() reporting true for a path whose job no longer exists
                renamedTrackedItems.untrack(path)
            }
            canonicalPath ?: (pathsToRemove ? pathsToRemove.iterator().next() : null)
        }
    }

    /**
     * Return a stable snapshot of all tracked job IDs.
     *
     * @return tracked job IDs
     */
    Set<String> trackedJobIdSet() {
        synchronized (this) {
            Set<String> result
            synchronized (trackedPathsMap) {
                result = new HashSet<>(trackedPathsMap.keySet())
            }
            synchronized (trackedJobIds) {
                result.addAll(trackedJobIds.values())
            }
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
                '}'
    }
}
