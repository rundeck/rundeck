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
        desc.properties.size() == 12
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
                'fetchAutomatically',
                'committerName',
                'committerEmail',
                'exportUuidBehavior'
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
                'fetchAutomatically',
                'committerName',
                'committerEmail',
                'exportUuidBehavior'
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
                'fetchAutomatically',
                'committerName',
                'committerEmail',
                'exportUuidBehavior',
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
                url                  : origindir.absolutePath,
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
