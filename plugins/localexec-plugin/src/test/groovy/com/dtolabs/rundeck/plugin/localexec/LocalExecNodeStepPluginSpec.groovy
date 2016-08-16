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

package com.dtolabs.rundeck.plugin.localexec

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.FilesystemFramework
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFrameworkNodes
import com.dtolabs.rundeck.core.common.IFrameworkProjectMgr
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

/**
 * Created by greg on 6/24/16.
 */
class LocalExecNodeStepPluginSpec extends Specification {
    File testDir
    File projectsDir

    def setup() {
        testDir = File.createTempFile("LocalExecNodeStepPluginSpec", "-test")
        projectsDir = File.createTempFile("LocalExecNodeStepPluginSpec", "-test")

    }

    def cleanup() {
        testDir.delete()
        projectsDir.delete()
    }

    def "no run authorization for local node throws exception"() {
        given:
        def plugin = new LocalExecNodeStepPlugin()
        plugin.setCommand("a command")

        def context = Mock(PluginStepContext)
        def config = [:]
        def node = new NodeEntryImpl('anode')
        def fwknode = new NodeEntryImpl('fwknode')

        def fwkNodes = Mock(IFrameworkNodes)
        def fwk = testFramework(fwkNodes)
        println testDir

        when:
        plugin.executeNodeStep(context, config, node)

        then:
        NodeStepException e = thrown()
        e.nodeName == 'anode'
        e.message ==~ /^Not authorized for "run" on local node .+/
        3 * context.getFramework() >> fwk
        1 * context.getFrameworkProject() >> 'aproject'
        1 * context.getExecutionContext() >> Mock(ExecutionContext) {
            1 * getAuthContext() >> Mock(AuthContext)
        }
        1 * fwkNodes.createFrameworkNode() >> fwknode
        1 * fwkNodes.getFrameworkNodeName() >> 'fwknode'
        1 * fwkNodes.filterAuthorizedNodes('aproject', { it.contains('run') }, _, _) >> new NodeSetImpl()

    }

    def "run authorization for local will execute"() {
        given:
        def plugin = new LocalExecNodeStepPlugin()
        plugin.setCommand("a command")

        def runner = Mock(LocalExecNodeStepPlugin.LocalCommandRunner)
        plugin.setRunner(runner)


        def context = Mock(PluginStepContext) {
            getLogger() >> Mock(PluginLogger)
        }
        def config = [:]
        def node = new NodeEntryImpl('anode')
        def fwknode = new NodeEntryImpl('fwknode')
        def filtered = new NodeSetImpl([fwknode: fwknode])

        def fwkNodes = Mock(IFrameworkNodes)
        def fwk = testFramework(fwkNodes)
        println testDir

        when:
        plugin.executeNodeStep(context, config, node)

        then:
        2 * context.getFramework() >> fwk
        1 * context.getFrameworkProject() >> 'aproject'
        1 * context.getExecutionContext() >> Mock(ExecutionContext) {
            1 * getAuthContext() >> Mock(AuthContext)
        }
        1 * fwkNodes.createFrameworkNode() >> fwknode
        1 * fwkNodes.filterAuthorizedNodes('aproject', { it.contains('run') }, _, _) >> filtered
        1 * runner.runLocalCommand(_, _, _, _, _) >> 0
    }

    private Framework testFramework(IFrameworkNodes fwkNodes) {
        new Framework(
                Mock([constructorArgs: [testDir, projectsDir]], FilesystemFramework),
                Mock(IFrameworkProjectMgr),
                Mock(IPropertyLookup),
                Mock(IFrameworkServices),
                fwkNodes
        )
    }
}
