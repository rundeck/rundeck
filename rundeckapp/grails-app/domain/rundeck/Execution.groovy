package rundeck
import com.dtolabs.rundeck.app.support.BaseNodeFilters
import com.dtolabs.rundeck.app.support.ExecutionContext
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.util.XmlParserUtil

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
    String succeededNodeList
    String abortedby
    boolean cancelled
    Boolean timedOut=false
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
        failedNodeList(nullable:true, blank:true)
        succeededNodeList(nullable:true, blank:true)
        abortedby(nullable:true, blank:true)
        serverNodeUUID(size:36..36, blank: true, nullable: true, validator: { val, obj ->
            if (null == val) return true;
            try { return null!= UUID.fromString(val) } catch (IllegalArgumentException e) {
                return false
            }
        })
        timeout(maxSize: 256, blank: true, nullable: true,)
        timedOut(nullable: true)
    }

    static mapping = {

        //mapping overrides superclass, so we need to relist these
        user column: "rduser"
        argString type: 'text'

        failedNodeList type: 'text'
        succeededNodeList type: 'text'
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
        filter(type: 'text')

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
        map.succeededNodeList = succeededNodeList
        map.abortedby=abortedby
        map.cancelled=cancelled
        if(timedOut){
            map.timedOut=timedOut
        }
        map.argString= argString
        map.loglevel= loglevel
        if(timeout) {
            map.timeout = timeout
        }
        map.id= this.id
        map.doNodedispatch= this.doNodedispatch
        if(doNodedispatch){
            map.nodefilters = [dispatch: [threadcount: nodeThreadcount?:1, keepgoing: nodeKeepgoing, excludePrecedence: nodeExcludePrecedence]]
            if (nodeRankAttribute) {
                map.nodefilters.dispatch.rankAttribute = nodeRankAttribute
            }
            map.nodefilters.dispatch.rankOrder = (null == nodeRankOrderAscending || nodeRankOrderAscending) ? 'ascending' : 'descending'
            if (filter) {
                map.nodefilters.filter = filter
            } else {
                map.nodefilters.filter = asFilter()
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
        exec.succeededNodeList = data.succeededNodeList
        exec.abortedby = data.abortedby
        exec.cancelled = XmlParserUtil.stringToBool(data.cancelled,false)
        exec.timedOut = XmlParserUtil.stringToBool(data.timedOut,false)
        exec.argString = data.argString
        exec.loglevel = data.loglevel
        exec.doNodedispatch = data.doNodedispatch
        exec.timeout = data.timeout
        if (data.nodefilters) {
            exec.nodeThreadcount = XmlParserUtil.stringToInt(data.nodefilters.dispatch?.threadcount,1)
            if (data.nodefilters.dispatch?.containsKey('keepgoing')) {
                exec.nodeKeepgoing = XmlParserUtil.stringToBool(data.nodefilters.dispatch.keepgoing, false)
            }
            if (data.nodefilters.dispatch?.containsKey('excludePrecedence')) {
                exec.nodeExcludePrecedence = XmlParserUtil.stringToBool(data.nodefilters.dispatch.excludePrecedence, true)
            }
            if (data.nodefilters.dispatch?.containsKey('rankAttribute')) {
                exec.nodeRankAttribute = data.nodefilters.dispatch.rankAttribute
            }
            if (data.nodefilters.dispatch?.containsKey('rankOrder')) {
                exec.nodeRankOrderAscending = data.nodefilters.dispatch.rankOrder == 'ascending'
            }
            if (data.nodefilters.filter) {
                exec.doNodedispatch = true
                exec.filter = data.nodefilters.filter
            } else {
                def map=[include:[:],exclude:[:]]
                if (data.nodefilters.include) {
                    exec.doNodedispatch = true
                    data.nodefilters.include.keySet().each { inf ->
                        if (null != filterKeys[inf]) {
                            map.include[inf]= data.nodefilters.include[inf]
                        }
                    }

                }
                if (data.nodefilters.exclude) {
                    exec.doNodedispatch = true
                    data.nodefilters.exclude.keySet().each { inf ->
                        if (null != filterKeys[inf]) {
                            map.exclude[inf] = data.nodefilters.exclude[inf]
                        }
                    }
                }
                exec.filter=asFilter(map)
            }
        }
        exec.project = data.project
        exec.user = data.user
        exec.workflow = Workflow.fromMap(data.workflow)
        exec
    }
}

