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
        return "job: ${step.getJobIdentifier()}${step.argString?' -- '+step.argString:''}"
    }

    static String summarizeCommandStep(WorkflowStepData step){
        StringBuffer sb = new StringBuffer()
        sb << (step.scriptInterpreter ? "${step.scriptInterpreter}" : '')
        sb << (step.interpreterArgsQuoted ? "'" : '')
        sb << (step.adhocRemoteString ? "${step.adhocRemoteString}" : '')
        sb << (step.adhocLocalString ? "${step.adhocLocalString}" : '')
        sb << (step.adhocFilepath ? "${step.adhocFilepath}" : '')
        sb << (step.argString ? " -- ${step.argString}" : '')
        sb << (step.interpreterArgsQuoted ? "'" : '')
        sb << (step.description ?( " ('" + step.description + "')" ) : '')
        sb << (step.fileExtension ?( " [" + step.fileExtension + "]" ) : '')
        return sb.toString()
    }
}
