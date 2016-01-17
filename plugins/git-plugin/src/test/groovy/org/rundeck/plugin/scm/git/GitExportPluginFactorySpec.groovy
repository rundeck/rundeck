package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.eclipse.jgit.util.FileUtils
import spock.lang.Specification

/**
 * Created by greg on 8/31/15.
 */
class GitExportPluginFactorySpec extends Specification {

    File tempdir

    def setup() {
        tempdir = File.createTempFile("GitExportPluginFactorySpec", "-test")
        tempdir.delete()
    }

    def cleanup() {
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE)
        }
    }

    def "base description"() {
        given:
        def factory = new GitExportPluginFactory()
        def desc = factory.description

        expect:
        desc.title == 'Git Export'
        desc.name == 'git-export'
        desc.properties.size() == 10
    }

    def "base description properties"() {
        given:
        def factory = new GitExportPluginFactory()
        def desc = factory.description

        expect:
        desc.properties*.name as Set == [
                'dir',
                'pathTemplate',
                'url',
                'branch',
                'strictHostKeyChecking',
                'sshPrivateKeyPath',
                'gitPasswordPath',
                'format',
                'committerName',
                'committerEmail',
        ] as Set
    }

    def "setup properties without basedir"() {
        given:
        def factory = new GitExportPluginFactory()
        def properties = factory.getSetupProperties()

        expect:
        properties*.name as Set == [
                'dir',
                'pathTemplate',
                'url',
                'branch',
                'strictHostKeyChecking',
                'sshPrivateKeyPath',
                'gitPasswordPath',
                'format',
                'committerName',
                'committerEmail',
        ] as Set
        def dirprop = properties.find { it.name == 'dir' }
        dirprop.defaultValue == null

    }

    def "setup properties with basedir"() {
        given:
        def factory = new GitExportPluginFactory()
        def tempdir = File.createTempFile("blah", "test")
        tempdir.deleteOnExit()
        tempdir.delete()
        def properties = factory.getSetupPropertiesForBasedir(tempdir)

        expect:
        properties*.name  as Set == [
                'dir',
                'pathTemplate',
                'url',
                'branch',
                'strictHostKeyChecking',
                'sshPrivateKeyPath',
                'gitPasswordPath',
                'format',
                'committerName',
                'committerEmail',
        ] as Set
        properties.find { it.name == 'dir' }.defaultValue == new File(tempdir.absolutePath, 'scm').absolutePath
    }

    def "create plugin"() {
        given:

        def factory = new GitExportPluginFactory()
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        Map<String,String> config = [
                dir                  : gitdir.absolutePath,
                pathTemplate         : '${job.group}${job.name}-${job.id}.xml',
                branch               : 'master',
                committerName        : 'test user',
                committerEmail       : 'test@example.com',
                strictHostKeyChecking: 'yes',
                format               : 'xml',
                url                  : origindir.absolutePath
        ]

        //create a git dir
        def git = GitExportPluginSpec.createGit(origindir)

        git.close()
        def ctxt = Mock(ScmOperationContext) {

        }
        when:
        def plugin = factory.createPlugin(ctxt, config)

        then:
        null != plugin

        gitdir.isDirectory()
        new File(gitdir, '.git').isDirectory()

    }
}
