package com.dtolabs.rundeck.plugin.script

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.service.FileCopierException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import spock.lang.Specification

/**
 * Tests for {@link ScriptFileCopier}, covering configuration of the command via the
 * node "script-copy" attribute when no project/framework default command is set (issue #9910).
 */
class ScriptFileCopierSpec extends Specification {

    def "description validates with no command configured"() {
        when:
            def report = Validator.validate(new Properties(), ScriptFileCopier.DESC)
            def resolverReport = Validator.validate(
                PropertyResolverFactory.createInstanceResolver(new Properties()),
                ScriptFileCopier.DESC,
                PropertyScope.Instance
            )
        then:
            report.valid
            resolverReport.valid
    }

    def "fails with a configuration error when neither project default nor node attribute is set"() {
        given:
            def framework = Mock(Framework) {
                getProjectProperty('proj', _) >> null
            }
            def context = Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> 'proj'
            }
            def node = new NodeEntryImpl('node1')
            def copier = new ScriptFileCopier()
        when:
            copier.copyScriptContent(context, 'echo hi', node, '/tmp/dest')
        then:
            def e = thrown(FileCopierException)
            e.failureReason == StepFailureReason.ConfigurationFailure
            e.message.contains(ScriptFileCopier.SCRIPT_ATTRIBUTE)
    }
}
