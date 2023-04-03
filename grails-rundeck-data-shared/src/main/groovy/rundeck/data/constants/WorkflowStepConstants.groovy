package rundeck.data.constants

class WorkflowStepConstants {
    public static final String TYPE_COMMAND = "builtin-command"
    public static final String TYPE_SCRIPT = "builtin-script"
    public static final String TYPE_SCRIPT_FILE = "builtin-scriptfile"
    public static final String TYPE_SCRIPT_URL = "builtin-scripturl"
    public static final String TYPE_JOB_REF = "builtin-jobref"
    public static final List<String> LIST_COMMAND_TYPES = [TYPE_COMMAND,TYPE_SCRIPT,TYPE_SCRIPT_FILE,TYPE_SCRIPT_URL]
    public static final String ERR_CODE_BLANK_COMMAND = 'commandExec.adhocExecution.adhocRemoteString.blank.message'
    public static final String ERR_CODE_BLANK_SCRIPT = 'commandExec.adhocExecution.adhocLocalString.blank.message'
    public static final String ERR_CODE_BLANK_FILE = 'commandExec.adhocExecution.adhocFilepath.blank.message'
    public static final String ERR_CODE_BLANK_URL = 'commandExec.adhocExecution.adhocString.blank.message'
    public static final String ERR_CODE_DUPLICATE_PARAM = 'scheduledExecution.adhocString.duplicate.message'
    public static final String ERR_CODE_STRICT_JOB_EXISTS = 'commandExec.jobName.strict.validation.message'
    public static final String ERR_CODE_JOB_NAME_BLANK = 'commandExec.jobName.blank.message'
    public static final String ERR_CODE_JOB_PROJECT_BLANK = 'commandExec.jobProject.blank.message'
    public static final String ERR_CODE_REF_JOB_UNAUTH = 'commandExec.jobProject.unauth.message'
}
