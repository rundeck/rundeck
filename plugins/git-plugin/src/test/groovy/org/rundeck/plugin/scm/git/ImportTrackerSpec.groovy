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
        !tracker.wasRenamed(path)
        tracker.originalValue(path) == null
        tracker.trackedItemIsUnknown(path)
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
}
