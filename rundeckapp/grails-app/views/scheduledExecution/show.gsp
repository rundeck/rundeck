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

<%@ page import="org.rundeck.core.auth.AuthConstants; grails.util.Environment" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="skipPrototypeJs" content="true"/>

    <title><g:appTitle/> - <g:enc>${scheduledExecution?.jobName}</g:enc></title>


    <asset:javascript src="scheduledExecution/show.js"/>
    <asset:javascript src="util/markdeep.js"/>
    <asset:javascript src="vendor/jquery.autocomplete.min.js"/>
    <asset:javascript src="util/tab-router.js"/>
    <asset:javascript src="static/pages/nodes.js" defer="defer"/>
    <asset:stylesheet src="static/css/pages/nodes.css"/>

    <asset:stylesheet href="static/css/pages/project-dashboard.css"/>
    <g:jsMessages code="jobslist.date.format.ko,select.all,select.none,delete.selected.executions,cancel.bulk.delete,cancel,close,all,bulk.delete,running"/>
    <g:jsMessages code="search.ellipsis
jobquery.title.titleFilter
jobquery.title.jobFilter
jobquery.title.jobIdFilter
jobquery.title.userFilter
jobquery.title.statFilter
jobquery.title.filter
jobquery.title.recentFilter
jobquery.title.startbeforeFilter
jobquery.title.startafterFilter
jobquery.title.endbeforeFilter
jobquery.title.endafterFilter
saved.filters
search
"/>
    <g:embedJSON id="jobParams"
                 data="${[filter: scheduledExecution?.filter, doNodeDispatch: scheduledExecution?.doNodedispatch, project: params.project
                         ?:
                         request.project]}"/>
    <g:embedJSON id="jobDetail"
                 data="${[isScheduled: isScheduled, id: scheduledExecution?.extid, name: scheduledExecution?.jobName, group: scheduledExecution?.groupPath,
                          project: params.project ?:
                                   request.project, scheduled: scheduledExecution?.scheduled, scheduleEnabled: scheduledExecution?.
                         hasScheduleEnabled(), executionEnabled: scheduledExecution?.hasExecutionEnabled()]}"/>
    <g:embedJSON id="pageParams" data="${[project: params.project ?: request.project]}"/>

    <g:jsMessages code="Node,Node.plural,option.value.required,options.remote.dependency.missing.required,option.default.button.title,option.default.button.text,option.select.choose.text"/>
    <script type="text/javascript">
        var joboptions;
        var remotecontroller;

        function init() {
            "use strict";
            var params = loadJsonData('jobParams');
            var jobNodeFilters = initJobNodeFilters(params);



            //setup option edit
            var joboptiondata = loadJsonData('jobOptionData');
            joboptions = new JobOptions(joboptiondata);




            var remoteoptionloader = new RemoteOptionLoader({
                url: "${createLink(controller:'scheduledExecution',action:'loadRemoteOptionValues',params:[format:'json'])}",
                id:"${scheduledExecution.extid}",
                fieldPrefix: "extra.option."
            });
            remotecontroller = new RemoteOptionController({ loader: remoteoptionloader});
            remotecontroller.setupOptions(joboptions);
            remotecontroller.loadData(loadJsonData('remoteOptionData'));

            joboptions.remoteoptions = remotecontroller;
            remotecontroller.begin();

            jQuery('input').on('keydown', function (evt) {
                return noenter(evt);
            });

            setupTabRouter('#jobtabs');

            PageActionHandlers.registerHandler('copy_other_project',function(el){
                jQuery('#jobid').val(el.data('jobId'));
                jQuery('#selectProject').modal();
                jQuery.ajax({
                    dataType:'json',
                    method: 'GET',
                    url:_genUrl(appLinks.authProjectsToCreateAjax),
                    success:function(data){
                        jQuery('#jobProject').empty();
                        for (let i in data.projectNames ) {
                            jQuery('#jobProject').append(
                                '<option value="' + data.projectNames[i] + '">' + data.projectNames[i] + '</option>'
                            );
                        }
                    }
                });
            });
            if (jQuery('#execFormRunButton').length) {
                let clicked=false
                jQuery('#execFormRunButton').on('click', function(evt) {
                    if (clicked) {
                        return false;
                    }
                    clicked = true
                    jQuery('#execOptFormRunButtons').hide()
                    jQuery('#execOptFormRunJobSpinner').css('display', 'flex')
                    return true
                });
            }

            initKoBind(null,
                {
                    jobNodeFilters: jobNodeFilters,
                    joboptions: joboptions,
                },
                // 'job/show'
            )
        }
        jQuery(init);
    </script>
    <g:set var="projectName" value="${scheduledExecution.project}"/>
    <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(
            context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name: projectName, action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])}"/>
    <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name:
            projectName, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>
    <g:set var="runAccess" value="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_RUN)}"/>
    <g:set var="readAccess" value="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_READ)}"/>
    <g:embedJSON id="authJson"
                 data="${[
                         projectAdmin: projAdminAuth,
                         deleteExec  : deleteExecAuth,
                         jobRun      : runAccess,
                         jobRead     : readAccess
                 ]}"/>


    <g:javascript>
    window._rundeck = Object.assign(window._rundeck || {}, {
        data:{
            projectAdminAuth:${enc(js:projAdminAuth)},
            deleteExecAuth:${enc(js:deleteExecAuth)},
            jobslistDateFormatMoment:"${enc(js:g.message(code:'jobslist.date.format.ko'))}",
            runningDateFormatMoment:"${enc(js:g.message(code:'jobslist.running.format.ko'))}",
            activityUrl: appLinks.reportsEventsAjax,
            nowrunningUrl: "${createLink(uri:"/api/${com.dtolabs.rundeck.app.api.ApiVersions.API_CURRENT_VERSION}/project/${projectName}/executions/running")}",
            bulkDeleteUrl: appLinks.apiExecutionsBulkDelete,
            activityPageHref:"${enc(js:createLink(controller:'reports',action:'index',params:[project:projectName]))}",
            sinceUpdatedUrl:"${enc(js:g.createLink(controller:'reports',action: 'since.json', params: [project:projectName]))}",
            filterListUrl:"${enc(js:g.createLink(controller:'reports',action: 'listFiltersAjax', params: [project:projectName]))}",
            filterSaveUrl:"${enc(js:g.createLink(controller:'reports',action: 'saveFilterAjax', params: [project:projectName]))}",
            filterDeleteUrl:"${enc(js:g.createLink(controller:'reports',action: 'deleteFilterAjax', params: [project:projectName]))}",
            autorefreshms:30000,
            pagination:{
                max: ${enc(js:params.max?params.int('max',10):10)}
            },
            query:{
                jobIdFilter:"${enc(js:scheduledExecution.extid)}"
            },
            filterOpts: {
                showFilter: false,
                showRecentFilter: true,
                showSavedFilter: false
            },
            runningOpts: {
                loadRunning: true,
                autorefresh:true,
                allowAutoRefresh: true
            }
    }
})
    </g:javascript>
    <asset:javascript src="static/pages/project-activity.js" defer="defer"/>
    <asset:javascript src="static/components/copybox.js"/>
</head>

<body>
<div class="content">
<div id="layoutBody">
    <g:if test="${flash.info}">
        <div class="list-group-item">
            <div class="alert alert-info">
                <g:enc>${flash.info}</g:enc>
            </div>
        </div>
    </g:if>
<tmpl:show scheduledExecution="${scheduledExecution}" crontab="${crontab}" jobComponents="${jobComponents}" jobComponentValues="${jobComponentValues}"/>
<g:render template="/menu/copyModal"
          model="[projectNames: projectNames]"/>
</div>
</div>
</body>
</html>


