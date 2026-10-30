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

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.storage.StorageTreeImpl
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import com.dtolabs.rundeck.plugins.scm.ScmUserInfoMissing
import com.dtolabs.rundeck.plugins.scm.SynchState
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.util.FileUtils
import org.eclipse.jgit.util.SystemReader
import org.rundeck.plugin.scm.git.config.Common
import org.rundeck.plugin.scm.git.config.Config
import org.rundeck.plugin.scm.git.config.Export
import org.rundeck.plugin.scm.git.exp.actions.CommitJobsAction
import org.rundeck.plugin.scm.git.exp.actions.TagAction
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicLong

/**
 * Created by greg on 8/31/15.
 */
class GitExportPluginSpec extends Specification {

    File tempdir

    def setup() {
        tempdir = File.createTempFile("GitExportPluginSpec", "-test")
        tempdir.delete()
        SystemReader.setInstance(new TestSystemReader())
    }

    def cleanup() {
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS)
        }
    }

    /**
     * Test map that requires callers to hold the map monitor for compound access.
     */
    private static class MonitorCheckedMap<K, V> extends LinkedHashMap<K, V> {
        @Override
        boolean containsKey(Object key) {
            if (!Thread.holdsLock(this)) {
                throw new ConcurrentModificationException('containsKey requires the map monitor')
            }
            super.containsKey(key)
        }

        @Override
        V put(K key, V value) {
            if (!Thread.holdsLock(this)) {
                throw new ConcurrentModificationException('put requires the map monitor')
            }
            super.put(key, value)
        }
    }

    @Unroll
    def "create plugin, required input"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        Map<String, String> input = [
                dir                  : gitdir.absolutePath,
                pathTemplate         : '${job.group}${job.name}-${job.id}.xml',
                branch               : 'master',
                committerName        : 'test user',
                committerEmail       : 'test@example.com',
                strictHostKeyChecking: 'yes',
                format               : 'xml',
                url                  : new File(tempdir, 'origin'),
        ]
        input.remove(requiredInputName)

        when:
        def config = Config.create(Export, input)

        then:
        ScmPluginInvalidInput e = thrown()
        e.message == requiredInputName + ' cannot be null'
        e.report.errors[requiredInputName] == 'cannot be null'



        where:
        _ | requiredInputName
        _ | 'dir'
        _ | 'pathTemplate'
        _ | 'branch'
        _ | 'committerName'
        _ | 'committerEmail'
        _ | 'url'
        _ | 'strictHostKeyChecking'
        _ | 'format'

    }

    def "create plugin, ok"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        git.close()

        def context = Mock(ScmOperationContext)
        when:
        def plugin = new GitExportPlugin(config)
        plugin.initialize(context)

        then:
        gitdir.isDirectory()
        new File(gitdir, '.git').isDirectory()
        openGit(gitdir).repository.getFullBranch()=='refs/heads/master'

    }

    def "create plugin, using config.format in the path template"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(
                gitdir,
                origindir,
                [
                        pathTemplate: 'blah.${config.format}',
                        format      : format
                ]
        )
        //create a git dir
        createGit(origindir).close()

        //create plugin
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        when:
        def path = plugin.mapper.fileForJob(Mock(JobReference))

        then:
        path == new File(gitdir, 'blah.' + format)

        where:
        format | _
        'xml'  | _
        'yaml' | _
    }

    def "create plugin, using sourceId in the path template"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(
                gitdir,
                origindir,
                [
                        pathTemplate      : '${job.sourceId}.x',
                        format            : 'xml',
                        exportUuidBehavior: 'original'
                ]
        )
        //create a git dir
        createGit(origindir).close()

        //create plugin
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        when:
        def path = plugin.mapper.fileForJob(Mock(JobScmReference) {
            getSourceId() >> sourceId
            getId() >> jobid
        }
        )

        then:
        path == new File(gitdir, '123.x')

        where:
        sourceId | jobid | _
        '123'    | '234' | _
        null     | '123' | _
    }


    def "re initialize plugin with new url"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def origin2dir = new File(tempdir, 'origin2')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()

        def context = Mock(ScmOperationContext)

        //first init with origin1
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        //add loose file in working dir
        def testfile=new File(gitdir,'test-file')
        testfile<<'test'


        //create origin2
        Export config2 = createTestConfig(gitdir, origin2dir)
        def git2 = createGit(origin2dir)
        def commit2 = addCommitFile(origin2dir, git2, 'testcommit.txt', 'blee')
        git2.close()

        def plugin = new GitExportPlugin(config2)

        when:
        plugin.initialize(context)

        then:
        gitdir.isDirectory()
        new File(gitdir, '.git').isDirectory()
        //loose file has been removed
        !testfile.exists()
        new File(gitdir,'testcommit.txt').exists()
        new File(gitdir,'testcommit.txt').text=='blee'
    }

    def "re initialize plugin with new branch"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()

        def context = Mock(ScmOperationContext)

        //first init with origin1
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        //add loose file in working dir
        def testfile=new File(gitdir,'test-file')
        testfile<<'test'


        //create dev branch
        def git2 = openGit(origindir)
        git2.branchCreate().setName('dev').call()
        git2.checkout().setName('dev').call()
        def commit2 = addCommitFile(origindir, git2, 'testcommit.txt', 'blee')
        git2.close()

        Export config2 = createTestConfig(gitdir, origindir,[
                branch:'dev'
        ])
        def plugin = new GitExportPlugin(config2)

        when:
        plugin.initialize(context)

        then:
        gitdir.isDirectory()
        new File(gitdir, '.git').isDirectory()
        //loose file has been removed
        !testfile.exists()
        new File(gitdir,'testcommit.txt').exists()
        new File(gitdir,'testcommit.txt').text=='blee'
        openGit(gitdir).repository.getFullBranch()=='refs/heads/dev'
    }
    def "re initialize plugin with same branch"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()

        def context = Mock(ScmOperationContext)

        //first init with origin1
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        //add loose file in working dir
        def testfile=new File(gitdir,'test-file')
        testfile<<'test'


        //create dev branch
        def git2 = openGit(origindir)
        git2.branchCreate().setName('dev').call()
        git2.checkout().setName('dev').call()
        def commit2 = addCommitFile(origindir, git2, 'testcommit.txt', 'blee')
        git2.close()

        def plugin = new GitExportPlugin(config)

        when:
        plugin.initialize(context)

        then:
        gitdir.isDirectory()
        new File(gitdir, '.git').isDirectory()
        //loose file has been removed
        testfile.exists()
        new File(gitdir,'testcommit.txt').exists()
        new File(gitdir,'testcommit.txt').text=='blah'
        openGit(gitdir).repository.getFullBranch()=='refs/heads/master'
    }

    def "get input view for commit action"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def context = Mock(ScmOperationContext)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(context)
        def path = 'testfile'
        def localfile = new File(gitdir, path)
        localfile << 'blah'

        when:
        def view = plugin.getInputViewForAction(context, GitExportPlugin.JOB_COMMIT_ACTION_ID)

        then:
        view.title == "Commit Changes to Git"
        view.actionId == GitExportPlugin.JOB_COMMIT_ACTION_ID
        view.properties.size() == 3
        view.properties*.name == [
                CommitJobsAction.P_MESSAGE,
                TagAction.P_TAG_NAME,
                CommitJobsAction.P_PUSH
        ]

    }

    static Export createTestConfig(File gitdir, File origindir, Map<String, String> override = [:]) {
        Map<String, String> input = [
                dir                  : gitdir.absolutePath,
                pathTemplate         : '${job.group}${job.name}-${job.id}.xml',
                branch               : 'master',
                committerName        : 'test user',
                committerEmail       : 'test@example.com',
                format               : 'xml',
                strictHostKeyChecking: 'yes',
                url                  : origindir.absolutePath
        ] + override
        def config = Config.create(Export, input)
        config
    }

    def "get job status, does not exist in repo"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: stripUuid ? 'remove' : 'preserve'])

        //create a git dir
        def git = createGit(origindir)

        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        when:
        def status = plugin.getJobStatus(jobref)

        then:
        status != null
        status.synchState == SynchState.CREATE_NEEDED
        status.commit == null
        1 * serializer.serialize('xml', _, (!stripUuid), null) >> { args ->
            args[1].write('data'.bytes)
        }
        0 * serializer.serialize(*_)

        where:
        stripUuid | _
        true      | _
        false     | _
    }

    def "get job status, exists in repo"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'a/b/name-xyz.xml', 'blah')

        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        when:
        def status = plugin.getJobStatus(jobref)

        then:
        status != null
        status.synchState == state
        status.commit != null
        status.commit.asMap().authorTimeZone != null
        status.commit.asMap().date != null
        status.commit.asMap().authorTime != null

        status.commit.asMap().message == 'test commit'
        status.commit.asMap().authorEmail == 'test@example.com'
        status.commit.asMap().authorName == 'test user1'
        status.commit.asMap().commitId == commit.name
        status.commit.asMap().commitId6 == commit.abbreviate(6).name()
        1 * serializer.serialize('xml', _, _,_) >> {
            it[1].write(contents.bytes)
        }
        0 * serializer.serialize(*_)

        where:
        contents | state
        'bloo'   | SynchState.EXPORT_NEEDED
        'blah'   | SynchState.CLEAN
    }

    def "get file diff, new content"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        if (orig) {
            def commit = addCommitFile(origindir, git, 'a/b/name-xyz.xml', orig)
        }

        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobExportReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        when:
        def diff = plugin.getFileDiff(jobref)

        then:
        diff != null
        diff.modified == modified
        diff.newNotFound == newNotFound
        diff.oldNotFound == oldNotFound
        diff.content == (modified ? """@@ -1 +1 @@
-${orig}+${contents}""" : orig ? '' : null)

        1 * serializer.serialize('xml', _, _,_) >> {
            it[1].write(contents.bytes)
        }
        0 * serializer.serialize(*_)

        where:
        orig     | contents | modified | newNotFound | oldNotFound
        'blah\n' | 'bloo\n' | true     | false       | false
        'blah\n' | 'blah\n' | false    | false       | false
        null     | 'blah\n' | false    | false       | true

    }

    def "get status clean"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        when:
        def status = plugin.getStatus(Mock(ScmOperationContext))

        then:
        status!=null
        status.state==SynchState.CLEAN
        status.message==null

    }

    def "get status clean with a configured fetch timeout"() {
        // A custom fetchTimeout must be plumbed through to the JGit fetch/pull commands
        // (BaseGitPlugin#fetchFromRemote/gitPull, RUN-4525) without breaking a normal,
        // fast local fetch against a reachable remote.
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [fetchTimeout: '5'])

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        when:
        def status = plugin.getStatus(Mock(ScmOperationContext))

        then:
        status!=null
        status.state==SynchState.CLEAN
        status.message==null
    }
    def "get status fetch fails throws ScmPluginException"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        //delete origin contents, will cause fetch to fail
        FileUtils.delete(origindir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS)

        when:
        plugin.getStatus(Mock(ScmOperationContext))

        then:
        // A fetch failure signals the remote is unreachable — throw so the caller can surface a
        // clear "Git server unavailable" message instead of a misleading local-state status.
        ScmPluginException e = thrown()
        e.message == 'Fetch from the repository failed: Could not access the Git repository. See the server log for details.'
    }

    static RevCommit addCommitFile(final File gitdir, final Git git, final String path, final String content) {
        def outfile = new File(gitdir, path)
        outfile.parentFile.mkdirs()
        outfile.withOutputStream {
            it.write(content.bytes)
        }
        git.add().addFilepattern(path).call()

        CommitCommand commit1 = git.commit().setMessage('test commit').setCommitter(new PersonIdent(
                'test user1',
                'test@example.com'
        )
        )
        commit1.setOnly(path)
        commit1.call()
    }

    static RevCommit renameCommitFile(final File gitdir, final Git git, final String oldPath, final String newPath, final String content) {
        def outfile = new File(gitdir, oldPath)
        def newfile = new File(gitdir, newPath)
        outfile.renameTo(newfile)
        newfile.withOutputStream {
            it.write(content.bytes)
        }
        outfile.delete()
        git.add().addFilepattern(oldPath).call()
        git.add().addFilepattern(newPath).call()

        CommitCommand commit1 = git.commit().setMessage('rename commit').setCommitter(new PersonIdent(
                'test user1',
                'test@example.com'
        )
        )
        commit1.setOnly(oldPath)
        commit1.setOnly(newPath)
        commit1.call()
    }

    def "get local file and path for job"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [pathTemplate: template])

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))


        def jobref = Stub(JobRevReference) {
            getJobName() >> 'name'
            getGroupPath() >> groupPath
            getId() >> 'xyz'
        }
        when:
        def result = plugin.getLocalFileForJob(jobref)
        def relative = plugin.getRelativePathForJob(jobref)

        then:
        result == new File(gitdir, path)
        relative == path


        where:
        groupPath | path               | template
        'a/b'     | 'a/b/name-xyz.xml' | '${job.group}${job.name}-${job.id}.xml'
        'a'       | 'a/name-xyz.xml'   | '${job.group}${job.name}-${job.id}.xml'
        ''        | 'name-xyz.xml'     | '${job.group}${job.name}-${job.id}.xml'
        null      | 'name-xyz.xml'     | '${job.group}${job.name}-${job.id}.xml'

        'a/b'     | 'job-xyz.xml'      | 'job-${job.id}.xml'
        'a'       | 'job-xyz.xml'      | 'job-${job.id}.xml'
        ''        | 'job-xyz.xml'      | 'job-${job.id}.xml'
        null      | 'job-xyz.xml'      | 'job-${job.id}.xml'
    }

    def "get local file and path for job with sourceId"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [pathTemplate: template])

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))


        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> groupPath
            getId() >> 'abc'
            getSourceId() >> 'xyz'
        }
        when:
        def result = plugin.getLocalFileForJob(jobref)
        def relative = plugin.getRelativePathForJob(jobref)

        then:
        result == new File(gitdir, path)
        relative == path


        where:
        groupPath | path               | template
        'a/b'     | 'a/b/name-xyz.xml' | '${job.group}${job.name}-${job.sourceId}.xml'
        'a'       | 'a/name-xyz.xml'   | '${job.group}${job.name}-${job.sourceId}.xml'
        ''        | 'name-xyz.xml'     | '${job.group}${job.name}-${job.sourceId}.xml'
        null      | 'name-xyz.xml'     | '${job.group}${job.name}-${job.sourceId}.xml'

        'a/b'     | 'job-xyz.xml'      | 'job-${job.sourceId}.xml'
        'a'       | 'job-xyz.xml'      | 'job-${job.sourceId}.xml'
        ''        | 'job-xyz.xml'      | 'job-${job.sourceId}.xml'
        null      | 'job-xyz.xml'      | 'job-${job.sourceId}.xml'
    }

    static Git createGit(final File file) {
        Git.init().setDirectory(file).call()
    }
    static Git openGit(final File base) {
       def arepo = new FileRepositoryBuilder().setGitDir(new File(base, ".git")).setWorkTree(base).build()
       new Git(arepo)
    }


    @Unroll
    def "scm state for status"(Map data, String scmStatus) {
        given:

        def ltemp = File.createTempFile("GitExportPluginSpec", "-test")
        ltemp.delete()
        def gitdir = new File(ltemp, 'scm')
        def origindir = new File(ltemp, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        def ctxt = Mock(ScmOperationContext)
        //create a git dir
        createGit(origindir)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        def git = Git.open(gitdir)
        def path = 'testfile'
        def localfile = new File(gitdir, path)

        def revCommit = null

        if (data.mkcommit) {
            //commit the file
            localfile.createNewFile()
            localfile << 'testout'
            git.add().addFilepattern(path).call()
            //println(plugin.debugStatus(git.status().call()))
            revCommit = git.commit().setOnly(path).setMessage('test1').setCommitter('test', 'test@example.com').call()
        }


        if (data.create) {
            localfile << 'newdata'
        } else if (data.remove) {
            localfile.delete()
        } else if (data.untrack) {
            //commit the removal from the index without touching the file on disk: status.removed
            //only covers a *pending* (uncommitted) removal, so the removal must itself be
            //committed for git status to report the still-present file as untracked rather than
            //removed - matching the committed-but-untracked case the UNTRACKED branch covers
            git.rm().setCached(true).addFilepattern(path).call()
            revCommit = git.commit().setMessage('untrack').setCommitter('test', 'test@example.com').call()
        }
        def status = git.status().addPath(path).call()

        git.close()


        when:
        def result = plugin.scmStateForStatus(status, revCommit, path)
        if (ltemp.exists()) {
            FileUtils.delete(ltemp, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS)
        }

        then:
        result == scmStatus


        where:
        data                            | scmStatus
        [:]                             | 'NOT_FOUND'
        [create: true]                  | 'NEW'
        [mkcommit: true, create: true]  | 'MODIFIED'
        [mkcommit: true, remove: true]  | 'DELETED'
        [mkcommit: true, untrack: true] | 'UNTRACKED'
    }


    def "job commit with no changes"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        def jobref = Stub(JobExportReference)

        def input = [:]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [] as Set, [] as Set, input)

        then:
        ScmPluginException e = thrown()
        e.message == 'No changes to local git repo need to be exported'

    }

    def "job commit with missing commit message"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def localfile = new File(gitdir, 'blah')
        localfile << 'blah'

        def jobref = Stub(JobExportReference)
        def input = [:]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [jobref] as Set, [] as Set, input)

        then:
        ScmPluginException e = thrown()
        e.message == "A ${CommitJobsAction.P_MESSAGE} is required".toString()
    }

    def "commit job no local changes"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)


        def jobref = Stub(JobExportReference)
        def input = [:]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [jobref] as Set, [] as Set, input)

        then:
        ScmPluginException e = thrown()
        e.message == 'No changes to local git repo need to be exported'
    }

    def "commit missing jobs and paths"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def localfile = new File(gitdir, 'blah')
        localfile << 'blah'


        def input = [commitMessage: "test"]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [] as Set, [] as Set, input)

        then:
        ScmPluginException e = thrown()
        e.message == 'No jobs were selected'
    }

    def "commit with deleted paths that do not exist in repo"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah', 'blah')
        def localfile = new File(gitdir, 'blah')
        localfile<<'newtext'


        def input = [message: "test"]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [] as Set, ['dne'] as Set, input)

        then:
        result!=null
        result.success
        !result.id
        result.message=='No git changes needed'
    }

    def "an explicitly requested tag is still created even when there are no git changes to commit"() {
        given: "the same 'nothing to commit for this request' situation as above"

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah', 'blah')
        def localfile = new File(gitdir, 'blah')
        localfile << 'newtext'

        def input = [message: "test", tagName: "v-no-changes"]
        when: "a tag is explicitly requested alongside the commit action"
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [] as Set, ['dne'] as Set, input)

        then: "the tag is created, not silently skipped because there was nothing new to commit"
        result != null
        result.success
        GitUtil.findTag('v-no-changes', plugin.git) != null
    }

    def "an explicitly requested tag and push both still happen when there are no git changes to commit"() {
        given: "the same 'nothing to commit for this request' situation as above, but with" +
                " push also requested - performTagAndPush's nothingCommittedAndNoPushRequested" +
                " check must not short-circuit before the push when push IS requested"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah', 'blah')
        def localfile = new File(gitdir, 'blah')
        localfile << 'newtext'

        def input = [message: "test", tagName: "v-no-changes-push", (CommitJobsAction.P_PUSH): 'true']
        when: "a tag and a push are both explicitly requested alongside the commit action"
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [] as Set, ['dne'] as Set, input)

        then: "the tag was created locally, and the result reflects the push, not just the commit"
        GitUtil.findTag('v-no-changes-push', plugin.git) != null
        result != null
        result.success
        result.message.startsWith('Remote push result:')
    }

    def "an explicitly requested push happens even when there is nothing to commit and no tag is requested"() {
        given: "the pathcount==0 branch (CommitJobsAction.perform's 'No git changes needed' case)" +
                " used to return immediately without calling performTagAndPush at all, silently" +
                " skipping an explicitly requested push - this covers that branch with push alone," +
                " since the tag+push test above only exercises the push reached via the tag block"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah', 'blah')
        def localfile = new File(gitdir, 'blah')
        localfile << 'newtext'

        def input = [message: "test", (CommitJobsAction.P_PUSH): 'true']
        when: "no tag is requested, only a push, with nothing selected to commit or delete"
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [] as Set, ['dne'] as Set, input)

        then: "the push still happens - the result reflects it, not just 'No git changes needed'"
        result != null
        result.success
        result.message.startsWith('Remote push result:')
    }

    def "commit with invalid tag should fail at validation without committing"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')
        localfile<<'newtext'


        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobExportReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        def input = [message: "test", tagName: tagName]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [jobref] as Set, [] as Set, input)

        then:
        ScmPluginInvalidInput e = thrown()
        e.report.errors['tagName'] == 'Tag name is not valid: ' + tagName
        0 * serializer.serialize(*_)

        where:
        tagName     | _
        'asdf asdf' | _
    }

    def "tag action with invalid tag should fail at validation"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')
//        localfile<<'newtext'

        def input = [message: "test", tagName: tagName]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.PROJECT_TAG_ACTION_ID, [] as Set, [] as Set, input)

        then:
        ScmPluginInvalidInput e = thrown()
        e.report.errors['tagName'] == 'Tag name is not valid: ' + tagName

        where:
        tagName     | _
        'asdf asdf' | _
    }
    def "push action with invalid tag should fail at validation"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')
//        localfile<<'newtext'

        def input = [message: "test", tagName: tagName]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.PROJECT_PUSH_ACTION_ID, [] as Set, [] as Set, input)

        then:
        ScmPluginInvalidInput e = thrown()
        e.report.errors['tagName'] == 'Tag name is not valid: ' + tagName

        where:
        tagName     | _
        'asdf asdf' | _
    }

    def "push action treats up-to-date remote updates as success"() {
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        def originGit = createGit(origindir)
        def initialCommit = addCommitFile(origindir, originGit, 'blah-xyz.xml', 'blah')
        originGit.close()

        def ctxt = Mock(ScmOperationContext)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        when:
        def result = plugin.export(ctxt, GitExportPlugin.PROJECT_PUSH_ACTION_ID, [] as Set, [] as Set, [:])

        then:
        result.success
        result.id == initialCommit.name
        result.commit.commitId == initialCommit.name
        result.message == "Remote push result: OK. (Commit: ${initialCommit.name})"
    }
    def "commit missing user info"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [
                committerName : '${user.fullName}',
                committerEmail: '${user.email}',]
        )

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo) {
            getFullName() >> userName
            getEmail() >> userEmail
        }
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def localfile = new File(gitdir, 'blah')
        localfile << 'blah'

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobExportReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        def input = [message: "Test"]
        when:
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [jobref] as Set, [] as Set, input)

        then:
        ScmUserInfoMissing e = thrown()
        e.fieldName == expectedMissing

        where:
        userName | userEmail | expectedMissing
        null     | null      | 'committerName'
        'bob'    | null      | 'committerEmail'
        null     | 'a@b'     | 'committerName'
    }

    def "cleanup closes the current repository"() {
        given: "cleanup() used to call git.close(), a no-op once closeRepo=false (true for every" +
                " repo reload except the very first clone) - it must close the Repository directly"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def plugin = new GitExportPlugin(config)
        def repo = Mock(BaseGitPluginSpec.RepositoryMock)
        plugin.git = new Git(repo)

        when:
        plugin.cleanup()

        then:
        1 * repo.close()
    }

    def "job change delete removes file"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> jobref
            getJobReference() >> jobref
            getEventType() >> JobChangeEvent.JobChangeEventType.DELETE
        }

        when:
        def result = plugin.jobChanged(event, jobref)

        then:
        !localfile.exists()
        plugin.jobStateMap['xyz'] == null
        result != null
        result.synchState == SynchState.EXPORT_NEEDED
    }

    def "job change delete logs and continues, without throwing, when the file cannot be deleted"() {
        given: "the job's file is a non-empty directory, so File.delete() returns false"

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def localfile = new File(gitdir, 'blah-xyz.xml')
        localfile.mkdirs()
        new File(localfile, 'child').text = 'blocks deletion'

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> jobref
            getJobReference() >> jobref
            getEventType() >> JobChangeEvent.JobChangeEventType.DELETE
        }

        when: "the deletion fails"
        def result = plugin.jobChanged(event, jobref)

        then: "no exception propagates (it would abort sibling listeners/events - see" +
                " JobEventsService.jobChanged/multiJobChanged), and the file isn't reported clean"
        noExceptionThrown()
        localfile.exists()
        result != null
        result.synchState == SynchState.EXPORT_NEEDED
        //the job's Rundeck record is already gone, so nothing will ever look this id up again -
        //don't leave a permanent per-failure entry behind (a small but real leak)
        plugin.jobStateMap['xyz'] == null
    }

    def "job change delete with a failed file removal replaces a stale CLEAN cache entry"() {
        given: "the job's cache already says CLEAN (from before this failed delete attempt)," +
                " and the file is a non-empty directory so File.delete() returns false"

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def localfile = new File(gitdir, 'blah-xyz.xml')
        localfile.mkdirs()
        new File(localfile, 'child').text = 'blocks deletion'
        plugin.jobStateMap['xyz'] = [synch: SynchState.CLEAN]

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> jobref
            getJobReference() >> jobref
            getEventType() >> JobChangeEvent.JobChangeEventType.DELETE
        }

        when: "the deletion fails"
        def result = plugin.jobChanged(event, jobref)

        then: "the stale CLEAN entry is replaced, not left in place - and not left cached forever"
        noExceptionThrown()
        result.synchState == SynchState.EXPORT_NEEDED
        plugin.jobStateMap['xyz'] == null
    }

    def "job change modify overwrites file"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')

        def serializer = Mock(JobSerializer) {
            1 * serialize('xml', _, _,_) >> { args ->
                args[1].write('newcontent'.bytes)
                return 10
            }
        }
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> jobref
            getJobReference() >> jobref
            getEventType() >> theEventType
        }

        when:
        def result = plugin.jobChanged(event, jobref)

        then:
        localfile.exists()
        localfile.text == 'newcontent'
        plugin.jobStateMap['xyz'] != null
        result != null
        result.synchState == SynchState.EXPORT_NEEDED

        where:
        theEventType                             | _
        JobChangeEvent.JobChangeEventType.MODIFY | _
    }

    def "a single job's in-flight status refresh does not report project-wide LOADING"() {
        given: "only the one-time bulk initJobsStatus()/refreshJobsStatus() sequence should make" +
                " getStatusInternal report the whole project as LOADING. An ordinary single-job" +
                " refresh (a status poll, a commit's cleanJobStatusCache, a jobChanged) installs" +
                " its own per-job LOADING placeholder in jobStateMap too, but that must not be" +
                " mistaken for the project-wide condition: doing so made getStatus (and anything" +
                " built on it, e.g. actionsAvailableForContext, CommitJobsAction, PushAction)" +
                " report LOADING with no git status attached for the duration of any unrelated" +
                " job's refresh"

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah')
        git.close()

        def ctxt = Mock(ScmOperationContext)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        //another job's change is already sitting uncommitted in the working tree - unrelated to
        //the job being refreshed below, but it's what CommitJobsAction would be invoked to commit
        new File(gitdir, 'a/b/otherjob-uncommitted.xml').text = 'needs commit'

        GitExportSynchState statusDuringRefresh = null
        def actionsDuringRefresh = null
        boolean ownPlaceholderStillLoadingDuringRefresh = false
        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'testjob'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getSourceId() >> '123'
            getVersion() >> 1L
            //a stored commit id that doesn't match local HEAD forces doRefreshJobStatus into its
            //serialize() branch, which only runs while this job's LOADING placeholder is installed
            getScmImportMetadata() >> ["commitId": "mismatched-commit-id"]
            getJobSerializer() >> serializer
        }

        when:
        def result = plugin.refreshJobStatus(jobref, 'a/b/testjob-xyz.xml')

        then: "project status and actions stayed real (not LOADING/empty) while this job's refresh was in flight"
        1 * serializer.serialize('xml', !null, true, null) >> { args ->
            //captured mid-refresh, while this job's own jobStateMap entry is still a placeholder
            ownPlaceholderStillLoadingDuringRefresh = plugin.jobStateMap['xyz'].synch == SynchState.LOADING
            statusDuringRefresh = plugin.getStatusInternal(ctxt, false)
            actionsDuringRefresh = plugin.actionsAvailableForContext(Stub(ScmOperationContext) {
                getFrameworkProject() >> 'testproj'
            })
            args[1].write('data'.bytes)
        }
        ownPlaceholderStillLoadingDuringRefresh
        statusDuringRefresh != null
        statusDuringRefresh.state != SynchState.LOADING
        //a real gitStatus is attached - CommitJobsAction/PushAction read this without NPE
        statusDuringRefresh.gitStatus != null
        !statusDuringRefresh.gitStatus.isClean()
        //the other job's uncommitted change is still visible and actionable
        actionsDuringRefresh*.id == [GitExportPlugin.PROJECT_COMMIT_ACTION_ID]

        and: "the real result replaces the placeholder once the refresh completes"
        result != null
        result.synch == SynchState.EXPORT_NEEDED
        plugin.getStatusInternal(ctxt, false).state != SynchState.LOADING
    }

    def "project status is LOADING only during the one-time bulk initJobsStatus/refreshJobsStatus sequence"() {
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah')
        git.close()

        def ctxt = Mock(ScmOperationContext)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        def jobref = Stub(JobScmReference) {
            getJobName() >> 'testjob'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getSourceId() >> '123'
            getVersion() >> 1L
            getScmImportMetadata() >> [:]
            getJobSerializer() >> Stub(JobSerializer) {
                serialize(_, _, _, _) >> { args -> args[1].write('data'.bytes) }
            }
        }

        //the same list instance ScmLoaderService passes to both calls - refreshJobsStatus tells
        //this apart from an unrelated call (e.g. ImportJobs) by reference, not just contents
        def joblist = [jobref]

        expect: "LOADING before the bulk sequence starts is simply the uninitialized default"
        !plugin.bulkStatusLoading

        when:
        plugin.initJobsStatus(joblist)

        then: "the whole project is LOADING once the bulk placeholders are installed"
        plugin.bulkStatusLoading
        plugin.getStatusInternal(ctxt, false).state == SynchState.LOADING

        when:
        plugin.refreshJobsStatus(joblist)

        then: "LOADING clears once the bulk refresh completes"
        !plugin.bulkStatusLoading
        plugin.getStatusInternal(ctxt, false).state != SynchState.LOADING
    }

    def "refreshJobsStatus for an unrelated job list (e.g. ImportJobs after an import) does not clear an in-progress bulk load"() {
        given: "the bulk sequence is mid-flight (initJobsStatus ran, its matching refreshJobsStatus" +
                " hasn't yet) when an unrelated refreshJobsStatus call comes in for a different," +
                " equal-content-but-different-instance job list"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah')
        git.close()

        def ctxt = Mock(ScmOperationContext)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        def jobref = Stub(JobScmReference) {
            getJobName() >> 'testjob'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getSourceId() >> '123'
            getVersion() >> 1L
            getScmImportMetadata() >> [:]
            getJobSerializer() >> Stub(JobSerializer) {
                serialize(_, _, _, _) >> { args -> args[1].write('data'.bytes) }
            }
        }

        when:
        plugin.initJobsStatus([jobref])
        //a different List instance with the same content, as an unrelated caller would build
        plugin.refreshJobsStatus(new ArrayList([jobref]))

        then: "the bulk load is still reported in progress - the unrelated call didn't end it"
        plugin.bulkStatusLoading
        plugin.getStatusInternal(ctxt, false).state == SynchState.LOADING
    }

    def "initJobsStatus does not leave the project stuck LOADING forever if it throws"() {
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah')
        git.close()

        def ctxt = Mock(ScmOperationContext)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        def failingJobref = Stub(JobScmReference) {
            getId() >> { throw new RuntimeException('boom') }
        }

        when:
        plugin.initJobsStatus([failingJobref])

        then:
        thrown(RuntimeException)
        !plugin.bulkStatusLoading
        plugin.getStatusInternal(ctxt, false).state != SynchState.LOADING
    }

    def "job change serializer fails does not overwrite file"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')

        def serializer = Mock(JobSerializer) {
            1 * serialize('xml', _, _,_) >> { args ->
                throw new IllegalArgumentException('failure')
            }
        }
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> jobref
            getJobReference() >> jobref
            getEventType() >> theEventType
        }

        when:
        def result = plugin.jobChanged(event, jobref)

        then:
        localfile.exists()
        localfile.text == 'blah'
        plugin.jobStateMap['xyz'] != null
        result != null
        result.synchState == SynchState.CLEAN

        where:
        theEventType                             | _
        JobChangeEvent.JobChangeEventType.MODIFY | _
    }

    @Unroll
    def "job change create creates file #isStripUuid"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: isStripUuid ? 'remove' : 'preserve'])

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def localfile = new File(gitdir, 'blah-xyz.xml')

        def serializer = Mock(JobSerializer) {
            1 * serialize('xml', _, (!isStripUuid),_) >> { args ->
                args[1].write('newcontent'.bytes)
                return 10
            }
            0 * serialize(*_)

        }
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> jobref
            getJobReference() >> jobref
            getEventType() >> theEventType
        }

        when:
        def result = plugin.jobChanged(event, jobref)

        then:
        localfile.exists()
        localfile.text == 'newcontent'
        plugin.jobStateMap['xyz'] != null
        result != null
        result.synchState == SynchState.CREATE_NEEDED

        where:
        theEventType                             | isStripUuid
        JobChangeEvent.JobChangeEventType.CREATE | true
        JobChangeEvent.JobChangeEventType.CREATE | false
    }

    @Unroll
    def "job change create creates file with sourceId"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'original'])

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)

        def serializer = Mock(JobSerializer) {
            1 * serialize('xml', _, false, expectId) >> { args ->
                args[1].write('newcontent'.bytes)
                return 10
            }
            0 * serialize(*_)

        }
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
            getSourceId() >> sourceId
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> jobref
            getJobReference() >> jobref
            getEventType() >> JobChangeEvent.JobChangeEventType.CREATE
        }

        when:
        def localfile = new File(gitdir, "blah-xyz.xml")
        def result = plugin.jobChanged(event, jobref)

        then:
        localfile.exists()
        localfile.text == 'newcontent'
        plugin.jobStateMap['xyz'] != null
        result != null
        result.synchState == SynchState.CREATE_NEEDED

        where:
        sourceId | expectId
        null     | 'xyz'
        '123'    | '123'
    }

    def "job change modify-rename removes old and writes new file"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')
        def localnewfile = new File(gitdir, 'blah2-xyz.xml')

        def serializer = Mock(JobSerializer) {
            1 * serialize('xml', _, _,_) >> { args ->
                args[1].write('newcontent'.bytes)
                return 10
            }
        }
        def origref = Stub(JobExportReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah2'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> origref
            getJobReference() >> jobref
            getEventType() >> theEventType
        }

        when:
        def result = plugin.jobChanged(event, jobref)

        then:
        !localfile.exists()
        localnewfile.exists()
        localnewfile.text == 'newcontent'
        plugin.jobStateMap['xyz'] != null
        result != null
        result.synchState == SynchState.EXPORT_NEEDED

        where:
        theEventType                                    | _
        JobChangeEvent.JobChangeEventType.MODIFY_RENAME | _
    }
    def "job change modify-rename removes old and writes new file using sourceId"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir,[exportUuidBehavior: 'original',pathTemplate: '${job.group}${job.name}-${job.sourceId}.xml'])

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def ctxt = Mock(ScmOperationContext) {
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        def commit = addCommitFile(gitdir, plugin.git, 'blah-abc.xml', 'blah')
        def localfile = new File(gitdir, 'blah-abc.xml')
        def localnewfile = new File(gitdir, 'blah2-abc.xml')

        def serializer = Mock(JobSerializer) {
            1 * serialize('xml', _, false,'abc') >> { args ->
                args[1].write('newcontent'.bytes)
                return 10
            }
        }
        def origref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getSourceId() >> 'abc'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah2'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getSourceId() >> 'abc'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        JobChangeEvent event = Mock(JobChangeEvent) {
            getOriginalJobReference() >> origref
            getJobReference() >> jobref
            getEventType() >> theEventType
        }

        when:
        def result = plugin.jobChanged(event, jobref)

        then:
        !localfile.exists()
        localnewfile.exists()
        localnewfile.text == 'newcontent'
        plugin.jobStateMap['xyz'] != null
        result != null
        result.synchState == SynchState.EXPORT_NEEDED

        where:
        theEventType                                    | _
        JobChangeEvent.JobChangeEventType.MODIFY_RENAME | _
    }


    def "fix cluster status with new files behind"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
        ScmOperationContext context = Mock(ScmOperationContext)
        //create a git dir
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'blah-abc.xml', 'blah')
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        git = createGit(origindir)
        addCommitFile(origindir, git, 'blah-abc.xml', 'blah2')
        git.push()
        git.close()

        def gitScm = openGit(gitdir)
        plugin.fetchFromRemote(context, gitScm)


        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'abc'
            getSourceId() >> 'xyz'
            getScmImportMetadata() >> [commitId:'a']
        }

        when:
        def status = plugin.clusterFixJobs(context, [jobref])

        then:
        status != null
        status.deleted
        status.deleted.size() == 1
        status.deleted[0]=='a/b/name-abc.xml'
    }

    def "fix cluster status with clean status"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
        ScmOperationContext context = Mock(ScmOperationContext)

        //create a git dir
        def git = createGit(origindir)

        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'abc'
            getSourceId() >> 'xyz'
            getScmImportMetadata() >> [:]
        }

        when:
        def status = plugin.clusterFixJobs(context, [jobref])

        then:
        status != null
        status.deleted.size() == 0
        status.restored.size() == 0

    }
    def "fix cluster status with modified status"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
        ScmOperationContext context = Mock(ScmOperationContext)

        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah')
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah2')
        git.push()
        git.close()

        def gitScm = openGit(gitdir)
        plugin.fetchFromRemote(context, gitScm)

        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'abc'
            getSourceId() >> 'xyz'
            getScmImportMetadata() >> [commitId:'a']
        }

        when:
        def status = plugin.clusterFixJobs(context, [jobref])

        then:
        status != null
        status.deleted.size() == 0
        status.restored.size() == 1

    }

    def "cluster fix continues when the automatic pull fails"() {
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
        ScmOperationContext context = Mock(ScmOperationContext)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah')
        git.close()
        def failure = new ScmPluginException(
                'Could not access the Git repository. See the server log for details.',
                new TransportException('https', new IOException('config'))
        )
        def plugin = new GitExportPlugin(config) {
            @Override
            PullResult gitPull(ScmOperationContext operationContext, Git git1 = null) {
                throw failure
            }
        }
        plugin.initialize(Mock(ScmOperationContext))

        git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah2')
        git.push()
        git.close()

        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'abc'
            getScmImportMetadata() >> [:]
        }

        when:
        def status = plugin.clusterFixJobs(context, [jobref])

        then:
        status.pull
        status.error == failure
    }

    def "cluster fix continues when the automatic pull fails with a credential-bearing transport error"() {
        given: "remoteAccessFailure (what gitPull really calls) redacts a transport failure's" +
                " message - this confirms its redacted cause still satisfies" +
                " keepClusterPullFailure's causedBy(e, GitAPIException) check, so cluster" +
                " alignment records the failure and continues instead of rethrowing it"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
        ScmOperationContext context = Mock(ScmOperationContext)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah')
        git.close()
        def plugin = new GitExportPlugin(config) {
            @Override
            PullResult gitPull(ScmOperationContext operationContext, Git git1 = null) {
                throw remoteAccessFailure(
                        'Failed pull from the repository',
                        new TransportException(
                                'fatal: repository https://user:secret@example.com/repo.git not found'
                        )
                )
            }
        }
        plugin.initialize(Mock(ScmOperationContext))

        git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah2')
        git.push()
        git.close()

        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'abc'
            getScmImportMetadata() >> [:]
        }

        when:
        def status = plugin.clusterFixJobs(context, [jobref])

        then:
        status.pull
        status.error != null
        !status.error.message.contains('secret')
    }

    def "cluster fix still fails when the automatic pull fails for another reason"() {
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
        ScmOperationContext context = Mock(ScmOperationContext)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah')
        git.close()
        def failure = new ScmPluginException('other', new IllegalStateException('nope'))
        def plugin = new GitExportPlugin(config) {
            @Override
            PullResult gitPull(ScmOperationContext operationContext, Git git1 = null) {
                throw failure
            }
        }
        plugin.initialize(Mock(ScmOperationContext))

        git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/name-abc.xml', 'blah2')
        git.push()
        git.close()

        when:
        plugin.clusterFixJobs(context, [])

        then:
        def thrown = thrown(ScmPluginException)
        thrown.is(failure)
    }


    def "get job status, does not exist in repo, respect serialize true"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior:  'remove'])

        //create a git dir
        def git = createGit(origindir)

        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        when:
        def status = plugin.getJobStatus(jobref, null, true)

        then:
        status != null
        status.synchState == SynchState.CREATE_NEEDED
        status.commit == null
        1 * serializer.serialize('xml', _, (false), null) >> { args ->
            args[1].write('data'.bytes)
        }
        0 * serializer.serialize(*_)

    }

    def "Check if the user has permission to the ssh or password configured for the integration"() {
        given:
        def scmUserInfo = Mock(ScmUserInfo)
        def storageTree = Mock(StorageTreeImpl){
            it.hasPath(_) >> userAccess
        }
        def scmOperationContext = Mock(ScmOperationContext){
            getStorageTree() >> storageTree
            getUserInfo() >> scmUserInfo
        }
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior:  'remove'])

        //create a git dir
        def git = createGit(origindir)

        git.close()
        def common = Mock(Common){
            getSshPrivateKeyPath() >> 'keys/test'
        }
        def plugin = new GitExportPlugin(config)
        plugin.setCommonConfig(common)
        plugin.initialize(Mock(ScmOperationContext))

        when:
        hasAccess = plugin.userHasAccessToKeyOrPassword(scmOperationContext)

        then:
        hasAccess !== null

        where:
        userAccess | hasAccess
        true       | true
        false      | false

    }

    def "get job status, does not exist in repo, respect serialize false"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior:  'remove'])

        //create a git dir
        def git = createGit(origindir)

        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'name'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }
        when:
        def status = plugin.getJobStatus(jobref, null, false)

        then:
        status != null
        status.synchState == SynchState.CREATE_NEEDED
        status.commit == null
        0 * serializer.serialize('xml', _, (false), null) >> { args ->
            args[1].write('data'.bytes)
        }
        0 * serializer.serialize(*_)

    }

    def "get status pull automatically"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [fetchAutomatically: 'true',
                                                             pullAutomatically: 'true'])

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))

        when:
        def status = plugin.getStatus(Mock(ScmOperationContext))

        then:
        status!=null
        status.state==SynchState.CLEAN
        status.message=='Automatic pull from the repository failed: Failed pull from the repository: Remote origin did not advertise Ref for branch master. This Ref may not exist in the remote or may be hidden by permission settings.'
    }

    def "initialize plugin with unknown branch without create config"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()

        def context = Mock(ScmOperationContext)

        //first init with origin1
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        //add loose file in working dir
        def testfile=new File(gitdir,'test-file')
        testfile<<'test'


        //create dev branch
        def git2 = openGit(origindir)
        git2.branchCreate().setName('dev').call()
        git2.checkout().setName('dev').call()
        def commit2 = addCommitFile(origindir, git2, 'testcommit.txt', 'blee')
        git2.close()

        Export config2 = createTestConfig(gitdir, origindir,[
                branch:'dev2'
        ])
        def plugin = new GitExportPlugin(config2)

        when:
        plugin.initialize(context)

        then:
        ScmPluginException e = thrown()
        e.message=="Failed cloning the repository: Remote branch 'dev2' not found in upstream origin"
    }

    def "setup is a no-op on an already-initialized plugin, so cloneOrCreate never reruns against a live repo"() {
        given: "cloneOrCreate's repo.close() calls only ever run inside setup(), guarded by `inited`," +
                " on an instance the host hasn't published yet (ScmService's factory always constructs a" +
                " fresh plugin per load, before registering it where getStatus()/jobChanged()/actions can" +
                " reach it). If that guard were ever lost, a second setup() call on an already-published," +
                " in-use instance could close its live repo/git fields out from under a concurrent status" +
                " refresh or action - this locks in the guard that prevents it."
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()

        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))
        def repoAfterFirstSetup = plugin.repo
        def gitAfterFirstSetup = plugin.git

        when: "initialize (setup) is called again on the same, already-inited instance"
        plugin.initialize(Mock(ScmOperationContext))

        then: "it's a no-op - the live repo/git fields are untouched, not closed and replaced"
        plugin.repo.is(repoAfterFirstSetup)
        plugin.git.is(gitAfterFirstSetup)
    }

    def "initialize plugin creates branch when branch does not exist on remote"() {
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        Export config2 = createTestConfig(gitdir, origindir, [
                branch      : 'dev2',
                createBranch: 'true',
                baseBranch  : 'master'
        ])
        def context = Mock(ScmOperationContext)

        when:
        new GitExportPlugin(config2).initialize(context)

        then:
        noExceptionThrown()
        def remoteGit = openGit(origindir)
        def remoteBranches = remoteGit.branchList().call().collect { it.name }
        remoteGit.close()
        remoteBranches.contains('refs/heads/dev2')
    }

    def "initialize plugin with unknown branch with create config"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()

        def context = Mock(ScmOperationContext)

        //first init with origin1
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        //add loose file in working dir
        def testfile=new File(gitdir,'test-file')
        testfile<<'test'


        //create dev branch
        def git2 = openGit(origindir)
        git2.branchCreate().setName('dev').call()
        git2.checkout().setName('dev').call()
        def commit2 = addCommitFile(origindir, git2, 'testcommit.txt', 'blee')
        git2.close()

        Export config2 = createTestConfig(gitdir, origindir,[
                branch:'dev2',
                createBranch:'true',
                baseBranch: 'dev'

        ])
        def plugin = new GitExportPlugin(config2)

        when:
        plugin.initialize(context)

        then:
        noExceptionThrown()
        def remoteGit = openGit(origindir)
        def remoteBranches = remoteGit.branchList().call().collect { it.name }
        remoteGit.close()
        remoteBranches.contains('refs/heads/dev2')

    }

    def "re initialize with createBranch keeps uncommitted files when already on export branch"() {
        given: "SCM export with Create Branch enabled and Branch != Base Branch"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        Export exportConfig = createTestConfig(gitdir, origindir, [
                branch      : 'dev2',
                createBranch: 'true',
                baseBranch  : 'master'
        ])
        new GitExportPlugin(exportConfig).initialize(Mock(ScmOperationContext))

        // serialized-but-uncommitted job files, as written by the export plugin
        def pendingJob = new File(gitdir, 'a-job.xml')
        pendingJob << '<job>modified</job>'
        def tracked = new File(gitdir, 'testcommit.txt')
        tracked << '\nlocal edit'

        when: "ScmLoader creates a new plugin instance and initializes again"
        def plugin = new GitExportPlugin(exportConfig)
        plugin.initialize(Mock(ScmOperationContext))

        then: "workdir is not wiped; local job changes remain visible for export"
        pendingJob.exists()
        pendingJob.text == '<job>modified</job>'
        tracked.exists()
        tracked.text.contains('local edit')
        plugin.branch == 'dev2'
        openGit(gitdir).repository.getFullBranch() == 'refs/heads/dev2'
    }

    def "initialize plugin with create config does not recreate branch when it already exists on remote"() {
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        // pre-create dev2 on the remote with a distinct tip commit
        git.branchCreate().setName('dev2').call()
        git.checkout().setName('dev2').call()
        def dev2Commit = addCommitFile(origindir, git, 'dev2-marker.txt', 'only on dev2')
        def dev2TipBefore = dev2Commit.name()
        git.close()
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        Export config2 = createTestConfig(gitdir, origindir, [
                branch      : 'dev2',
                createBranch: 'true',
                baseBranch  : 'master'
        ])
        def plugin = new GitExportPlugin(config2)
        def context = Mock(ScmOperationContext)

        when:
        plugin.initialize(context)

        then:
        noExceptionThrown()
        plugin.branch == 'dev2'
        def remoteGit = openGit(origindir)
        def dev2TipAfter = remoteGit.repository.resolve('refs/heads/dev2').name
        remoteGit.close()
        dev2TipAfter == dev2TipBefore
    }

    def "initialize plugin with unknown branch with create config and bad remote"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'testcommit.txt', 'blah')
        git.close()

        def context = Mock(ScmOperationContext)

        //first init with origin1
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        //add loose file in working dir
        def testfile=new File(gitdir,'test-file')
        testfile<<'test'


        //create dev branch
        def git2 = openGit(origindir)
        git2.branchCreate().setName('dev').call()
        git2.checkout().setName('dev').call()
        def commit2 = addCommitFile(origindir, git2, 'testcommit.txt', 'blee')
        git2.close()

        Export config2 = createTestConfig(gitdir, origindir,[
                branch:'dev2',
                createBranch:'true',
                baseBranch: 'wrong'
        ])
        def plugin = new GitExportPlugin(config2)

        when:
        plugin.initialize(context)

        then:
        ScmPluginException e = thrown()
        e.message == "Non existent remote branch: wrong"
    }

    def "refresh status should re-serialize job if stored commit is different than local commit"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah')
        git.close()

        def context = Mock(ScmOperationContext)

        //first init with origin1
        new GitExportPlugin(config).initialize(Mock(ScmOperationContext))

        def plugin = new GitExportPlugin(config)
        plugin.initialize(context)

        def outfile = new File(origindir, 'a/b/testjob-xyz.xml')
        AtomicLong latch = new AtomicLong(-1)
        plugin.fileSerializeRevisionCounter.putIfAbsent(outfile, latch)

        def sourceId = "123"
        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'testjob'
            getGroupPath() >> 'a/b'
            getId() >> 'xyz'
            getSourceId() >> sourceId
            getVersion()>>1L
            getScmImportMetadata()>>["commitId":"123"]
            getJobSerializer() >> serializer

        }
        def originalPath = 'a/b/testjob-xyz.xml'

        when:
        def result = plugin.refreshJobStatus(jobref,originalPath )

        then:
        1 * serializer.serialize('xml', !null, true, null) >> { args ->
            args[1].write('data'.bytes)
        }

        result !=null
        result.synch ==SynchState.EXPORT_NEEDED

    }

    def "get job status, force  re-serialize if stored commit is different than local commit "() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)
        //create a git dir
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah')

        git.close()

        def context = Mock(ScmOperationContext)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(context)

        git = createGit(origindir)
        addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah2')
        git.push()
        git.close()

        def gitScm = openGit(gitdir)
        plugin.fetchFromRemote(context, gitScm)

        def outfile = new File(origindir, 'a/b/testjob-xyz.xml')
        AtomicLong latch = new AtomicLong(-1)
        plugin.fileSerializeRevisionCounter.putIfAbsent(outfile, latch)

        def jobId = "xyz"
        def commit = plugin.lastCommitForPath('a/b/testjob-xyz.xml')

        plugin.jobStateMap[jobId]= [id:jobId, ident: "${jobId}:1:${commit.name}:true", "synch": SynchState.CLEAN]

        def serializer = Mock(JobSerializer)
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'testjob'
            getGroupPath() >> 'a/b'
            getId() >> jobId
            getSourceId() >> "123"
            getVersion()>>1L
            getScmImportMetadata()>>["commitId":"123"]
            getJobSerializer() >> serializer
        }
        when:
        def status = plugin.clusterFixJobs(context, [jobref])

        then:
        status != null
        status.restored != null
        status.restored.size() == 1
        1 * serializer.serialize('xml', !null, _, null) >> { args ->
            args[1].write('data'.bytes)
        }
        0 * serializer.serialize(*_)

    }

    def "init job status sets loading status"(){
        given:
            def gitdir = new File(tempdir, 'scm')
            def origindir = new File(tempdir, 'origin')
            Export config = createTestConfig(gitdir, origindir)
            //create a git dir
            def git = createGit(origindir)
            addCommitFile(origindir, git, 'a/b/testjob-xyz.xml', 'blah')

            git.close()

            def plugin = new GitExportPlugin(config)
            plugin.initialize(Mock(ScmOperationContext))
            def jobrefs = [
                 Stub(JobExportReference) {
                    getJobName() >> 'name'
                    getGroupPath() >> 'a/b'
                    getId() >> 'xyz'
                    getVersion() >> 1
                }
            ]
        when:
            plugin.initJobsStatus(jobrefs)
        then: "job synch state is loading"
            plugin.jobStateMap['xyz'].synch==SynchState.LOADING
            plugin.jobStateMap['xyz'].id=='xyz'
            plugin.jobStateMap['xyz'].version==1


    }

    def "init job status synchronizes the check and insert on the job-state map"() {
        given:
        def plugin = new GitExportPlugin(Mock(Export))
        plugin.jobStateMap = new MonitorCheckedMap<String, Map>()
        def jobrefs = [
                Stub(JobExportReference) {
                    getId() >> 'xyz'
                    getVersion() >> 1
                }
        ]

        when:
        plugin.initJobsStatus(jobrefs)

        then:
        plugin.jobStateMap['xyz'].synch == SynchState.LOADING
    }

    def "Should delete directory if this exists no matter the OS"(){
        given:
            def gitdir = new File(tempdir, 'scm')
            def origindir = new File(tempdir, 'origin')
            Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
            ScmOperationContext context = Mock(ScmOperationContext)
            //create a git dir
            def git = createGit(origindir)
            addCommitFile(origindir, git, 'blah-abc.xml', 'blah')
            git.close()
            def plugin = new GitExportPlugin(config)
            plugin.initialize(Mock(ScmOperationContext))
            File base = new File(config.dir)

        when:
            plugin.totalClean()

        then:
            !base.exists()
    }

    def "Directory should not exists and should not call FileUtils.Delete method"(){
        given:
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir, [exportUuidBehavior: 'preserve'])
        ScmOperationContext context = Mock(ScmOperationContext)
        //create a git dir
        def git = createGit(origindir)
        addCommitFile(origindir, git, 'blah-abc.xml', 'blah')
        git.close()
        def plugin = new GitExportPlugin(config)
        plugin.initialize(Mock(ScmOperationContext))
        File base = new File("/test")

        when:
        plugin.totalClean()

        then:
        !base.exists()
        0 * FileUtils.delete(_, _)
    }

    def "cleanJobStatusCache refreshes stale cached status after commit"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)
        git.close()

        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }

        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        addCommitFile(gitdir, plugin.git, 'blah-xyz.xml', 'blah')
        def localfile = new File(gitdir, 'blah-xyz.xml')
        localfile << 'newtext'

        def serializer = Mock(JobSerializer) {
            _ * serialize('xml', _, _, _) >> { args -> args[1].write('newtext'.bytes) }
        }
        def jobref = Stub(JobScmReference) {
            getJobName() >> 'blah'
            getGroupPath() >> ''
            getId() >> 'xyz'
            getVersion() >> 1
            getJobSerializer() >> serializer
        }

        //populate the cache with the pre-commit "modified" status, as a real status check would
        def preCommitStatus = plugin.getJobStatus(jobref)

        when:
        def input = [message: "commit message", push: 'false']
        def result = plugin.export(ctxt, GitExportPlugin.JOB_COMMIT_ACTION_ID, [jobref] as Set, [] as Set, input)

        then:
        preCommitStatus.synchState == SynchState.EXPORT_NEEDED
        result.success
        //the cached status must be refreshed to reflect the commit, not left stale as EXPORT_NEEDED
        plugin.jobStateMap['xyz'].synch == SynchState.CLEAN
    }
}
