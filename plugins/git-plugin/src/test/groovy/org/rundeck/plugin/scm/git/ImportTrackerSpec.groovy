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

    def "trackJobAtPath cleans up an orphaned forward mapping even when the reverse entry was already claimed by another job"() {
        given: "job1 renamed A->B, then re-asserted at A, leaving two forward mappings by design"
        def pathA = "a/b"
        def pathB = "c/d"
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
        tracker.jobRenamed(job1, pathA, pathB)
        tracker.trackJobAtPath(job1, pathA)

        when: "job2 claims A, removing job1's reverse entry but not its forward mapping at B"
        tracker.trackJobAtPath(job2, pathA)

        then:
        tracker.trackedJob(pathA) == '456'
        tracker.trackedJob(pathB) == '123'

        when: "job1 is then tracked at a third path, a direct move with no reverse entry to compare against"
        tracker.trackJobAtPath(job1, pathC)

        then: "the orphaned mapping at B is cleaned up instead of surviving as a phantom DELETE_NEEDED"
        tracker.trackedJob(pathB) == null
        tracker.trackedCommit(pathB) == null
        tracker.trackedJob(pathC) == '123'
        tracker.trackedPath('123') == pathC
        tracker.trackedPaths() == [pathA, pathC] as Set
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
