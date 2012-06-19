import org.codehaus.groovy.grails.commons.ConfigurationHolder
/**
 * ExecutionContext
 */
abstract class ExecutionContext extends BaseNodeFilters{
    String project
    String argString
    String user
    String loglevel="WARN"

    static mapping = {
        def config = ConfigurationHolder.config
        if (config.rundeck.v14.rdbsupport == 'true') {
            user column: "rduser"
            adhocLocalString type:'text'
            adhocRemoteString type:'text'
            adhocFilepath type:'text'
            argString type:'text'
        }
    }
    Boolean nodeKeepgoing=false
    Boolean doNodedispatch=false
    Integer nodeThreadcount=1
    String adhocRemoteString
    String adhocLocalString
    String adhocFilepath
    Boolean adhocExecution=false
    String nodeRankAttribute
    Boolean nodeRankOrderAscending=true
}

