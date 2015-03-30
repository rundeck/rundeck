package com.dtolabs.rundeck.core.execution.impl.jsch

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.utils.BasicSource
import com.dtolabs.rundeck.core.execution.utils.PasswordSource
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

/**
 * Created by greg on 3/20/15.
 */
class SudoResponderSpec extends Specification {
    def setup() {
    }

    def "sudo not enabled"(String password, byte[] expected, boolean enabled) {
        setup:
        def node = new NodeEntryImpl("test")
        def fwk = AbstractBaseTest.createTestFramework()
        fwk.getFrameworkProjectMgr().createFrameworkProject("SudoResponderTest")
        def pwdsource = Mock(PasswordSource) {
            getPassword() >> password.bytes
        }
        def resp = SudoResponder.create(node, fwk, Mock(ExecutionContext) {
            getFrameworkProject() >> 'SudoResponderTest'
        }, "sudo-", pwdsource, null
        )

        expect:
        resp.inputBytes == expected
        resp.sudoEnabled == enabled

        where:
        password | expected | enabled
        'test'   | null     | false
    }

    def "input bytes with sudo enabled"(String password, byte[] expected, boolean enabled) {
        setup:
        def node = new NodeEntryImpl("test")
        node.getAttributes().put("sudo-command-enabled", "true")
        def fwk = AbstractBaseTest.createTestFramework()
        fwk.getFrameworkProjectMgr().createFrameworkProject("SudoResponderTest")
        def pwdsource = new BasicSource(password.bytes)
        def resp = SudoResponder.create(node, fwk, Mock(ExecutionContext) {
            getFrameworkProject() >> 'SudoResponderTest'
        }, "sudo-", pwdsource, null
        )

        expect:
        resp.inputBytes == expected
        resp.inputBytes == (password + '\n').bytes
        resp.sudoEnabled == enabled

        where:
        password | expected                | enabled
        'test'   | ('test\n').bytes | true
    }
}
