package rundeck.data.validation.validators.workflowstep

import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.StepPlugin
import grails.util.Holders
import org.rundeck.app.core.FrameworkServiceCapabilities
import rundeck.data.constants.WorkflowStepConstants
import rundeck.data.job.RdWorkflowStep
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import rundeck.data.validation.validators.plugin.RundeckPluginValidator
import org.springframework.validation.Validator
import org.springframework.web.context.request.RequestContextHolder

class WorkflowStepValidatorFactory {

    static final COMMAND_TYPES = [WorkflowStepConstants.TYPE_COMMAND,
                                  WorkflowStepConstants.TYPE_SCRIPT,
                                  WorkflowStepConstants.TYPE_SCRIPT_FILE,
                                  WorkflowStepConstants.TYPE_SCRIPT_URL,
    ]
    private FrameworkServiceCapabilities frameworkService

    WorkflowStepValidatorFactory(FrameworkServiceCapabilities frameworkService) {

        this.frameworkService = frameworkService
    }

    Validator createValidator(String field, RdWorkflowStep wfstep) {
        if(COMMAND_TYPES.contains(wfstep.pluginType)) {
            return new CommandExecWorkflowStepValidator()
        } else if(WorkflowStepConstants.TYPE_JOB_REF == wfstep.pluginType) {
            List<String> projectNames = frameworkService.projectNames(frameworkService.userAuthContext(getSession()))
            return new JobExecWorkflowStepValidator(Holders.grailsApplication.mainContext.getBean(JobDataProvider),
            projectNames,
            true)
        }
        Class pluginService = wfstep.nodeStep ? NodeStepPlugin : StepPlugin
        return new RundeckPluginValidator(frameworkService,
                pluginService,
                "${field}.pluginType",
                "${field}.configuration"
        )
    }

    def getSession() {
        RequestContextHolder.currentRequestAttributes().getSession()
    }
}
