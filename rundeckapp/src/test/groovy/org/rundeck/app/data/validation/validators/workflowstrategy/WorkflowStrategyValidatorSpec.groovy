package org.rundeck.app.data.validation.validators.workflowstrategy

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import org.rundeck.app.data.job.RdJob
import org.rundeck.app.data.job.RdWorkflow
import org.rundeck.app.data.job.RdWorkflowStep
import rundeck.services.FrameworkService
import rundeck.services.PluginService
import spock.lang.Specification

class WorkflowStrategyValidatorSpec extends Specification {
    def "invalid when missing plugin type"() {
        given:
        def fwkSvc = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(IFramework) {
                getWorkflowStrategyService() >> Mock(WorkflowStrategyService)
            }
            getPluginService() >> Mock(PluginService) {
                validatePlugin(_,_,_,_) >> null
            }
        }
        def validator = new WorkflowStrategyValidator(fwkSvc)

        when:
        RdJob job = new RdJob(workflow: new RdWorkflow(strategy: "missing-plugin"))
        validator.validate(job, job.errors)

        then:
        job.errors.fieldErrors[0].field == "workflow.strategy"
        job.errors.fieldErrors[0].code == "workflow.strategy.missing.plugin"

    }

    def "invalidate when workflow strategy is invalid"() {
        given:
        def fwkSvc = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(IFramework) {
                getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    getStrategyForWorkflow(_,_) >> Mock(WorkflowStrategy) {
                        validate(_) >> wfStrategyReport
                    }
                }
            }
            getPluginService() >> Mock(PluginService) {
                validatePlugin(_,_,_,_) >> new ValidatedPlugin(report: pluginReport)
            }
        }
        def validator = new WorkflowStrategyValidator(fwkSvc)

        when:
        RdJob job = new RdJob(workflow: new RdWorkflow(steps: [new RdWorkflowStep(pluginType: "builtin-command", configuration: [exec:"echo hello"])],strategy: "missing-plugin"))
        validator.validate(job, job.errors)

        then:
        job.errors.fieldErrors[0].field == "workflow.strategy"
        job.errors.fieldErrors[0].code == "workflow.strategy.invalid.configuration"

        where:
        pluginReport                                    | wfStrategyReport
        new Validator.Report(errors:[fld1:"Bad"])       | null
        null                                            | new Validator.Report(errors:[fld1:"Bad"])

    }
}
