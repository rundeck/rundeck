package rundeck.data.validation.validators.workflowstrategy

import com.dtolabs.rundeck.core.common.FrameworkServiceCapabilities
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItemFactory
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.plugins.PluginServiceCapabilities
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import rundeck.data.job.RdJob
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import spock.lang.Specification

class WorkflowStrategyValidatorSpec extends Specification {
    def "invalid when missing plugin type"() {
        given:
        def fwkSvc = Mock(FrameworkServiceCapabilities) {
            getRundeckFramework() >> Mock(IFramework) {
                getWorkflowStrategyService() >> Mock(WorkflowStrategyService)
            }
            existsFrameworkProject(_) >> true
            getFrameworkPropertyResolverWithProps(_,_) >> Mock(PropertyResolver)
            getPluginService() >> Mock(PluginServiceCapabilities) {
                getPluginRegistry() >> Mock(PluginRegistry) {
                    validatePluginByName(_,_,_,_) >> null
                }
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
        def fwkSvc = Mock(FrameworkServiceCapabilities) {
            getRundeckFramework() >> Mock(IFramework) {
                getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    getStrategyForWorkflow(_,_) >> Mock(WorkflowStrategy) {
                        validate(_) >> wfStrategyReport
                    }
                }
            }
            existsFrameworkProject(_) >> true
            getWorkflowExecutionItemFactory() >> Mock(WorkflowExecutionItemFactory) {
                createExecutionItemForWorkflow(_) >> Mock(WorkflowExecutionItem)
            }
            getFrameworkPropertyResolverWithProps(_,_) >> Mock(PropertyResolver)
            getPluginService() >> Mock(PluginServiceCapabilities) {
                getPluginRegistry() >> Mock(PluginRegistry) {
                    validatePluginByName(_,_,_,_) >> new ValidatedPlugin(report: pluginReport)
                }
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
