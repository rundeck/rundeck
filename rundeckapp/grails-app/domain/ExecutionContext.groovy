import org.codehaus.groovy.grails.commons.ConfigurationHolder
/**
 * ExecutionContext
 */
abstract class ExecutionContext extends BaseNodeFilters{
    String project
    String argString
    String user
    Workflow workflow
    String loglevel="WARN"

    static mapping = {
        def config = ConfigurationHolder.config
        if (config.rundeck.v14.rdbsupport == 'true') {
            user column: "rduser"
        }
    }
    Boolean nodeKeepgoing=false
    Boolean doNodedispatch=false
    Integer nodeThreadcount=1
    String adhocRemoteString
    String adhocLocalString
    String adhocFilepath
    Boolean adhocExecution=false
}

