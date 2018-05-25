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

package com.dtolabs.rundeck.core.execution.impl.jsch

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

/**
 * Created by greg on 4/12/16.
 */
class JschNodeExecutorSpec extends Specification {
    public static final String PROJECT_NAME = 'JschNodeExecutorSpec'
    Framework framework
    IRundeckProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def cleanup() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    def "require hostname"() {
        given:
        def exec = new JschNodeExecutor(framework)
        def context = Mock(ExecutionContext) {
            getFrameworkProject() >> PROJECT_NAME
        }
        def command = ['echo', 'hi'].toArray(new String[2])
        def node = new NodeEntryImpl('anode')
        node.setHostname(hostval)

        when:
        def result = exec.executeCommand(context, command, node)


        then:
        !result.success
        result.failureReason.toString() == 'ConfigurationFailure'
        result.failureMessage == "Hostname must be set to connect to remote node 'anode'"

        where:
        hostval | _
        null    | _
        ""      | _
    }
}
