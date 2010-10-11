
/**
* Execution
*/
class Execution extends ExecutionContext {

    ScheduledExecution scheduledExecution
    Date dateStarted
    Date dateCompleted 
    String status
    String outputfilepath
    String failedNodeList
    boolean cancelled

    static belongsTo = ScheduledExecution
    
    static constraints = {
        workflow(nullable:true)
        argString(nullable:true)
        dateStarted(nullable:true)
        dateCompleted(nullable:true)
        status(nullable:true)
        outputfilepath(nullable:true)
        scheduledExecution(nullable:true)
        loglevel(nullable:true)
        nodeInclude(nullable:true)
        nodeExclude(nullable:true)
        nodeIncludeName(nullable:true)
        nodeExcludeName(nullable:true)
        nodeIncludeType(nullable:true)
        nodeExcludeType(nullable:true)
        nodeIncludeTags(nullable:true)
        nodeExcludeTags(nullable:true)
        nodeIncludeOsName(nullable:true)
        nodeExcludeOsName(nullable:true)
        nodeIncludeOsFamily(nullable:true)
        nodeExcludeOsFamily(nullable:true)
        nodeIncludeOsArch(nullable:true)
        nodeExcludeOsArch(nullable:true)
        nodeIncludeOsVersion(nullable:true)
        nodeExcludeOsVersion(nullable:true)
        nodeExcludePrecedence(nullable:true)
        nodeKeepgoing(nullable:true)
        doNodedispatch(nullable:true)
        nodeThreadcount(nullable:true)
        adhocExecution(nullable:true)
        adhocRemoteString(nullable:true, blank:true)
        adhocLocalString(nullable:true, blank:true)
        adhocFilepath(nullable:true, blank:true)
        failedNodeList(nullable:true, blank:true)
    }



    public String toString() {
        return "Workflow execution: ${workflow}"
    }

 
    // various utility methods helpful to the presentation layer
    def String durationAsString() {
        if (!dateStarted || !dateCompleted) return ""
        def dms = dateCompleted.getTime() - dateStarted.getTime()
        def duration
        if (dms < 1000) {
            duration = "0s"
        } else if (dms >= 1000 && dms < 60000)  {
            duration = String.valueOf( dms.intdiv( 1000)) + "s"
        } else {
            duration = String.valueOf( dms.intdiv( 60000) ) + "m"
        }
        return String.valueOf(duration)        
    }

    def String outputfilepathSizeAsString() {
        if (outputfilepath && new File(outputfilepath).exists()) {
            return String.valueOf(new File(outputfilepath).size()) + " bytes"
        } else {
            return ""
        }
    }

    def String generateLoggingNamespace() {
        return "com.dtolabs.rundeck.core."+this.command
    }
}

