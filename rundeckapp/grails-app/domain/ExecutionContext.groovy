
/**
 * ExecutionContext
 */
abstract class ExecutionContext extends BaseNodeFilters{
    String project
    String argString
    String user
    Workflow workflow
    String loglevel="WARN"

    Boolean nodeKeepgoing=false
    Boolean doNodedispatch=false
    Integer nodeThreadcount=1
    String adhocRemoteString
    String adhocLocalString
    String adhocFilepath
    Boolean adhocExecution=false
}

