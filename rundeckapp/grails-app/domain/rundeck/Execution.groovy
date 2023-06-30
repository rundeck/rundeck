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

import com.dtolabs.rundeck.app.domain.EmbeddedJsonData
import com.dtolabs.rundeck.app.support.DomainIndexHelper
import com.dtolabs.rundeck.app.support.ExecutionContext
import com.dtolabs.rundeck.core.common.FrameworkResource
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.util.XmlParserUtil
import com.fasterxml.jackson.core.JsonParseException
import grails.gorm.DetachedCriteria
import org.rundeck.app.data.model.v1.execution.ExecutionData
import rundeck.data.job.RdNodeConfig
import rundeck.data.validation.shared.SharedExecutionConstraints
import rundeck.data.validation.shared.SharedNodeConfigConstraints
import rundeck.data.validation.shared.SharedProjectNameConstraints
import rundeck.data.validation.shared.SharedServerNodeUuidConstraints
import rundeck.services.ExecutionService
import rundeck.services.execution.ExecutionReferenceImpl

/**
* Execution
*/
class Execution extends ExecutionContext implements EmbeddedJsonData, ExecutionData {
    ScheduledExecution scheduledExecution
    String uuid = UUID.randomUUID().toString()
    String jobUuid
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
    Orchestrator orchestrator
    String userRoleList
    String serverNodeUUID
    Integer nodeThreadcount=1
    Long retryOriginalId
    Long retryPrevId
    String extraMetadata

    boolean serverNodeUUIDChanged = false

    static hasOne = [logFileStorageRequest: LogFileStorageRequest]
    static transients = ['executionState', 'customStatusString', 'userRoles', 'extraMetadataMap', 'serverNodeUUIDChanged']
    static constraints = {
        importFrom SharedExecutionConstraints
        importFrom SharedNodeConfigConstraints
        importFrom SharedServerNodeUuidConstraints
        project(matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX, validator:{ val, Execution obj->
            if(obj.scheduledExecution && obj.scheduledExecution.project!=val){
                return 'job.project.mismatch.error'
            }
        })
        logFileStorageRequest(nullable:true)
        workflow(nullable:true)
        scheduledExecution(nullable:true)
        orchestrator(nullable: true)
        retryExecution(nullable: true)
        retryOriginalId(nullable: true)
        retryPrevId(nullable: true)
        extraMetadata(nullable: true)
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
        extraMetadata(type: 'text')

        DomainIndexHelper.generate(delegate) {
            index 'EXEC_IDX_1', ['id', 'project', 'dateCompleted']
            index 'EXEC_IDX_2', ['dateStarted', 'status']
            index 'EXEC_IDX_3', ['project', 'dateCompleted']
            index 'EXEC_IDX_4', ['dateCompleted', 'scheduledExecution']
            index 'EXEC_IDX_5', ['scheduledExecution', 'status']
            index 'EXEC_IDX_6', ['user','dateStarted']
            index 'EXEC_IDX_7', ['serverNodeUUID']
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
        lastExecutionDateByUser { user ->
            eq 'user', user
            projections {
                property 'dateStarted'
            }
            maxResults 1
            order 'dateStarted', 'desc'
        }
    }

    static DetachedCriteria<Execution> runningExecutionsCriteria = new DetachedCriteria<>(Execution).build {
        isNotNull('dateStarted')
        isNull('dateCompleted')
        or {
            isNull('status')
            and{
                ne('status', ExecutionService.EXECUTION_SCHEDULED)
                ne('status', ExecutionService.EXECUTION_QUEUED)
            }
        }
    }

    @Override
    Serializable getRetryExecutionId() {
        return retryExecution?.id
    }

    @Override
    Serializable getLogFileStorageRequestId() {
        return logFileStorageRequest?.id
    }

    RdNodeConfig getNodeConfig() {
        new RdNodeConfig(
                nodeInclude : nodeInclude,
                nodeExclude : nodeExclude,
                nodeIncludeName : nodeIncludeName,
                nodeExcludeName : nodeExcludeName,
                nodeIncludeTags : nodeIncludeTags,
                nodeExcludeTags : nodeExcludeTags,
                nodeIncludeOsName : nodeIncludeOsName,
                nodeExcludeOsName : nodeExcludeOsName,
                nodeIncludeOsFamily : nodeIncludeOsFamily,
                nodeExcludeOsFamily : nodeExcludeOsFamily,
                nodeIncludeOsArch : nodeIncludeOsArch,
                nodeExcludeOsArch : nodeExcludeOsArch,
                nodeIncludeOsVersion : nodeIncludeOsVersion,
                nodeExcludeOsVersion : nodeExcludeOsVersion,
                nodeExcludePrecedence : nodeExcludePrecedence,
                successOnEmptyNodeFilter: successOnEmptyNodeFilter,
                filter: filter,
                filterExclude: filterExclude,
                excludeFilterUncheck: excludeFilterUncheck,
                nodeKeepgoing : nodeKeepgoing,
                doNodedispatch : doNodedispatch,
                nodeRankAttribute : nodeRankAttribute,
                nodeRankOrderAscending : nodeRankOrderAscending,
                nodeFilterEditable : nodeFilterEditable,
                nodeThreadcount : nodeThreadcount
        )
    }

    public String toString() {
        return "Workflow execution: ${workflow}"
    }

    Map getExtraMetadataMap() {
        extraMetadata ? asJsonMap(extraMetadata) : [:]
    }

    void setExtraMetadataMap(Map config) {
        extraMetadata = config ? serializeJsonMap(config) : null
    }


    public setUserRoles(List l) {
        def json = serializeJsonList(l)
        setUserRoleList(json)
    }

    public List getUserRoles() {
        if (userRoleList) {
            try {
                return asJsonList(userRoleList)
            } catch(JsonParseException ex) {
                return Arrays.asList(userRoleList.split(/,/))
            }
        } else {
            return []
        }
    }
    public boolean statusSucceeded(){
        return getExecutionState()==ExecutionService.EXECUTION_SUCCEEDED
    }

    public String getExecutionState() {
        return cancelled ? ExecutionService.EXECUTION_ABORTED :
            (null == dateCompleted && status == ExecutionService.EXECUTION_QUEUED) ? ExecutionService.EXECUTION_QUEUED :
                null != dateStarted && dateStarted.getTime() > System.currentTimeMillis() ? ExecutionService.EXECUTION_SCHEDULED :
                    (null == dateCompleted && status!=ExecutionService.AVERAGE_DURATION_EXCEEDED) ? ExecutionService.EXECUTION_RUNNING :
                        (status == ExecutionService.AVERAGE_DURATION_EXCEEDED) ? ExecutionService.AVERAGE_DURATION_EXCEEDED:
                            (status in ['true', 'succeeded']) ? ExecutionService.EXECUTION_SUCCEEDED :
                                cancelled ? ExecutionService.EXECUTION_ABORTED :
                                    willRetry ? ExecutionService.EXECUTION_FAILED_WITH_RETRY :
                                        timedOut ? ExecutionService.EXECUTION_TIMEDOUT :
                                            (status == 'missed') ? ExecutionService.EXECUTION_MISSED :
                                                (status in ['false', 'failed']) ? ExecutionService.EXECUTION_FAILED :
                                                    isCustomStatusString(status) ? ExecutionService.EXECUTION_STATE_OTHER : status.toLowerCase()

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
                                                 ExecutionService.EXECUTION_QUEUED,
                                                 ExecutionService.EXECUTION_SCHEDULED,
                                                 ExecutionService.AVERAGE_DURATION_EXCEEDED])
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
        if (this.retryPrevId) {
            map.retryPrevId = retryPrevId
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
        if (this.extraMetadata) {
            map.extra = this.extraMetadataMap
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
        if (data.retryPrevId) {
            exec.retryPrevId = Long.valueOf(data.retryPrevId)
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
        if (data.extra instanceof Map) {
            exec.extraMetadataMap = data.extra
        }
        exec
    }

    ExecutionReference asReference(Closure<String> genTargetNodes = null) {
        JobReference jobRef = null
        String adhocCommand = null
        if (scheduledExecution) {
            jobRef = scheduledExecution.asReference()
        } else if (workflow && workflow.commands && workflow.commands[0]) {
            adhocCommand = workflow.commands[0].summarize()
        }
        String targetNodes = genTargetNodes?.call(this)
        return new ExecutionReferenceImpl(
                project: project,
                id: id,
                retryOriginalId: retryOriginalId?.toString(),
                retryPrevId: retryPrevId?.toString(),
                retryNextId: retryExecution?.id?.toString(),
                options: argString,
                filter: filter,
                job: jobRef,
                adhocCommand: adhocCommand,
                dateStarted: dateStarted,
                status: status,
                succeededNodeList: succeededNodeList,
                failedNodeList: failedNodeList,
                targetNodes: targetNodes,
                metadata: extraMetadataMap,
                scheduled: executionType in ['scheduled','user-scheduled'],
                executionType: executionType
        )
    }

    void beforeUpdate() {
        serverNodeUUIDChanged = this.isDirty('serverNodeUUID')
    }
}

