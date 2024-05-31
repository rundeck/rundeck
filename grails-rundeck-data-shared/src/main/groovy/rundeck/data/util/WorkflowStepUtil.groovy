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
        Map jobRef = step.configuration.jobref
        String jobIdentifier = jobRef.useName!="true" ? jobRef.uuid : (null==jobRef.group?'':jobRef.group+"/")+jobRef.name
        return "job: ${jobIdentifier}${jobRef.argString?' -- '+jobRef.argString:''}"
    }

    static String summarizeCommandStep(WorkflowStepData step){
        Map cfg = step.configuration
        StringBuffer sb = new StringBuffer()
        sb << (cfg.scriptInterpreter ? "${cfg.scriptInterpreter}" : '')
        sb << (cfg.interpreterArgsQuoted ? "'" : '')
        sb << (cfg.exec ? "${cfg.exec}" : '')
        sb << (cfg.script ? "${cfg.script}" : '')
        sb << (cfg.scripturl ? "${cfg.scripturl}" : '')
        sb << (cfg.scriptfile ? "${cfg.scriptfile}" : '')
        sb << (cfg.args ? " -- ${cfg.args}" : '')
        sb << (cfg.interpreterArgsQuoted ? "'" : '')
        sb << (step.description ?( " ('" + step.description + "')" ) : '')
        sb << (cfg.fileExtension ?( " [" + cfg.fileExtension + "]" ) : '')
        return sb.toString()
    }
}
