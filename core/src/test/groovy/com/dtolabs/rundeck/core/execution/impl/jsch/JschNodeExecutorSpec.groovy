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

    def teardown() {
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
