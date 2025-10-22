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
import org.rundeck.app.data.model.v1.report.RdExecReport

class BaseReport implements RdExecReport{

    String className = "ExecReport"
    String node
    String title
    String status
    @Deprecated
    String actionType
    String project
    @Deprecated
    String ctxType
    @Deprecated
    String ctxName
    @Deprecated
    String maprefUri
    String reportId
    String tags
    String author
    Date dateStarted
    Date dateCompleted 
    String message

    @Deprecated
    String ctxCommand
    @Deprecated
    String ctxController
    Long executionId
    String jobId
    Boolean adhocExecution
    String adhocScript
    String abortedByUser
    String succeededNodeList
    String failedNodeList
    String filterApplied
    String jobUuid
    String executionUuid

    static mapping = {
        message type: 'text'
        title type: 'text'
        project column: 'ctx_project'
        adhocScript type: 'text'
        filterApplied type: 'text'
        succeededNodeList type: 'text'
        failedNodeList type: 'text'
        jobId column: 'jc_job_id'
        className column: 'CLASS'

        DomainIndexHelper.generate(delegate) {
            index 'EXEC_REPORT_IDX_0', [/*'class',*/ 'ctxProject', 'dateCompleted', /*'jcExecId', 'jcJobId'*/]
            index 'EXEC_REPORT_IDX_1', ['ctxProject'/*, 'jcJobId'*/]
            index 'BASE_REPORT_IDX_2', [/*'class',*/ 'ctxProject', 'dateCompleted', 'dateStarted']
        }
    }

   static constraints = {
        reportId(nullable:true, maxSize: 1024+2048 /*jobName + groupPath size limitations from ScheduledExecution*/)
        tags(nullable:true)
        node(nullable:true)
        maprefUri(nullable:true)
        ctxName(nullable:true)
        ctxType(nullable:true)
        status(nullable:false, maxSize: 256)
        actionType(nullable:false, maxSize: 256)

       adhocExecution(nullable:true)
       ctxCommand(nullable:true,blank:true)
       ctxController(nullable:true,blank:true)
       jobId(nullable:true,blank:true)
       executionId(nullable:true)
       adhocScript(nullable:true,blank:true)
       abortedByUser(nullable:true,blank:true)
       succeededNodeList(nullable:true,blank:true)
       failedNodeList(nullable:true,blank:true)
       filterApplied(nullable:true,blank:true)
       jobUuid(nullable:true)
       executionUuid(nullable:true)
    }
    public static final ArrayList<String> exportProps = [
            'node',
            'title',
            'status',
            'actionType',
            'project',
            'reportId',
            'tags',
            'author',
            'message',
            'dateStarted',
            'dateCompleted',
            'executionId',
            'jobId',
            'adhocExecution',
            'adhocScript',
            'abortedByUser',
            'succeededNodeList',
            'failedNodeList',
            'filterApplied',
            'jobUuid',
            'executionUuid'
    ]

    def Map toMap(){
        def map=this.properties.subMap(exportProps)
        if(map.status=='timeout'){
            map.status='timedout'
        }
        if(map.actionType=='timeout'){
            map.actionType='timedout'
        }
        map
    }

    static buildFromMap(BaseReport obj, Map data) {
        data.each { k, v ->
            if ((k == 'status' || k == 'actionType')) {
                if (v == 'timedout') {
                    //XXX: use 'timeout' internally for timedout status, due to previous varchar(7) length limitations on
                    // the field :-Σ
                    v = 'timeout'
                }else if (v == 'succeeded') {
                    v='succeed'
                }else if (v.toString().length()>7) {
                    v='other'
                }
            }
            obj[k] = v
        }
    }

    static BaseReport fromMap(Map data) {
        def BaseReport report = new BaseReport()
        buildFromMap(report, data.subMap(exportProps))
        report
    }

    static void deleteByProject(String project) {
        BaseReport.where {
            project == project
        }.deleteAll()
    }

    /**
     * Generate an ExecReport based off of an existing execution
     * @param exec
     * @return
     */
    static BaseReport fromExec(Execution exec){
        def failedCount = exec.failedNodeList ?exec.failedNodeList.split(',').size():0
        def successCount=exec.succeededNodeList? exec.succeededNodeList.split(',').size():0;
        def failedList = exec.failedNodeList ?exec.failedNodeList:''
        def succeededList=exec.succeededNodeList? exec.succeededNodeList:'';
        def totalCount = failedCount+successCount;
        def adhocScript = null
        if(
                null == exec.scheduledExecution
                        && exec.workflow.commands
                        && exec.workflow.commands.size()==1
                        && exec.workflow.commands[0] instanceof CommandExec
        ){
            adhocScript=exec.workflow.commands[0].adhocRemoteString
        }
        def summary = "[${exec.workflow.commands?exec.workflow.commands.size():0} steps]"
        def issuccess = exec.statusSucceeded()
        def iscancelled = exec.cancelled
        def istimedout = exec.timedOut
        def ismissed = exec.status == "missed"
        def status = issuccess ? "succeed" : iscancelled ? "cancel" : exec.willRetry ? "retry" : istimedout ?
                "timedout" : ismissed ? "missed" : "fail"
        return fromMap([
                executionId: exec.id,
                jobId: exec.scheduledExecution?.id,
                adhocExecution: null==exec.scheduledExecution,
                adhocScript: adhocScript,
                abortedByUser: iscancelled? exec.abortedby ?: exec.user:null,
                node:"${successCount}/${failedCount}/${totalCount}",
                title: adhocScript?adhocScript:summary,
                status: status,
                project: exec.project,
                reportId: exec.scheduledExecution?( exec.scheduledExecution.groupPath ? exec.scheduledExecution.generateFullName() : exec.scheduledExecution.jobName): 'adhoc',
                author: exec.user,
                message: (issuccess ? 'Job completed successfully' : iscancelled ? ('Job killed by: ' + (exec.abortedby ?: exec.user)) : ismissed ? "Job missed execution at: ${exec.dateStarted}" : 'Job failed'),
                dateStarted: exec.dateStarted,
                dateCompleted: exec.dateCompleted,
                actionType: status,
                failedNodeList: failedList,
                succeededNodeList: succeededList,
                filterApplied: exec.filter,
                jobUuid: exec.scheduledExecution?.uuid,
                executionUuid: exec.uuid
        ])
    }


    String getNodeList(){
        def ret
        if(this.succeededNodeList){
            ret = this.failedNodeList?this.succeededNodeList+',': this.succeededNodeList
        }
        ret = this.failedNodeList?ret+ this.failedNodeList:ret
        ret
    }

    def beforeInsert() {
        if (!this.className) {
            this.className = "BaseReport"
        }
    }
}
