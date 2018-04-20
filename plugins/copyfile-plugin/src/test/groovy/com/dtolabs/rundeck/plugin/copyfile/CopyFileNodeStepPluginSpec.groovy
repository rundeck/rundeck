/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.plugin.copyfile

import com.dtolabs.rundeck.core.common.FilesystemFramework
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFrameworkNodes
import com.dtolabs.rundeck.core.common.IFrameworkProjectMgr
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.utils.FileUtils
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

/**
 * @author greg
 * @since 3/31/17
 */
class CopyFileNodeStepPluginSpec extends Specification {
    Path testDir
    Path projectsDir
    Path copyDir
    Path destDir

    def setup() {
        testDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-test")
        projectsDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-projects")
        copyDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-test-files")
        destDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-test-dest")

    }

    def cleanup() {
        FileUtils.deleteDir(testDir.toFile())
        FileUtils.deleteDir(projectsDir.toFile())
        FileUtils.deleteDir(copyDir.toFile())
        FileUtils.deleteDir(destDir.toFile())
    }

    def makeDirFiles(Path dir, List<String> paths = []) {
        int i = 0
        for (String path : paths) {
            File file1 = new File(dir.toFile(), path)
            file1.getParentFile().mkdirs()
            file1.text = "$i test file"
            i++
        }
    }

    def "copy single file"() {
        given:
        def plugin = new CopyFileNodeStepPlugin()
        def copyfile = new File(copyDir.toFile(), "test1.txt")
        plugin.sourcePath = copyfile.getAbsolutePath()
        plugin.recursive = false
        plugin.pattern = null
        def destPath = destDir.toString() + '/'
        def expectPath = destDir.toString() + '/test1.txt'
        plugin.destinationPath = destPath

        def context = Mock(PluginStepContext) {
            getLogger() >> Mock(PluginLogger)
        }
        def node = Mock(INodeEntry)
        def execSvc = Mock(ExecutionService)
        def fkwScs = Mock(IFrameworkServices) {
            getExecutionService() >> execSvc
        }
        def mockFwk = testFramework(fkwScs)

        when:
        plugin.executeNodeStep(context, [:], node)
        then:


        1 * context.getFramework() >> mockFwk
        1 * execSvc.fileCopyFile(_, copyfile, node, expectPath)

    }


    def "copy recursive dir"() {
        given:
        def plugin = new CopyFileNodeStepPlugin()
        plugin.sourcePath = copyDir.toString()
        plugin.recursive = true
        plugin.pattern = null
        def destPath = destDir.toString() + '/'
        plugin.destinationPath = destPath

        def context = Mock(PluginStepContext) {
            getLogger() >> Mock(PluginLogger)
        }
        def node = Mock(INodeEntry)
        def execSvc = Mock(ExecutionService)
        def fkwScs = Mock(IFrameworkServices) {
            getExecutionService() >> execSvc
        }
        def mockFwk = testFramework(fkwScs)

        when:
        plugin.executeNodeStep(context, [:], node)
        then:


        1 * context.getFramework() >> mockFwk
        1 * execSvc.fileCopyFile(_, new File(copyDir.toString()), node, destPath)
    }

    @Unroll
    def "copy recursive #recursive with pattern #pattern"() {
        given:
        def plugin = new CopyFileNodeStepPlugin()
        def srcPath = copyDir.toString() + '/'
        plugin.sourcePath = srcPath
        plugin.recursive = recursive
        plugin.pattern = pattern
        def destPath = destDir.toString() + '/'
        plugin.destinationPath = destPath
        plugin.echo = false

        def context = Mock(PluginStepContext) {
            getLogger() >> Mock(PluginLogger) {
                log(_, _) >> { args ->
                    System.err.println(args[1])
                }
            }
        }
        def node = Mock(INodeEntry)
        def execSvc = Mock(ExecutionService)
        def fkwScs = Mock(IFrameworkServices) {
            getExecutionService() >> execSvc
        }
        def mockFwk = testFramework(fkwScs)
        makeDirFiles(copyDir, [
                'test1.txt',
                'file2.txt',
                'sub1/test3.xml',
                'sub1/sub2/test4.blah'
        ]
        )

        when:
        plugin.executeNodeStep(context, [:], node)
        then:

        if (expectList) {
            1 * context.getFramework() >> mockFwk
            1 * execSvc.fileCopyFiles(_, _, { arg ->
                def collect = arg*.getAbsolutePath().collect { it.substring(srcPath.length()) }
                collect.containsAll(expectList) && expectList.containsAll(collect)
            }, destPath, node
            ) >> expectList.toArray()
        }
        0 * execSvc._(*_)

        where:
        recursive | pattern     | expectList
        false     | '*'         | ['test1.txt', 'file2.txt']
        true      | '*'         | ['test1.txt', 'file2.txt', 'sub1']
        false     | '**/test*'  | ['test1.txt', 'sub1/test3.xml', 'sub1/sub2/test4.blah']
        true      | '**/test*'  | ['test1.txt', 'sub1/test3.xml', 'sub1/sub2/test4.blah']

        false     | '**'        | ['test1.txt', 'file2.txt', 'sub1/test3.xml', 'sub1/sub2/test4.blah']
        true      | '**'        |
                ['test1.txt', 'file2.txt', 'sub1', 'sub1/test3.xml', 'sub1/sub2/test4.blah', 'sub1/sub2']
        false     | '*.txt'     | ['test1.txt', 'file2.txt']
        true      | '*.txt'     | ['test1.txt', 'file2.txt']
        false     | 'sub*/*'    | ['sub1/test3.xml']
        true      | 'sub*/sub*' | ['sub1/sub2']
        false     | '**/sub2'   | []
        true      | '**/sub2'   | ['sub1/sub2']
        false     | '*/sub2'    | []
        true      | '*/sub2'    | ['sub1/sub2']
        false     | 'sub1'      | []
        true      | 'sub1'      | ['sub1']

    }

    private Framework testFramework(IFrameworkServices mock) {
        new Framework(
                Mock([constructorArgs: [testDir.toFile(), projectsDir.toFile()]], FilesystemFramework),
                Mock(IFrameworkProjectMgr),
                Mock(IPropertyLookup),
                mock,
                Mock(IFrameworkNodes)
        )
    }
}
