<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="skipPrototypeJs" content="true"/>

    <g:set var="projectName" value="${params.project ?: request.project}"/>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}"/>
    <title><g:message code="gui.menu.Workflows"/> - <g:enc>${projectLabel}</g:enc></title>
    <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name: projectName, action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])}"/>
    <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name: projectName, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>

    <asset:javascript src="menu/jobs.next.js"/>
    <asset:javascript src="static/pages/project-activity.js" defer="defer"/>
    <asset:javascript src="static/pages/job/browse.js" defer="defer"/>
    <asset:stylesheet href="static/css/pages/job/browse.css" />

    <g:jsMessages code="Node,Node.plural,job.starting.execution,job.scheduling.execution,option.value.required,options.remote.dependency.missing.required,,option.default.button.title,option.default.button.text,option.select.choose.text"/>

    <g:javascript>
//LEGACY, to be removed
        function showError(message){
             appendText('#error',message);
             jQuery("#error").show();
        }
        function showErrorModal(message){
             setText('#modalErrorContent',message);
             jQuery("#errorModal").modal('show');
        }
        var _jobExecUnloadHandlers=new Array();
        function _registerJobExecUnloadHandler(handler){
            _jobExecUnloadHandlers.push(handler);
        }
        function unloadExec(){
            if(_jobExecUnloadHandlers.length>0){
                for(var i =0;i<_jobExecUnloadHandlers.length;i++){
                    _jobExecUnloadHandlers[i].call();
                }
                _jobExecUnloadHandlers.length=0;
            }

            jQuery('#execDiv').modal('hide');
            clearHtml('execDivContent');

            jQuery('#busy').hide();
        }
        function requestError(item,message){
            unloadExec();
            showError("Failed request: "+item+". Result: "+message);
        }
        function loadExec(id,eparams) {
            jQuery("#error").hide();
            var params=eparams;
            if(!params){
                params={id:id};
            }
            jQuery('#execDivContent').load(_genUrl(appLinks.scheduledExecutionExecuteFragment, params),
                function(response,status,xhr){
                if (status == "success") {
                    loadedFormSuccess(!!id,id);
                } else {
                    requestError("executeFragment for [" + id + "]",xhr.statusText);
                }
            });
        }
        function execSubmit(elem, target) {
            var data = new FormData(jQuery('#' + elem + ' form')[0]);
            jQuery.ajax({
                url: target,
                type: 'POST',
                data: data,
                contentType: false,
                dataType: 'json',
                processData: false,
                success: function (result) {
                    if (result.id) {
                        if (result.follow && result.href) {
                            document.location = result.href;
                        } else {

                            unloadExec();
                        }
                    } else if (result.error === 'invalid') {
                        // reload form for validation
                        loadExec(null, jQuery('#' + elem + ' form').serialize() + "&dovalidate=true");
                    } else {
                        unloadExec();
                        showErrorModal("Failed to run job: "+(result.message ? result.message : result.error ? result.error : "Failed request"));
                    }
                },
                error: function (data, jqxhr, err) {
                    requestError("runJobInline", err);
                }
            });
        }
        function loadedFormSuccess(doShow,id){
            jQuery('#execDivContent .exec-options-body').addClass('modal-body')
            jQuery('#execDivContent .exec-options-footer').addClass('modal-footer')
            if (jQuery('#execFormCancelButton').length) {
                jQuery('#execFormCancelButton').on('click',function(evt) {
                    stopEvent(evt);
                    unloadExec();
                    return false;
                });
                jQuery('#execFormCancelButton').attr('name', "_x");
            }
            if (jQuery('#execFormRunButton').length) {
                let clicked=false
                jQuery('#execFormRunButton').on('click', function(evt) {
                    stopEvent(evt);
                    if (clicked) {
                        return false;
                    }
                    clicked = true;
                    jQuery('#execOptFormRunButtons').hide()
                    jQuery('#execOptFormRunJobSpinner').css('display', 'flex')
                    execSubmit('execDivContent', appLinks.scheduledExecutionRunJobInline);
                    return false;
                });
            }
            jQuery('#showScheduler').on('shown.bs.popover', function() {
                if (jQuery('#scheduleAjaxButton').length) {
                    jQuery('#scheduleAjaxButton').on( 'click', function(evt) {
                        stopEvent(evt);
                        if (isValidDate()) {
                            toggleAlert(true);
		                    execSubmit('execDivContent',
                                appLinks.scheduledExecutionScheduleJobInline);
		                    //$('formbuttons').loading(message('job.scheduling.execution'));
                        } else {
                            toggleAlert(false);
                        }
                        return false;
                    });
                }
            });

            //setup option handling
            //setup option edit
            var joboptiondata = loadJsonData('jobOptionData');
            var joboptions = new JobOptions(joboptiondata);

            if (document.getElementById('optionSelect')) {
                // ko.applyBindings(joboptions, document.getElementById('optionSelect'));
            }

            var remoteoptionloader = new RemoteOptionLoader({
                url: "${createLink(controller:'scheduledExecution',action:'loadRemoteOptionValues',params:[format:'json'])}",
                id:id,
                fieldPrefix: "extra.option."
            });
            var remotecontroller = new RemoteOptionController({
                loader: remoteoptionloader,
            });
            remotecontroller.setupOptions(joboptions);

            remotecontroller.loadData(loadJsonData('remoteOptionData'));
            if (typeof(_registerJobExecUnloadHandler) == 'function') {
                _registerJobExecUnloadHandler(remotecontroller.unsubscribeAll);
            }
            joboptions.remoteoptions = remotecontroller;
            remotecontroller.begin();

            jQuery('input').on('keydown', function (evt) {
                return noenter(evt);
            });
            initKoBind('#execDiv',{joboptions:joboptions},/*'menu/jobs'*/)
            if(doShow){
                jQuery('#execDiv').modal('show');
            }
            jQuery('#busy').hide();
        }
        jQuery(document).ready(function () {
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

            jQuery(document).on('click','.act_execute_job',function(evt){
                evt.preventDefault();
               loadExec(jQuery(this).data('jobId'));
            });
            });
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
            pagination:{
                max: ${enc(js:params.max?params.int('max',10):10)}
        },
        query:{
            jobIdFilter:'!null'
          },
          filterOpts: {
              showFilter: false,
              showRecentFilter: true,
              showSavedFilter: false
          },
          runningOpts: {
              loadRunning:false,
              allowAutoRefresh: false
          }
  }
})
    </g:javascript>
    <g:embedJSON id="pageQueryParams" data="${[queryParams:params]}"/>
</head>

<body>
<div class="vue-ui-socket">
    <ui-socket section="job-list-page" location="main"/>

</div>

<div class="modal fade" id="execDiv" role="dialog" aria-labelledby="execJobModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="execJobModalLabel"><g:message code="job.execute.action.button" /></h4>
            </div>
            <div class="" id="execDivContent"></div>
        </div>
    </div>
</div>
<div class="modal fade" id="errorModal" role="dialog" aria-labelledby="execErrModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="execErrModalLabel"><g:message code="request.error.title" /></h4>
            </div>
            <div class="modal-body">
                <div class="alert alert-danger" id="modalErrorContent"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal"><g:message code="ok" default="OK"/></button>
            </div>
        </div>
    </div>
</div>
<g:render template="/menu/copyModal" model="[projectNames: projectNames]"/>
</body>