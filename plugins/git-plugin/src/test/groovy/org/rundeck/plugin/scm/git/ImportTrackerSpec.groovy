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
import spock.lang.Specification

/**
 * Created by greg on 10/5/15.
 */
class ImportTrackerSpec extends Specification {
    def "track job"(){
        given:
        def path = "a/b"
        def job = Mock(JobScmReference){
            getId()>>'123'
            getScmImportMetadata()>> [
                    commitId: 'abc'
            ]
        }
        def tracker = new ImportTracker()

        when:
        tracker.trackJobAtPath(job, path)

        then:
        tracker.trackedPaths()==[path] as Set
        tracker.renamedValue(path) == null
        tracker.trackedJob(path) == '123'
        tracker.trackedCommit(path) == 'abc'
        !tracker.wasRenamed(path)
        tracker.originalValue(path) == null
        !tracker.trackedItemIsUnknown(path)
    }
    def "track/untrack job"(){
        given:
        def path = "a/b"
        def job = Mock(JobScmReference){
            getId()>>'123'
            getScmImportMetadata()>> [
                    commitId: 'abc'
            ]
        }
        def tracker = new ImportTracker()

        when:
        tracker.trackJobAtPath(job, path)
        tracker.untrackPath(path)

        then:
        tracker.trackedPaths()==[] as Set
        tracker.renamedValue(path) == null
        tracker.trackedJob(path) == null
        tracker.trackedCommit(path) == null
        tracker.trackedPath('123') == null
        !tracker.wasRenamed(path)
        tracker.originalValue(path) == null
        tracker.trackedItemIsUnknown(path)
    }
    def "trackJobAtPath moving a job directly to a new path drops the old forward mapping"() {
        given: "a job whose path changes (e.g. a path-template edit) without going through jobRenamed"
        def oldpath = "a/b"
        def newpath = "c/d"
        def job = Mock(JobScmReference) {
            getId() >> '123'
            getScmImportMetadata() >> [
                    commitId: 'abc'
            ]
        }
        def tracker = new ImportTracker()

        when:
        tracker.trackJobAtPath(job, oldpath)
        tracker.trackJobAtPath(job, newpath)

        then: "only the new path remains tracked, so the old path can't surface as a false DELETE_NEEDED"
        tracker.trackedPaths() == [newpath] as Set
        tracker.trackedJob(newpath) == '123'
        tracker.trackedPath('123') == newpath
        tracker.trackedJob(oldpath) == null
        tracker.trackedCommit(oldpath) == null
    }
    def "track/rename job"(){
        given:
        def path = "a/b"
        def newpath = "c/d"
        def job = Mock(JobScmReference){
            getId()>>'123'
            getScmImportMetadata()>> [
                    commitId: 'abc'
            ]
        }
        def tracker = new ImportTracker()

        when:
        tracker.trackJobAtPath(job, path)
        tracker.jobRenamed(job, path, newpath)

        then:
        tracker.trackedPaths()==[newpath] as Set
        tracker.renamedValue(path) == newpath
        tracker.trackedJob(path) == null
        tracker.trackedJob(newpath) == '123'
        tracker.trackedCommit(path) == null
        tracker.trackedCommit(newpath) == 'abc'
        tracker.wasRenamed(path)
        tracker.originalValue(path) == null
        tracker.originalValue(newpath) == path
        !tracker.trackedItemIsUnknown(path)
        !tracker.trackedItemIsUnknown(newpath)
    }
    def "track/rename/revert job"(){
        given:
        def path = "a/b"
        def newpath = "c/d"
        def job = Mock(JobScmReference){
            getId()>>'123'
            getScmImportMetadata()>> [
                    commitId: 'abc'
            ]
        }
        def tracker = new ImportTracker()

        when:
        tracker.trackJobAtPath(job, path)
        tracker.jobRenamed(job, path, newpath)
        tracker.jobRenamed(job, newpath, path)

        then:
        tracker.trackedPaths()==[path] as Set
        tracker.renamedValue(path) == null
        tracker.trackedJob(path) == '123'
        tracker.trackedJob(newpath) == null
        tracker.trackedCommit(path) == 'abc'
        tracker.trackedCommit(newpath) == null
        !tracker.wasRenamed(path)
        tracker.originalValue(path) == null
        tracker.originalValue(newpath) == null
        !tracker.trackedItemIsUnknown(path)
        tracker.trackedItemIsUnknown(newpath)
    }

    def "trackJobAtPath clears a stale reverse mapping when a different job claims the path, and the orphan flag is consumed cleanly on the next tracking call"() {
        given: "job1 tracked at A, then job2 claims A directly (a different job wins the same path)"
        def pathA = "a/b"
        def pathC = "e/f"
        def job1 = Mock(JobScmReference) {
            getId() >> '123'
            getScmImportMetadata() >> [commitId: 'abc']
        }
        def job2 = Mock(JobScmReference) {
            getId() >> '456'
            getScmImportMetadata() >> [commitId: 'def']
        }
        def tracker = new ImportTracker()
        tracker.trackJobAtPath(job1, pathA)

        when: "job2 claims A"
        tracker.trackJobAtPath(job2, pathA)

        then: "job1's reverse mapping is cleared immediately, and it's flagged orphaned"
        tracker.trackedJob(pathA) == '456'
        tracker.trackedPath('123') == null

        when: "job1 is later tracked at a new path"
        tracker.trackJobAtPath(job1, pathC)

        then: "the consumed orphan flag doesn't leave anything stale behind, and doesn't misfire as a scan for mappings that no longer exist"
        tracker.trackedJob(pathC) == '123'
        tracker.trackedPath('123') == pathC
        tracker.trackedPaths() == [pathA, pathC] as Set
    }

    def "trackJobAtPath directly reverting to a renamed-from path clears the stale forward mapping at the old path"() {
        given: "job renamed A->B via jobRenamed, then moved back to A directly - e.g. a " +
                "path-template revert that bypasses jobRenamed entirely"
        def pathA = "a/b"
        def pathB = "c/d"
        def job = Mock(JobScmReference) {
            getId() >> '123'
            getScmImportMetadata() >> [commitId: 'abc']
        }
        def tracker = new ImportTracker()
        tracker.trackJobAtPath(job, pathA)
        tracker.jobRenamed(job, pathA, pathB)

        when: "the job is tracked directly back at A, without going through jobRenamed"
        tracker.trackJobAtPath(job, pathA)

        then: "B's stale forward mapping doesn't survive to be falsely reported as DELETE_NEEDED"
        tracker.trackedJob(pathB) == null
        tracker.trackedCommit(pathB) == null
        tracker.trackedJob(pathA) == '123'
        tracker.trackedPath('123') == pathA
        tracker.trackedPaths() == [pathA] as Set
    }

    def "first-time registration of many jobs does not scan every previously tracked job"() {
        given: "trackJobAtPath must not confuse an ordinary never-tracked-before job with an orphan." +
                " untrackJobPaths() is the only caller of trackedJobIds.findAll() (the O(n) scan an" +
                " orphan-cleanup-on-every-call regression would run on every registration), and" +
                " findAll() is the only Map method here that reaches entrySet() - keySet()/values()/" +
                "get()/remove(), used everywhere else on this map, have their own implementations that" +
                " don't. Counting entrySet() calls is therefore a deterministic, CI-safe proxy for" +
                " how many times that scan ran - no wall-clock timing, so no flaking on slow CI"
        def tracker = new ImportTracker()
        //a 1-element array, not a field on the map itself: Groovy's property access on a Map
        //subclass (countingMap.xyz) means countingMap.get('xyz'), not the declared field
        int[] entrySetCalls = [0]
        def countingMap = new LinkedHashMap<String, String>() {
            @Override
            Set<Map.Entry<String, String>> entrySet() {
                entrySetCalls[0]++
                super.entrySet()
            }
        }
        //trackedJobIds is private (deliberately - see ImportTracker's class Javadoc), so this
        //instrumentation-only substitution goes through reflection rather than a production setter
        def trackedJobIdsField = ImportTracker.getDeclaredField('trackedJobIds')
        trackedJobIdsField.setAccessible(true)
        trackedJobIdsField.set(tracker, countingMap)
        int jobCount = 2000

        when: "each job is registered for the first time at its own distinct path"
        (1..jobCount).each { int i ->
            JobScmReference job = Stub(JobScmReference) {
                getId() >> "job${i}".toString()
                getScmImportMetadata() >> [commitId: "commit${i}".toString()]
            }
            tracker.trackJobAtPath(job, "path${i}".toString())
        }

        then: "none of the first-time registrations triggered the orphan-cleanup scan"
        tracker.trackedPaths().size() == jobCount
        entrySetCalls[0] == 0
    }

    def "untrackJob removes every tracked path for the deleted job"() {
        given:
        def originalPath = "a/b"
        def renamedPath = "c/d"
        def job = Mock(JobScmReference) {
            getId() >> '123'
            getScmImportMetadata() >> [
                    commitId: 'abc'
            ]
        }
        def tracker = new ImportTracker()

        when:
        tracker.trackJobAtPath(job, originalPath)
        tracker.jobRenamed(job, originalPath, renamedPath)
        tracker.trackJobAtPath(job, originalPath)
        tracker.untrackJob('123')

        then:
        tracker.trackedPaths().isEmpty()
        tracker.trackedPath('123') == null
        tracker.trackedJob(originalPath) == null
        tracker.trackedJob(renamedPath) == null
    }
}
