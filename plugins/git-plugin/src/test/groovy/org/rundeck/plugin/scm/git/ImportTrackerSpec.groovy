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
