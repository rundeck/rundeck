/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import com.dtolabs.rundeck.app.support.ExecutionContext
import com.dtolabs.rundeck.core.common.FrameworkResource
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.util.XmlParserUtil
import rundeck.services.ExecutionService
import rundeck.services.JobReferenceImpl
import rundeck.services.execution.ExecutionReferenceImpl

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
    String executionType
    Integer retryAttempt=0
    Boolean willRetry=false
    Execution retryExecution
    Orchestrator orchestrator;
    String userRoleList
    String serverNodeUUID
    Integer nodeThreadcount=1
    Long retryOriginalId

    static hasOne = [logFileStorageRequest: LogFileStorageRequest]
    static transients = ['executionState', 'customStatusString', 'userRoles']
    static constraints = {
        project(matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX, validator:{val,Execution obj->
            if(obj.scheduledExecution && obj.scheduledExecution.project!=val){
                return 'job.project.mismatch.error'
            }
        })
        logFileStorageRequest(nullable:true)
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
        orchestrator(nullable: true);
        failedNodeList(nullable:true, blank:true)
        succeededNodeList(nullable:true, blank:true)
        abortedby(nullable:true, blank:true)
        serverNodeUUID(maxSize: 36, size:36..36, blank: true, nullable: true, validator: { val, obj ->
            if (null == val) return true;
            try { return null!= UUID.fromString(val) } catch (IllegalArgumentException e) {
                return false
            }
        })
        timeout(maxSize: 256, blank: true, nullable: true,)
        retry(maxSize: 256, blank: true, nullable: true,matches: /^\d+$/)
        timedOut(nullable: true)
        executionType(nullable: true, maxSize: 30)
        retryAttempt(nullable: true)
        retryExecution(nullable: true)
        willRetry(nullable: true)
        nodeFilterEditable(nullable: true)
        userRoleList(nullable: true)
        retryDelay(nullable:true)
        successOnEmptyNodeFilter(nullable: true)
        retryOriginalId(nullable: true)
        excludeFilterUncheck(nullable: true)
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
        timeout( type: 'text')
        retry( type: 'text')
        userRoleList(type: 'text')
        serverNodeUUID(type: 'string')

        DomainIndexHelper.generate(delegate) {
            index 'EXEC_IDX_1', ['id', 'project', 'dateCompleted']
            index 'EXEC_IDX_2', ['dateStarted', 'status']
            index 'EXEC_IDX_3', ['project', 'dateCompleted']
            index 'EXEC_IDX_4', ['dateCompleted', 'scheduledExecution']
            index 'EXEC_IDX_5', ['scheduledExecution', 'status']
        }
    }

    static namedQueries = {
        isScheduledAdHoc {
            eq 'status', ExecutionService.EXECUTION_SCHEDULED
        }
        withServerNodeUUID { uuid ->
            eq 'serverNodeUUID', uuid
        }
        withProject{ project ->
            eq 'project', project
        }
        lastExecutionByUser{ user ->
            eq 'user', user
            maxResults 1
            order 'dateStarted', 'desc'
        }
	}


    public String toString() {
        return "Workflow execution: ${workflow}"
    }

    public setUserRoles(List l) {
        setUserRoleList(l?.join(","))
    }

    public List getUserRoles() {
        if (userRoleList) {
            return Arrays.asList(userRoleList.split(/,/))
        } else {
            return []
        }
    }
    public boolean statusSucceeded(){
        return getExecutionState()==ExecutionService.EXECUTION_SUCCEEDED
    }

    public String getExecutionState() {
        return cancelled ? ExecutionService.EXECUTION_ABORTED :
                null != dateStarted && dateStarted.getTime() > System.currentTimeMillis() ? ExecutionService.EXECUTION_SCHEDULED :
                    null == dateCompleted ? ExecutionService.EXECUTION_RUNNING :
                        (status in ['true', 'succeeded']) ? ExecutionService.EXECUTION_SUCCEEDED :
                                cancelled ? ExecutionService.EXECUTION_ABORTED :
                                        willRetry ? ExecutionService.EXECUTION_FAILED_WITH_RETRY :
                                                timedOut ? ExecutionService.EXECUTION_TIMEDOUT :
                                                        (status in ['false', 'failed']) ? ExecutionService.EXECUTION_FAILED :
                                                                isCustomStatusString(status)? ExecutionService.EXECUTION_STATE_OTHER : status.toLowerCase()
    }

    public boolean hasExecutionEnabled() {
        return !scheduledExecution || scheduledExecution.hasExecutionEnabled();
    }

    public String getCustomStatusString(){
        executionState==ExecutionService.EXECUTION_STATE_OTHER?status:null
    }

    public static boolean isCustomStatusString(String value){
        null!=value && !(value.toLowerCase() in [ExecutionService.EXECUTION_TIMEDOUT,
                                                 ExecutionService.EXECUTION_FAILED_WITH_RETRY,
                                                 ExecutionService.EXECUTION_ABORTED,
                                                 ExecutionService.EXECUTION_SUCCEEDED,
                                                 ExecutionService.EXECUTION_FAILED,
                                                 ExecutionService.EXECUTION_SCHEDULED])
    }

    // various utility methods helpful to the presentation layer

    /**
     * Returns the duration of the execution in amount of milliseconds.
     * @return
     */
    def Long durationAsLong() {
        if (!dateStarted || !dateCompleted) return null
        return (dateCompleted.getTime() - dateStarted.getTime())
    }

    def String durationAsString() {

        def dms = durationAsLong()
        if(!dms) return ""

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
        if(this.executionType) {
            map.executionType=executionType
        }
        if(this.retryAttempt){
            map.retryAttempt=retryAttempt
        }
        if(this.retryOriginalId){
            map.retryOriginalId=retryOriginalId
        }
        if(this.retry){
            map.retry=this.retry
        }
        if(this.retryDelay){
            map.retryDelay=this.retryDelay
        }
        if(this.retryExecution){
            map.retryExecutionId=retryExecution.id
        }
        if(this.willRetry){
            map.willRetry=true
        }
        if(doNodedispatch){
            map.nodefilters = [dispatch: [threadcount: nodeThreadcount?:1, keepgoing: nodeKeepgoing, excludePrecedence: nodeExcludePrecedence]]
            if (nodeRankAttribute) {
                map.nodefilters.dispatch.rankAttribute = nodeRankAttribute
            }
            if(this.filterExclude && this.excludeFilterUncheck){
                map.nodefilters.dispatch.excludeFilterUncheck = this.excludeFilterUncheck
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
		if(this.orchestrator){
			map.orchestrator=this.orchestrator.toMap();
		}
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
        exec.doNodedispatch = XmlParserUtil.stringToBool(data.doNodedispatch,false)
        exec.timeout = data.timeout
        if(data.executionType) {
            exec.executionType = data.executionType
        }
        if(data.retryAttempt){
            exec.retryAttempt= XmlParserUtil.stringToInt(data.retryAttempt, 0)
        }
        if(data.retryOriginalId){
            exec.retryOriginalId= Long.valueOf(data.retryOriginalId)
        }
        if(data.retry){
            exec.retry=data.retry
        }
        if(data.retryDelay){
            exec.retryDelay=data.retryDelay
        }
        if(data.retryExecutionId){
            exec.retryExecution=Execution.get(data.retryExecutionId)
        }
        if(data.willRetry){
            exec.willRetry=XmlParserUtil.stringToBool(data.willRetry,false)
        }
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
            exec.excludeFilterUncheck = data.nodefilters.excludeFilterUncheck ? data.nodefilters.excludeFilterUncheck : false
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
        if(data.orchestrator){
            exec.orchestrator = Orchestrator.fromMap(data.orchestrator)
        }
        exec
    }

    ExecutionReference asReference(Closure<String> genTargetNodes = null) {
        JobReferenceImpl jobRef = null
        if (scheduledExecution) {
            jobRef = new JobReferenceImpl(
                    id: scheduledExecution.extid,
                    jobName: scheduledExecution.jobName,
                    groupPath: scheduledExecution.groupPath,
                    project: scheduledExecution.project
            )
        }
        String targetNodes = genTargetNodes?.call(this)
        return new ExecutionReferenceImpl(
                project: project,
                id: id,
                options: argString,
                filter: filter,
                job: jobRef,
                dateStarted: dateStarted,
                status: status,
                succeededNodeList: succeededNodeList,
                failedNodeList: failedNodeList,
                targetNodes: targetNodes
        )
    }
}

