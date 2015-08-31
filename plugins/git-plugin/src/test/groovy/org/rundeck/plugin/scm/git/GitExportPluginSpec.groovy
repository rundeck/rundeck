package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.SynchState
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.FileUtils
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
        def config = [
                dir           : gitdir.absolutePath,
                pathTemplate  : '${job.group}${job.name}-${job.id}.xml',
                branch        : 'master',
                committerName : 'test user',
                committerEmail: 'test@example.com',
                url           : new File(tempdir, 'origin'),
        ]
        config.remove(requiredInputName)

        when:
        def plugin = new GitExportPlugin(config, 'test')
        plugin.initialize()

        then:
        IllegalArgumentException e = thrown()
        e.message == requiredInputName + ' cannot be null'


        where:
        _ | requiredInputName
        _ | 'dir'
        _ | 'pathTemplate'
        _ | 'branch'
        _ | 'committerName'
        _ | 'committerEmail'
        _ | 'url'

    }

    def "create plugin, ok"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def config = [
                dir           : gitdir.absolutePath,
                pathTemplate  : '${job.group}${job.name}-${job.id}.xml',
                branch        : 'master',
                committerName : 'test user',
                committerEmail: 'test@example.com',
                url           : origindir
        ]

        //create a git dir
        def git = createGit(origindir)
        git.close()

        when:
        def plugin = new GitExportPlugin(config, 'test')
        plugin.initialize()

        then:
        gitdir.isDirectory()
        new File(gitdir, '.git').isDirectory()

    }

    def "get export properties"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def config = [
                dir           : gitdir.absolutePath,
                pathTemplate  : '${job.group}${job.name}-${job.id}.xml',
                branch        : 'master',
                committerName : 'test user',
                committerEmail: 'test@example.com',
                url           : origindir
        ]

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config, 'test')
        plugin.initialize()

        when:
        def props = plugin.getExportProperties([] as Set)

        then:
        props.size() == 3
        props*.name == [
                'commitMessage',
                'tagName',
                'push'
        ]

    }

    def "get job status, does not exist in repo"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def config = [
                dir           : gitdir.absolutePath,
                pathTemplate  : '${job.group}${job.name}-${job.id}.xml',
                branch        : 'master',
                committerName : 'test user',
                committerEmail: 'test@example.com',
                url           : origindir
        ]

        //create a git dir
        def git = createGit(origindir)

        git.close()
        def plugin = new GitExportPlugin(config, 'test')
        plugin.initialize()

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
        1 * serializer.serialize('xml', _)
        0 * serializer.serialize(*_)
    }

    def "get job status, exists in repo"() {
        given:

        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def config = [
                dir           : gitdir.absolutePath,
                pathTemplate  : '${job.group}${job.name}-${job.id}.xml',
                branch        : 'master',
                committerName : 'test user',
                committerEmail: 'test@example.com',
                url           : origindir
        ]

        //create a git dir
        def git = createGit(origindir)
        def commit = addCommitFile(origindir, git, 'a/b/name-xyz.xml', 'blah')

        git.close()
        def plugin = new GitExportPlugin(config, 'test')
        plugin.initialize()

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
        def config = [
                dir           : gitdir.absolutePath,
                pathTemplate  : '${job.group}${job.name}-${job.id}.xml',
                branch        : 'master',
                committerName : 'test user',
                committerEmail: 'test@example.com',
                url           : origindir
        ]

        //create a git dir
        def git = createGit(origindir)
        if (orig) {
            def commit = addCommitFile(origindir, git, 'a/b/name-xyz.xml', orig)
        }

        git.close()
        def plugin = new GitExportPlugin(config, 'test')
        plugin.initialize()

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


    RevCommit addCommitFile(final File gitdir, final Git git, final String path, final String content) {
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
        def config = [
                dir           : gitdir.absolutePath,
                pathTemplate  : template,
                branch        : 'master',
                committerName : 'test user',
                committerEmail: 'test@example.com',
                url           : origindir
        ]

        //create a git dir
        def git = createGit(origindir)
        git.close()
        def plugin = new GitExportPlugin(config, 'test')
        plugin.initialize()


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
}
