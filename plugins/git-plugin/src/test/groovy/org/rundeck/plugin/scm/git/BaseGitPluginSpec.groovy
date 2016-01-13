package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.plugins.scm.JobFileMapper
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import org.eclipse.jgit.api.Git
import org.rundeck.plugin.scm.git.config.Common
import org.rundeck.plugin.scm.git.config.Export
import spock.lang.Specification

/**
 * Created by greg on 10/14/15.
 */
class BaseGitPluginSpec extends Specification {
    def "getSshConfig"() {
        given:
        Common config = new Common(configInput)
        def base = new BaseGitPlugin(config)
        when:
        def sshConfig = base.sshConfig

        then:
        sshConfig == sshResult

        where:
        sshResult                      | configInput
        [:]                            | [:]
        [StrictHostKeyChecking: 'yes'] | [strictHostKeyChecking: 'yes']
        [StrictHostKeyChecking: 'no']  | [strictHostKeyChecking: 'no']
        [:]                            | [strictHostKeyChecking: 'ask']
        [:]                            | [strictHostKeyChecking: 'other']
    }

    def "serialize job to valid file path"() {
        given:
        Common config = new Common()
        def base = new BaseGitPlugin(config)
        base.mapper = Mock(JobFileMapper)
        def job = Mock(JobExportReference)
        def outfile = File.createTempFile("BaseGitPluginSpec", "serialize-job.temp")
        outfile.deleteOnExit()

        when:
        base.serialize(job, format)

        then:
        1 * base.mapper.fileForJob(_) >> outfile
        1 * job.getJobSerializer() >> Mock(JobSerializer) {
            1 * serialize(format, !null)>>{args->
                args[1].write('data'.bytes)
            }
        }


        where:
        format | _
        'xml'  | _
        'yaml' | _
    }

    def "serialize job: cannot create parent dir"() {
        given:
        Common config = new Common()
        def base = new BaseGitPlugin(config)
        base.mapper = Mock(JobFileMapper)
        def job = Mock(JobExportReference)
        def outfile1 = File.createTempFile("BaseGitPluginSpec", "serialize-job.temp")
        outfile1.deleteOnExit()

        //parent dir is actually a file, so mkdirs() will fail
        def outfile = new File(outfile1, "test")


        when:
        base.serialize(job, format)

        then:
        1 * base.mapper.fileForJob(_) >> outfile
        ScmPluginException e = thrown()
        e.message.startsWith('Cannot create necessary dirs to serialize file to path')

        where:
        format | _
        'xml'  | _
        'yaml' | _
    }

    def "serialize job: no output"() {
        given:
        Common config = new Common()
        def base = new BaseGitPlugin(config)
        base.mapper = Mock(JobFileMapper)
        def job = Mock(JobExportReference)
        def outfile = File.createTempFile("BaseGitPluginSpec", "serialize-job.temp")
        outfile.deleteOnExit()

        when:
        base.serialize(job, format)

        then:
        1 * base.mapper.fileForJob(_) >> outfile
        1 * job.getJobSerializer() >> Mock(JobSerializer) {
            1 * serialize(format, !null)//no write to stream
        }
        ScmPluginException e = thrown()
        e.message.startsWith('Failed to serialize job, no content was written')

        where:
        format | _
        'xml'  | _
        'yaml' | _
    }

    def "serialize job: IO exception"() {
        given:
        Common config = new Common()
        def base = new BaseGitPlugin(config)
        base.mapper = Mock(JobFileMapper)
        def job = Mock(JobExportReference)
        def outfile = File.createTempFile("BaseGitPluginSpec", "serialize-job.temp")
        outfile.deleteOnExit()

        when:
        base.serialize(job, format)

        then:
        1 * base.mapper.fileForJob(_) >> outfile
        1 * job.getJobSerializer() >> Mock(JobSerializer) {
            1 * serialize(format, !null)>>{
                throw new IOException("test forced error")
            }
        }
        ScmPluginException e = thrown()
        e.message.startsWith('Failed to serialize job')
        e.message.endsWith('test forced error')

        where:
        format | _
        'xml'  | _
        'yaml' | _
    }


    def "serialize temp"() {
        given:
        Common config = new Common()
        def base = new BaseGitPlugin(config)
        base.mapper = Mock(JobFileMapper)
        def job = Mock(JobExportReference)

        when:
        def outfile = base.serializeTemp(job, format)


        then:
        1 * job.getJobSerializer() >> Mock(JobSerializer) {
            1 * serialize(format, !null)
        }
        outfile != null
        outfile.isFile()


        where:
        format | _
        'xml'  | _
        'yaml' | _
    }

    def "fetch from remote clean"() {
        given:
        Common config = new Common()
        def base = new BaseGitPlugin(config)

        def tempdir = File.createTempFile("BaseGitPluginSpec", "-test")
        tempdir.delete()
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        //create a git dir
        createGit(origindir).close()
        def git = Git.cloneRepository().
                setRemote("origin").
                setBranch("master").
                setURI(origindir.absolutePath).
                setDirectory(gitdir).
                call()

        base.git = git

        when:
        def update = base.fetchFromRemote(Mock(ScmOperationContext))

        then:
        update == null
    }

    def "fetch from remote with changes"() {
        given:
        Common config = new Common()
        def base = new BaseGitPlugin(config)
        base.branch = 'master'

        def tempdir = File.createTempFile("BaseGitPluginSpec", "-test")
        tempdir.delete()
        def gitdir = new File(tempdir, 'scm')
        gitdir.mkdir()
        def origindir = new File(tempdir, 'origin')
        createGit(origindir).close()
        def remotegit = Git.open(origindir)
        def commit1 = GitExportPluginSpec.addCommitFile(origindir, remotegit, 'afile', 'blah')
        remotegit.close()

        //clone a git dir
        def git = Git.cloneRepository().
                setRemote("origin").
                setBranch("master").
                setURI(origindir.absolutePath).
                setDirectory(gitdir).
                call()

        base.git = git

        remotegit = Git.open(origindir)
        def commit = GitExportPluginSpec.addCommitFile(origindir, remotegit, 'afile', 'blah2')
        remotegit.close()

        when:
        def update = base.fetchFromRemote(Mock(ScmOperationContext))

        then:
        commit1 != null
        commit != null
        git != null
        git.repository.config.getString('branch', 'master', 'remote') == 'origin'
        base.remoteTrackingBranch(git) == 'refs/remotes/origin/master'
        update != null
        update.remoteName == 'refs/heads/master'
        update.localName == 'refs/remotes/origin/master'
        update.oldObjectId == commit1
        update.newObjectId == commit
    }


    static void setRemote(Git git, String remote, String url) {
        git.repository.config.setString("remote", remote, "url", url)
        git.repository.config.save()
    }

    static Git createGit(final File file) {
        Git.init().setDirectory(file).call()
    }


    def "expand user string"() {
        given:
        def userinfo = Stub(ScmUserInfo) {
            getUserName() >> 'Z'
            getFirstName() >> 'A'
            getLastName() >> 'B'
            getFullName() >> 'A B'
            getEmail() >> 'c@d.e'
        }

        expect:
        BaseGitPlugin.expand(input, userinfo) == result

        where:
        input                                                                           | result
        'Blah'                                                                          | 'Blah'
        '${user.userName}'                                                              | 'Z'
        '${user.login}'                                                                 | 'Z'
        '${user.fullName}'                                                              | 'A B'
        '${user.firstName}'                                                             | 'A'
        '${user.lastName}'                                                              | 'B'
        '${user.email}'                                                                 | 'c@d.e'
        'Bob ${user.firstName} x ${user.lastName} y ${user.email} H ${user.userName} I' | 'Bob A x B y c@d.e H Z I'
    }

    def "expand context vars in path"() {
        given:
        def userinfo = Stub(ScmUserInfo) {
            getUserName() >> 'Z'
            getFirstName() >> 'A'
            getLastName() >> 'B'
            getFullName() >> 'A B'
            getEmail() >> 'c@d.e'
        }
        def context = Stub(ScmOperationContext) {
            getUserInfo() >> userinfo
            getFrameworkProject() >> 'testproject'
        }

        expect:
        BaseGitPlugin.expandContextVarsInPath(context, path) == result

        where:
        path                           | result
        'a/b/c'                        | 'a/b/c'
        'a/${user.login}/c'            | 'a/Z/c'
        'a/${user.userName}/c'         | 'a/Z/c'
        'a/${user.firstName}/c'        | 'a/A/c'
        'a/${user.lastName}/c'         | 'a/B/c'
        'a/${user.fullName}/c'         | 'a/A B/c'
        'a/${user.email}/c'            | 'a/c@d.e/c'
        'a/${project}/c'               | 'a/testproject/c'
        'a/${project}/${user.login}/c' | 'a/testproject/Z/c'
    }

    def "expand user missing info"() {
        given:
        def userinfo = Stub(ScmUserInfo) {
        }

        expect:
        BaseGitPlugin.expand(input, userinfo) == result

        where:
        input                                                                           | result
        'Blah'                                                                          | 'Blah'
        '${user.userName}'                                                              | ''
        '${user.fullName}'                                                              | ''
        '${user.firstName}'                                                             | ''
        '${user.lastName}'                                                              | ''
        '${user.email}'                                                                 | ''
        'Bob ${user.firstName} x ${user.lastName} y ${user.email} H ${user.userName} I' | 'Bob  x  y  H  I'
    }

}
