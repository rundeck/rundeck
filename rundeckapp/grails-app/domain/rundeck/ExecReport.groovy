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


class ExecReport extends BaseReport{

    String ctxCommand
    String ctxController
    String jcExecId
    String jcJobId
    Boolean adhocExecution
    String adhocScript
    String abortedByUser
    String succeededNodeList
    String failedNodeList
    String filterApplied

    static mapping = {
        adhocScript type: 'text'
        filterApplied type: 'text'
        succeededNodeList type: 'text'
        failedNodeList type: 'text'
        DomainIndexHelper.generate(delegate) {
            index 'EXEC_REPORT_IDX_0', [/*'class', 'ctxProject', 'dateCompleted',*/ 'jcExecId', 'jcJobId']
            index 'EXEC_REPORT_IDX_1', [/*'ctxProject',*/ 'jcJobId']
            index 'EXEC_REPORT_IDX_2', [/*'class',*/ 'jcExecId']
        }
    }

    static constraints = {
        adhocExecution(nullable:true)
        ctxCommand(nullable:true,blank:true)
        ctxController(nullable:true,blank:true)
        jcExecId(nullable:true,blank:true)
        jcJobId(nullable:true,blank:true)
        adhocScript(nullable:true,blank:true)
        abortedByUser(nullable:true,blank:true)
        succeededNodeList(nullable:true,blank:true)
        failedNodeList(nullable:true,blank:true)
        filterApplied(nullable:true,blank:true)

    }

    public static final ArrayList<String> exportProps = BaseReport.exportProps +[
            'jcExecId',
            'jcJobId',
            'adhocExecution',
            'adhocScript',
            'abortedByUser',
            'succeededNodeList',
            'failedNodeList',
            'filterApplied'
    ]
    def Map toMap(){
        def map = this.properties.subMap(exportProps)
        if (map.status == 'timeout') {
            map.status = 'timedout'
        }
        if (map.actionType == 'timeout') {
            map.actionType = 'timedout'
        }
        map
    }

    static buildFromMap(ExecReport obj, Map map) {
        BaseReport.buildFromMap(obj, map)
    }
    /**
     * Generate an ExecReport based off of an existing execution
     * @param exec
     * @return
     */
    static ExecReport fromExec(Execution exec){
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
        def status = issuccess ? "succeed" : iscancelled ? "cancel" : exec.willRetry ? "retry" : istimedout ?
            "timedout" : "fail"
        return fromMap([
                jcExecId:exec.id,
                jcJobId: exec.scheduledExecution?.id,
                adhocExecution: null==exec.scheduledExecution,
                adhocScript: adhocScript,
                abortedByUser: iscancelled? exec.abortedby ?: exec.user:null,
                node:"${successCount}/${failedCount}/${totalCount}",
                title: adhocScript?adhocScript:summary,
                status: status,
                ctxProject: exec.project,
                reportId: exec.scheduledExecution?( exec.scheduledExecution.groupPath ? exec.scheduledExecution.generateFullName() : exec.scheduledExecution.jobName): 'adhoc',
                author: exec.user,
                message: (issuccess ? 'Job completed successfully' : iscancelled ? ('Job killed by: ' + (exec.abortedby ?: exec.user)) : 'Job failed'),
                dateStarted: exec.dateStarted,
                dateCompleted: exec.dateCompleted,
                actionType: status,
                failedNodeList: failedList,
                succeededNodeList: succeededList,
                filterApplied: exec.filter
        ])
    }
    static ExecReport fromMap(Map map) {
        def report = new ExecReport()
        buildFromMap(report, map.subMap( exportProps))
        report
    }

    String getNodeList(){
        def ret
        if(this.succeededNodeList){
            ret = this.failedNodeList?this.succeededNodeList+',': this.succeededNodeList
        }
        ret = this.failedNodeList?ret+ this.failedNodeList:ret
        ret
    }
}
