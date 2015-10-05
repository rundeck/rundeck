package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.JobScmReference

/**
 * Internal tracker for matching repo paths with imported jobs and commits
 */
class ImportTracker {
    RenameTracker<String> renamedTrackedItems = new RenameTracker<>()

    Map<String, String> trackedCommits = Collections.synchronizedMap([:])
    Map<String, String> trackedJobIds = Collections.synchronizedMap([:])

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

    Map<String, String> trackedDetail(String path) {
        [id: trackedJobIds[path], commitId: trackedCommits[path]]
    }

    @Override
    public String toString() {
        return "ImportTracker{" +
                "renamedTrackedItems=" + renamedTrackedItems +
                ", trackedCommits=" + trackedCommits +
                ", trackedJobIds=" + trackedJobIds +
                '}';
    }
}
