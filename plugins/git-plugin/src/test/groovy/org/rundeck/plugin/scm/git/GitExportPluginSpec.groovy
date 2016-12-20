package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import com.dtolabs.rundeck.plugins.scm.ScmUserInfoMissing
import com.dtolabs.rundeck.plugins.scm.SynchState
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.util.FileUtils
import org.rundeck.plugin.scm.git.config.Config
import org.rundeck.plugin.scm.git.config.Export
import org.rundeck.plugin.scm.git.exp.actions.CommitJobsAction
import org.rundeck.plugin.scm.git.exp.actions.TagAction
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 8/31/15.
 */
class GitExportPluginSpec extends Specification {

    File tempdir

    def setup() {
        tempdir = File.createTempFile("GitExportPluginSpec", "-test")
        tempdir.delete()
    }

    def cleanup() {
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE)
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
        Export config = createTestConfig(gitdir, origindir)

        //create a git dir
        def git = createGit(origindir)

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
        def status = plugin.getJobStatus(jobref)

        then:
        status != null
        status.synchState == SynchState.CREATE_NEEDED
        status.commit == null
        1 * serializer.serialize('xml', _)>>{args->
            args[1].write('data'.bytes)
        }
        0 * serializer.serialize(*_)
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
        def jobref = Stub(JobExportReference) {
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
        1 * serializer.serialize('xml', _) >> {
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

        1 * serializer.serialize('xml', _) >> {
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
    def "get status fetch fails"() {
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
        FileUtils.delete(origindir, FileUtils.RECURSIVE)

        when:
        def status = plugin.getStatus(Mock(ScmOperationContext))

        then:
        status!=null
        status.state==SynchState.REFRESH_NEEDED
        status.message=='Fetch from the repository failed: Invalid remote: origin'
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
        }
        def status = git.status().addPath(path).call()

        git.close()


        when:
        def result = plugin.scmStateForStatus(status, revCommit, path)
        if (ltemp.exists()) {
            FileUtils.delete(ltemp, FileUtils.RECURSIVE)
        }

        then:
        result == scmStatus


        where:
        data                           | scmStatus
        [:]                            | 'NOT_FOUND'
        [create: true]                 | 'NEW'
        [mkcommit: true, create: true] | 'MODIFIED'
        [mkcommit: true, remove: true] | 'DELETED'
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

    def "export job no local changes"() {
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

    def "export missing jobs and paths"() {
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

    def "export missing user info"() {
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
        def jobref = Stub(JobExportReference) {
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
            1 * serialize('xml', _) >> { args ->
                args[1].write('newcontent'.bytes)
                return 10
            }
        }
        def jobref = Stub(JobExportReference) {
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
            1 * serialize('xml', _) >> { args ->
                throw new IllegalArgumentException('failure')
            }
        }
        def jobref = Stub(JobExportReference) {
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

    def "job change create creates file"() {
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
        def localfile = new File(gitdir, 'blah-xyz.xml')

        def serializer = Mock(JobSerializer) {
            1 * serialize('xml', _) >> { args ->
                args[1].write('newcontent'.bytes)
                return 10
            }
        }
        def jobref = Stub(JobExportReference) {
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
        theEventType                             | _
        JobChangeEvent.JobChangeEventType.CREATE | _
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
            1 * serialize('xml', _) >> { args ->
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
        def jobref = Stub(JobExportReference) {
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
}
