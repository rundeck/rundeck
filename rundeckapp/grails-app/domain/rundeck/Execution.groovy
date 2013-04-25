package rundeck
import com.dtolabs.rundeck.app.support.BaseNodeFilters
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
    String abortedby
    boolean cancelled
    Workflow workflow

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
        nodeRankOrderAscending(nullable: true)
        nodeRankAttribute(nullable: true)
        adhocExecution(nullable:true)
        adhocRemoteString(nullable:true, blank:true)
        adhocLocalString(nullable:true, blank:true)
        adhocFilepath(nullable:true, blank:true)
        failedNodeList(nullable:true, blank:true)
        abortedby(nullable:true, blank:true)
    }

    static mapping = {

        //mapping overrides superclass, so we need to relist these
        user column: "rduser"
        argString type: 'text'

        failedNodeList type: 'text'
        outputfilepath type: 'text'
        nodeInclude(type: 'text')
        nodeExclude(type: 'text')
        nodeIncludeName(type: 'text')
        nodeExcludeName(type: 'text')
        nodeIncludeTags(type: 'text')
        nodeExcludeTags(type: 'text')
        nodeIncludeOsName(type: 'text')
        nodeExcludeOsName(type: 'text')
        nodeIncludeOsFamily(type: 'text')
        nodeExcludeOsFamily(type: 'text')
        nodeIncludeOsArch(type: 'text')
        nodeExcludeOsArch(type: 'text')
        nodeIncludeOsVersion(type: 'text')
        nodeExcludeOsVersion(type: 'text')

        adhocLocalString type: 'text'
        adhocRemoteString type: 'text'
        adhocFilepath type: 'text'
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

    def Map toMap(){
        def map=[:]
        if(scheduledExecution){
            map.jobId=scheduledExecution.extid
        }
        map.dateStarted=dateStarted
        map.dateCompleted=dateCompleted
        map.status=status
        map.outputfilepath=outputfilepath
        map.failedNodeList = failedNodeList
        map.abortedby=abortedby
        map.cancelled=cancelled
        map.argString= argString
        map.loglevel= loglevel
        map.id= this.id
        map.doNodedispatch= this.doNodedispatch
        if(doNodedispatch){
            def yfilters = ["": "hostname"]
            map.nodefilters = [dispatch: [threadcount: nodeThreadcount, keepgoing: nodeKeepgoing, excludePrecedence: nodeExcludePrecedence]]
            if (nodeRankAttribute) {
                map.nodefilters.dispatch.rankAttribute = nodeRankAttribute
            }
            map.nodefilters.dispatch.rankOrder = (null == nodeRankOrderAscending || nodeRankOrderAscending) ? 'ascending' : 'descending'
            final Collection inclFilters = BaseNodeFilters.filterKeys.keySet().findAll {this["nodeInclude" + filterKeys[it]]}
            if (inclFilters) {
                map.nodefilters.include = [:]
                inclFilters.each { ek ->
                    map.nodefilters.include[yfilters[ek] ?: ek] = this["nodeInclude${filterKeys[ek]}"]
                }
            }
            final Collection exclFilters = BaseNodeFilters.filterKeys.keySet().findAll {this["nodeExclude" + filterKeys[it]]}
            if (exclFilters) {
                map.nodefilters.exclude = [:]
                exclFilters.each { ek ->
                    map.nodefilters.exclude[yfilters[ek] ?: ek] = this["nodeExclude${filterKeys[ek]}"]
                }
            }
        }
        map.project= this.project
        map.user= this.user
        map.workflow=this.workflow.toMap()
        map
    }
    static Execution fromMap(Map data, ScheduledExecution job=null){
        Execution exec= new Execution()
        if(job){
            exec.scheduledExecution=job
        }
        exec.dateStarted=data.dateStarted
        exec.dateCompleted=data.dateCompleted
        exec.status=data.status
        exec.outputfilepath = data.outputfilepath
        exec.failedNodeList = data.failedNodeList
        exec.abortedby = data.abortedby
        exec.cancelled = data.cancelled
        exec.argString = data.argString
        exec.loglevel = data.loglevel
        exec.doNodedispatch = data.doNodedispatch
        if (data.nodefilters) {
            exec.nodeThreadcount = data.nodefilters.dispatch.threadcount
            if (data.nodefilters.dispatch.containsKey('keepgoing')) {
                exec.nodeKeepgoing = data.nodefilters.dispatch.keepgoing
            }
            if (data.nodefilters.dispatch.containsKey('excludePrecedence')) {
                exec.nodeExcludePrecedence = data.nodefilters.dispatch.excludePrecedence
            }
            if (data.nodefilters.dispatch.containsKey('rankAttribute')) {
                exec.nodeRankAttribute = data.nodefilters.dispatch.rankAttribute
            }
            if (data.nodefilters.dispatch.containsKey('rankOrder')) {
                exec.nodeRankOrderAscending = data.nodefilters.dispatch.rankOrder == 'ascending'
            }
            if (data.nodefilters.include) {
                exec.doNodedispatch = true
                data.nodefilters.include.keySet().each { inf ->
                    if (null != filterKeys[inf]) {
                        exec["nodeInclude${filterKeys[inf]}"] = data.nodefilters.include[inf]
                    }
                }

            }
            if (data.nodefilters.exclude) {
                exec.doNodedispatch = true
                data.nodefilters.exclude.keySet().each { inf ->
                    if (null != filterKeys[inf]) {
                        exec["nodeExclude${filterKeys[inf]}"] = data.nodefilters.exclude[inf]
                    }
                }
            }
        }
        exec.project = data.project
        exec.user = data.user
        exec.workflow = Workflow.fromMap(data.workflow)
        exec
    }
}

