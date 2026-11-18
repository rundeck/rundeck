package rundeck.data.util

import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand
import rundeck.data.constants.WorkflowStepConstants

class WorkflowStepUtil {

    /**
     * Step types summarized as a command or script: the legacy builtin command type, and the
     * command, inline script and script file/URL plugin types that steps deserialized from
     * workflow JSON carry.
     */
    static final List<String> COMMAND_STEP_TYPES = Collections.unmodifiableList(
        [
            WorkflowStepConstants.TYPE_COMMAND,
            ExecCommand.EXEC_COMMAND_TYPE,
            ScriptCommand.SCRIPT_COMMAND_TYPE,
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
        ]
    )

    /**
     * Step types that run a single command string
     */
    static final List<String> EXEC_STEP_TYPES = Collections.unmodifiableList(
        [WorkflowStepConstants.TYPE_COMMAND, ExecCommand.EXEC_COMMAND_TYPE]
    )

    /**
     * Summarize a workflow step
     * @param step workflow step
     * @return summary text: the command or script of a command step, the job of a job reference step,
     * otherwise the plugin type
     */
    static String summarize(WorkflowStepData step) {
        if(step.pluginType in COMMAND_STEP_TYPES) {
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

    /**
     * Summarize a command or script step. Reads the job definition keys (exec, script, scriptfile,
     * scripturl, args) and the equivalent keys used by the command plugin types (adhocRemoteString,
     * adhocLocalString, adhocFilepath, argString).
     * @param step command or script step
     * @return summary text
     */
    static String summarizeCommandStep(WorkflowStepData step){
        Map cfg = step.configuration ?: [:]
        def quoted = cfg.interpreterArgsQuoted?.toString() == 'true'
        def args = cfg.args ?: cfg.argString
        StringBuffer sb = new StringBuffer()
        sb << (cfg.scriptInterpreter ? "${cfg.scriptInterpreter}" : '')
        sb << (quoted ? "'" : '')
        sb << (cfg.exec ? "${cfg.exec}" : '')
        sb << (cfg.adhocRemoteString ? "${cfg.adhocRemoteString}" : '')
        sb << (cfg.script ? "${cfg.script}" : '')
        sb << (cfg.adhocLocalString ? "${cfg.adhocLocalString}" : '')
        sb << (cfg.scripturl ? "${cfg.scripturl}" : '')
        sb << (cfg.scriptfile ? "${cfg.scriptfile}" : '')
        sb << (cfg.adhocFilepath ? "${cfg.adhocFilepath}" : '')
        sb << (args ? " -- ${args}" : '')
        sb << (quoted ? "'" : '')
        sb << (step.description ?( " ('" + step.description + "')" ) : '')
        sb << (cfg.fileExtension ?( " [" + cfg.fileExtension + "]" ) : '')
        return sb.toString()
    }

    /**
     * Get the command string of a step that runs a single command
     * @param step workflow step, may be null
     * @return the command string, or null if the step is not a command step
     */
    static String getCommandString(WorkflowStepData step) {
        if (!(step?.pluginType in EXEC_STEP_TYPES)) {
            return null
        }
        Map cfg = step.configuration ?: [:]
        return (cfg.exec ?: cfg.adhocRemoteString)?.toString()
    }
}
