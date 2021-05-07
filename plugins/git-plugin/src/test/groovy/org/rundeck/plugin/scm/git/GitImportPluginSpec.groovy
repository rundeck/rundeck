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

import com.dtolabs.rundeck.plugins.scm.ImportResult
import com.dtolabs.rundeck.plugins.scm.ImportSynchState
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.util.FileUtils
import org.eclipse.jgit.util.SystemReader
import org.rundeck.plugin.scm.git.config.Config
import org.rundeck.plugin.scm.git.config.Import
import spock.lang.Specification

/**
 * Created by greg on 9/19/16.
 */
class GitImportPluginSpec extends Specification {
    File tempdir

    def setup() {
        tempdir = File.createTempFile("GitImportPluginSpec", "-test")
        tempdir.delete()
        SystemReader.setInstance(new TestSystemReader())
    }

    def cleanup() {
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS)
        }
    }

    static Import createTestConfig(File gitdir, File origindir, Map<String, String> override = [:]) {
        Map<String, String> input = [
                dir                  : gitdir.absolutePath,
                pathTemplate         : '${job.group}${job.name}-${job.id}.xml',
                branch               : 'master',
                format               : 'xml',
                strictHostKeyChecking: 'yes',
                url                  : origindir.absolutePath,
                useFilePattern       : 'true',
                filePattern          : '.*\\.xml',
                importUuidBehavior   : 'preserve'

        ] + override
        def config = Config.create(Import, input)
        config
    }

    def "get job status unimported job"() {
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)

        Git git = GitExportPluginSpec.createGit(origindir)

        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-123.xml', 'blah')
        git.close()

        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        )
        def job = Mock(JobScmReference) {
            getScmImportMetadata() >> [:]
            getProject() >> projectName
            getId() >> '123'
            getJobName() >> 'job1'
            getGroupPath() >> ''
            getJobAndGroup() >> 'job1'
        }

        when:
        def status = plugin.getJobStatus(job, null)


        then:
        status.synchState == ImportSynchState.IMPORT_NEEDED
    }

    def "get job status clean job"() {
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)

        Git git = GitExportPluginSpec.createGit(origindir)

        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-123.xml', 'blah')
        git.close()

        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        )
        def job = Mock(JobScmReference) {
            getScmImportMetadata() >> [commitId: commit.name, url: origindir.absolutePath]
            getProject() >> projectName
            getId() >> '123'
            getJobName() >> 'job1'
            getGroupPath() >> ''
            getJobAndGroup() >> 'job1'
            getImportVersion() >> 12L
            getVersion() >> 12L
        }

        when:
        def status = plugin.getJobStatus(job, null)


        then:
        status.synchState == ImportSynchState.CLEAN
    }

    /**
     * Repo
     */
    def "get job status previously imported different new repo"() {
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)

        Git git = GitExportPluginSpec.createGit(origindir)

        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-123.xml', 'blah')
        git.close()

        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        )
        def job = Mock(JobScmReference) {
            getScmImportMetadata() >> [commitId: 'xyzwrong', url: wrongurl]
            getProject() >> projectName
            getId() >> '123'
            getJobName() >> 'job1'
            getGroupPath() >> ''
            getJobAndGroup() >> 'job1'
        }

        when:
        def status = plugin.getJobStatus(job, null)


        then:
        status.synchState == ImportSynchState.IMPORT_NEEDED

        where:
        wrongurl  | _
        'invalid' | _
        null      | _

    }

    def "get status on deleted jobs"() {
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)

        Git git = GitExportPluginSpec.createGit(origindir)


        git.close()

        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        )
        plugin.jobStateMap['0001'] = ['synch':'DELETE_NEEDED','path':'job/xy-0001.xml']

        when:
        def ret = plugin.getTrackedItemsForAction(actionId)

        then:
        ret
        ret.size()==1

        where:
        actionId      | _
        'import-all'  | _
        'import-jobs' | _
    }

    def "perform pull on clean state withouth npe"() {
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)

        Git git = GitExportPluginSpec.createGit(origindir)


        git.close()
        ScmOperationContext context = Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }

        JobImporter importer = Mock(JobImporter)
        List<String> selectedPaths = []
        Map<String, String> input = [:]

        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(context)

        when:
        def ret = plugin.scmImport(context, actionId,
                importer,
                selectedPaths,
                input)

        then:
        ret != null
        ret.success

        where:
        actionId      | _
        'remote-pull' | _
    }

    def "getStatusInternal no action needed"(){
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)
        Git git = GitExportPluginSpec.createGit(origindir)
        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-123.xml', 'blah')
        git.close()
        ScmOperationContext context = Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(context)
        plugin.importTracker = Mock(ImportTracker){
            trackedPaths() >> ['job1-1234.xml']
            getTrackedCommits() >> ['job1-1234.xml' : '123123']
            getTrackedJobIds() >> ['job1-12345.xml':'1234']
        }

        when:
        def result = plugin.getStatusInternal(context, false)
        then:
        result.state == ImportSynchState.CLEAN
    }

    def "getStatusInternal import needed"(){
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)
        Git git = GitExportPluginSpec.createGit(origindir)
        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-123.xml', 'blah')
        git.close()
        ScmOperationContext context = Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(context)
        plugin.importTracker = Mock(ImportTracker){
            trackedPaths() >> ['job1-123.xml']
            getTrackedCommits() >> ['job1-123.xml' : '123123']
        }
        plugin.jobStateMap["123"] = [
                "synch": ImportSynchState.IMPORT_NEEDED,
                "path": "job1-123.xml"
        ]

        when:
        def result = plugin.getStatusInternal(context, false)
        then:
        result.state == ImportSynchState.IMPORT_NEEDED
    }

    def "getStatusInternal delete needed"(){
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)
        Git git = GitExportPluginSpec.createGit(origindir)
        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-123.xml', 'blah')
        git.close()
        ScmOperationContext context = Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(context)
        plugin.importTracker = Mock(ImportTracker){
            trackedPaths() >> ['job1-1231.xml']
            getTrackedCommits() >> ['job1-123.xml' : '123123']
            getTrackedJobIds() >> ['123123':'123123.xml']
        }
        when:
        def result = plugin.getStatusInternal(context, false)
        then:
        result.state == ImportSynchState.DELETE_NEEDED
    }

    def "getStatusInternal import needed for job mod"(){
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)
        Git git = GitExportPluginSpec.createGit(origindir)
        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-1234.xml', 'blah')
        git.close()
        ScmOperationContext context = Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(context)
        plugin.jobStateMap['1234'] = ['synch':'MODIFIED','path':'job1-1234.xml']
        plugin.importTracker = Mock(ImportTracker){
            trackedPaths() >> ['job1-1234.xml']
            getTrackedCommits() >> ['job1-1234.xml' : '123123']
            getTrackedJobIds() >> ['job1-1234.xml':'1234']
        }
        plugin.jobStateMap["123123"] = [
                "synch": ImportSynchState.IMPORT_NEEDED,
                "path": "job1-1234.xml"
        ]
        when:
        def result = plugin.getStatusInternal(context, false)
        then:
        result.message == '1 file(s) need to be imported'
        result.importNeeded == 1
        result.state == ImportSynchState.IMPORT_NEEDED
    }

    def "perform import renamed job"() {
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def actionId = "import-jobs"
        Import config = createTestConfig(gitdir, origindir)

        Git git = GitExportPluginSpec.createGit(origindir)
        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'test/job-123.xml', 'blah')
        git.close()

        ScmOperationContext context = Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }

        JobImporter importer = Mock(JobImporter)
        List<String> selectedPaths = ["test/job-123.xml"]
        List<String> deletePaths = []

        def job = Mock(JobScmReference) {
            getScmImportMetadata() >> ["commitId":commit.name]
            getProject() >> projectName
            getId() >> '123'
            getJobName() >> 'job1'
            getGroupPath() >> ''
            getJobAndGroup() >> 'job1'
        }

        def renamedTracker = new RenameTracker<>()
        renamedTracker.trackItem('job-123.xml','test/job-123.xml')

        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(context)
        plugin.importTracker = Mock(ImportTracker){
            trackedPaths() >> ['test/job-123.xml']
            getTrackedCommits() >> ['test/job-123.xml' : '123']
            getTrackedJobIds() >> ['test/job-123.xml':'123']
            getRenamedTrackedItems() >>renamedTracker
        }

        when:
        def ret = plugin.scmImport(context, actionId,
                importer,
                selectedPaths,
                deletePaths,
                [:]
        )

        then:

        0*importer.deleteJob(projectName, "123")>>Mock(ImportResult){
            isSuccessful()>>true
        }

        then:
        1*importer.importFromStream(_,_,_,_,_)>>Mock(ImportResult){
            isSuccessful()>>true
            getJob()>>job
        }

        then:
        plugin.jobStateMap["123"]!=null
        plugin.jobStateMap["123"]["synch"]!= ImportSynchState.CLEAN
        ret != null
        ret.success
    }

    def "get job status IMPORT_NEEDED when job is renamed"() {
        given:
        def projectName = 'GitImportPluginSpec'
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Import config = createTestConfig(gitdir, origindir)

        Git git = GitExportPluginSpec.createGit(origindir)

        def commit = GitExportPluginSpec.addCommitFile(origindir, git, 'job1-123.xml', 'blah')
        def commit2 = GitExportPluginSpec.addCommitFile(origindir, git, 'job2-234.xml', 'blah 222')
        def commit3 = GitExportPluginSpec.addCommitFile(origindir, git, 'job2-234.xml', 'blah 333')
        def commit4 = GitExportPluginSpec.renameCommitFile(origindir, git, 'job1-123.xml', 'job1-rename-123.xml', 'blah 333')
        def commit5 = GitExportPluginSpec.addCommitFile(origindir, git, 'job2-234.xml', 'blah 4444')

        git.close()

        def plugin = new GitImportPlugin(config, [])
        plugin.initialize(Mock(ScmOperationContext) {
            getFrameworkProject() >> projectName
        }
        )
        def job = Mock(JobScmReference) {
            getScmImportMetadata() >> [commitId: commit.name, url: origindir.absolutePath]
            getProject() >> projectName
            getId() >> '123'
            getJobName() >> 'job1'
            getGroupPath() >> ''
            getJobAndGroup() >> 'job1'
            getImportVersion() >> 12L
            getVersion() >> 12L
        }

        when:
        def status = plugin.getJobStatus(job, null)


        then:
        status.synchState == ImportSynchState.IMPORT_NEEDED
        plugin.importTracker.renamedTrackedItems !=null
        plugin.importTracker.renamedTrackedItems.wasRenamed('job1-123.xml')
        plugin.importTracker.renamedTrackedItems.originalValue('job1-rename-123.xml') == 'job1-123.xml'
    }

}
