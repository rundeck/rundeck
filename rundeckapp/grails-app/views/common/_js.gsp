%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<%@ page import="grails.util.Environment" %>
<script type="text/javascript">
    <g:set var="currentProject" value="${params.project?:request.project}"/>
    <g:set var="projParams" value="${currentProject?[project:currentProject]:[:]}"/>
    var appLinks = {
        disclosureIcon: '${resource(dir:"images",file:"icon-tiny-disclosure.png")}',
        disclosureIconOpen: '${resource(dir:"images",file:"icon-tiny-disclosure-open.png")}',
        iconTinyWarn: '${resource(dir:"images",file:"icon-tiny-warn.png")}',
        iconTinyOk: '${resource(dir:"images",file:"icon-tiny-ok.png")}',
        iconSmallRemoveX: '${resource(dir:"images",file:"icon-small-removex.png")}',
        iconTinyRemoveX: '${resource(dir:"images",file:"icon-tiny-removex.png")}',
        iconSpinner: '${resource(dir:"images",file:"icon-tiny-disclosure-waiting.gif")}',
        executionCancelExecution: '${createLink(controller:"execution",action:"cancelExecution",params:[format:'json'])}',
        tailExecutionOutput: '${createLink(controller: "execution", action: "tailExecutionOutput",params:[format:'json'])}',
        reportsEventsFragment:"${createLink(controller:'reports',action:'eventsFragment',params:projParams)}",
        executionAjaxExecState: "${createLink(action: 'ajaxExecState', controller: 'execution')}",
        executionAjaxExecNodeState: "${createLink(action: 'ajaxExecNodeState', controller: 'execution')}",
        frameworkViewResourceModelConfig: "${createLink(action: 'viewResourceModelConfig', controller: 'framework')}",
        frameworkCheckResourceModelConfig: "${createLink(action: 'checkResourceModelConfig', controller: 'framework')}",
        frameworkEditResourceModelConfig: "${createLink(action: 'editResourceModelConfig', controller: 'framework')}",
        frameworkCreateResourceModelConfig: "${createLink(action: 'createResourceModelConfig', controller: 'framework')}",
        frameworkNodes: "${createLink(controller:"framework",action:"nodes",params:projParams)}",
        frameworkNodesFragment: "${createLink(controller:"framework",action:"nodesFragment",params:projParams)}",
        frameworkNodesQueryAjax: "${createLink(controller:"framework",action:"nodesQueryAjax",params:projParams)}",
        frameworkAdhoc: "${createLink(controller:"framework",action:"adhoc",params:projParams)}",
        frameworkReloadNodes: "${createLink(controller:"framework",action:"reloadNodes",params:projParams)}",
        frameworkNodeSummaryAjax: "${createLink(controller:"framework",action:"nodeSummaryAjax",params:projParams)}",
        frameworkDeleteNodeFilterAjax: "${createLink(controller:"framework",action:"deleteNodeFilterAjax",params:projParams)}",
        reportsEventsAjax: "${g.createLink(controller: 'reports', action: 'eventsAjax',params:projParams)}",
        menuNowrunningAjax: "${g.createLink(controller: 'menu', action: 'nowrunningAjax',params:projParams)}",
        menuHomeAjax: "${g.createLink(controller: 'menu', action: 'homeAjax',params:projParams)}",
        menuHomeSummaryAjax: "${g.createLink(controller: 'menu', action: 'homeSummaryAjax',params:projParams)}",
        menuProjectNamesAjax: "${g.createLink(controller: 'menu', action: 'projectNamesAjax',params:projParams)}",
        menuJobsAjax: "${g.createLink(controller: 'menu', action: 'jobsAjax',params:[format:'json'])}",
        scheduledExecutionRunAdhocInline: "${createLink(controller:'scheduledExecution',action:'runAdhocInline',params:projParams)}",
        scheduledExecutionCreate: "${createLink(controller:'scheduledExecution',action:'create',params:projParams)}",
        scheduledExecutionExecuteFragment: '${createLink(controller:"scheduledExecution",action:"executeFragment",params:projParams)}',
        scheduledExecutionActionMenuFragment: '${createLink(controller:"scheduledExecution",action:"actionMenuFragment",params:projParams)}',
        scheduledExecutionRunJobInline: '${createLink(controller:"scheduledExecution",action:"runJobInline",params:projParams)}',
        scheduledExecutionScheduleJobInline: '${createLink(controller:"scheduledExecution",action:"scheduleJobInline",params:projParams)}',
        scheduledExecutionDetailFragment: '${createLink(controller:'scheduledExecution',action:'detailFragment',params: projParams)}',
        scheduledExecutionDetailFragmentAjax: '${createLink(controller:'scheduledExecution',action:'detailFragmentAjax',params: projParams)}',
        scheduledExecutionJobExecutionsAjax: '${createLink(controller:'scheduledExecution',action:'jobExecutionsAjax',params: projParams)}',
        scheduledExecutionSanitizeHtml: '${createLink(controller:'scheduledExecution',action:'sanitizeHtml',params: projParams)}',
        scheduledExecutionWorkflowJson: '${createLink(controller:'scheduledExecution',action:'workflowJson',params: projParams)}',
        executionFollowFragment: "${createLink(controller:'execution',action:'followFragment',params:projParams)}",
        adhocHistoryAjax: "${createLink(controller:'execution',action:'adhocHistoryAjax',params:projParams)}",
        menuJobs: "${createLink(controller:'menu',action:'jobs',params: projParams)}",
        userAddFilterPref: "${createLink(controller:'user',action:'addFilterPref',params:projParams)}",
        userClearApiToken: "${g.createLink(controller: 'user', action: 'clearApiToken',params:[format:'json'])}",
        userGenerateApiToken: "${g.createLink(controller: 'user', action: 'generateApiToken',params:[format:'json'])}",

        workflowEdit: '${createLink(controller:"workflow",action:"edit",params:projParams)}',
        workflowRender: '${createLink(controller:"workflow",action:"render",params:projParams)}',
        workflowSave: '${createLink(controller:"workflow",action:"save",params:projParams)}',
        workflowReorder: '${createLink(controller:"workflow",action:"reorder",params:projParams)}',
        workflowRemove: '${createLink(controller:"workflow",action:"remove",params:projParams)}',
        workflowUndo: '${createLink(controller:"workflow",action:"undo",params:projParams)}',
        workflowRedo: '${createLink(controller:"workflow",action:"redo",params:projParams)}',
        workflowRevert: '${createLink(controller:"workflow",action:"revert",params:projParams)}',
        workflowRenderUndo: '${createLink(controller:"workflow",action:"renderUndo",params:projParams)}',

        editOptsRenderUndo: '${createLink(controller:"editOpts",action:"renderUndo",params:projParams)}',
        editOptsEdit: '${createLink(controller:"editOpts",action:"edit",params:projParams)}',
        editOptsRender: '${createLink(controller:"editOpts",action:"render",params:projParams)}',
        editOptsSave: '${createLink(controller:"editOpts",action:"save",params:projParams)}',
        editOptsRenderAll: '${createLink(controller:"editOpts",action:"renderAll",params:projParams)}',
        editOptsRenderSummary: '${createLink(controller:"editOpts",action:"renderSummary",params:projParams)}',
        editOptsRemove: '${createLink(controller:"editOpts",action:"remove",params:projParams)}',
        editOptsReorder: '${createLink(controller:"editOpts",action:"reorder",params:projParams)}',
        editOptsUndo: '${createLink(controller:"editOpts",action:"undo",params:projParams)}',
        editOptsRedo: '${createLink(controller:"editOpts",action:"redo",params:projParams)}',
        editOptsRevert: '${createLink(controller:"editOpts",action:"revert",params:projParams)}',
        menuJobsPicker: '${createLink(controller:"menu",action:"jobsPicker",params:projParams)}',
        scheduledExecutionGroupTreeFragment: '${createLink(controller:"scheduledExecution",action:"groupTreeFragment",params:projParams)}',
        storageKeysBrowse: '${createLink(controller: 'menu',action: 'storage')}',
        storageKeysApi: '${createLink(uri:'/storage/access/keys')}',
        storageKeysDownload: '${createLink(uri:'/storage/download/keys')}',
        storageKeysDelete: '${createLink(uri:'/storage/delete/keys')}',
        apiExecutionsBulkDelete: '${createLink(controller:'execution',action: 'deleteBulkApi')}'
    } ;
    <g:if test="${Environment.current==Environment.DEVELOPMENT}" >
    function _messageMissingError(code){
        throw code;
    }
    </g:if>
</script>
