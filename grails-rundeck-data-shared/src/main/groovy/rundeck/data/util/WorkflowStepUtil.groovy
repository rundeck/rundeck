package rundeck.data.util

import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import rundeck.data.constants.WorkflowStepConstants

class WorkflowStepUtil {

    static String summarize(WorkflowStepData step) {
        if(step.pluginType == WorkflowStepConstants.TYPE_COMMAND) {
            return summarizeCommandStep(step)
        } else if(step.pluginType == WorkflowStepConstants.TYPE_JOB_REF) {
            return summarizeJobStep(step)
        } else {
            return "Plugin["+ step.pluginType + ', nodeStep: '+step.nodeStep+']'
        }

    }

    static String summarizeJobStep(WorkflowStepData step) {
        return "job: ${step.configuration.jobIdentifier}${step.configuration.argString?' -- '+step.configuration.argString:''}"
    }

    static String summarizeCommandStep(WorkflowStepData step){
        StringBuffer sb = new StringBuffer()
        sb << (step.configuration.scriptInterpreter ? "${step.configuration.scriptInterpreter}" : '')
        sb << (step.configuration.interpreterArgsQuoted ? "'" : '')
        sb << (step.configuration.adhocRemoteString ? "${step.configuration.adhocRemoteString}" : '')
        sb << (step.configuration.adhocLocalString ? "${step.configuration.adhocLocalString}" : '')
        sb << (step.configuration.adhocFilepath ? "${step.configuration.adhocFilepath}" : '')
        sb << (step.configuration.argString ? " -- ${step.configuration.argString}" : '')
        sb << (step.configuration.interpreterArgsQuoted ? "'" : '')
        sb << (step.description ?( " ('" + step.description + "')" ) : '')
        sb << (step.configuration.fileExtension ?( " [" + step.configuration.fileExtension + "]" ) : '')
        return sb.toString()
    }
}
