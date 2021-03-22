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
        }

        when:
        def result = plugin.getStatusInternal(context, false)
        then:
        result.state == ImportSynchState.DELETE_NEEDED
    }

}
