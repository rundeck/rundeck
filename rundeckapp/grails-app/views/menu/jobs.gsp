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
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <g:set var="projectName" value="${params.project ?: request.project}"></g:set>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}"/>
    <g:set var="paginateJobs" value="${grailsApplication.config.rundeck.gui.paginatejobs}" />
    <g:set var="paginateJobsPerPage" value="${grailsApplication.config.rundeck.gui.paginatejobs.max.per.page}" />
    <title><g:message code="gui.menu.Workflows"/> - <g:enc>${projectLabel}</g:enc></title>

    <asset:javascript src="util/yellowfade.js"/>
    <asset:javascript src="pagehistory.js"/>
    <asset:javascript src="prototype/effects"/>
    <asset:javascript src="menu/jobs.js"/>
    <g:if test="${grails.util.Environment.current==grails.util.Environment.DEVELOPMENT}">
        <asset:javascript src="menu/joboptionsTest.js"/>
        <asset:javascript src="menu/job-remote-optionsTest.js"/>
    </g:if>
    <g:embedJSON data="${projectNames ?: []}" id="projectNamesData"/>
    <g:embedJSON data="${nextSchedListIds ?: []}" id="nextScheduled"/>
    <g:embedJSON id="pageParams" data="${[project: params.project?:request.project,]}"/>
    <g:jsMessages code="Node,Node.plural,job.starting.execution,job.scheduling.execution,option.value.required,options.remote.dependency.missing.required,,option.default.button.title,option.default.button.text,option.select.choose.text"/>
    <!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
    <script type="text/javascript">
        /** knockout binding for activity */
        var pageActivity;
        function showError(message){
             appendText($('error'),message);
             $("error").show();
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
                _jobExecUnloadHandlers.clear();
            }

            jQuery('#execDiv').modal('hide');
            clearHtml('execDivContent');

            $('busy').hide();
        }
        function requestError(item,message){
            unloadExec();
            showError("Failed request: "+item+". Result: "+message);
        }
        function loadExec(id,eparams) {
            $("error").hide();
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
                            if (!pageActivity.selected()) {
                                pageActivity.activateNowRunningTab();
                            }
                            unloadExec();
                        }
                    } else if (result.error === 'invalid') {
                        // reload form for validation
                        loadExec(null, Form.serialize(elem) + "&dovalidate=true");
                    } else {
                        unloadExec();
                        showError(result.message ? result.message : result.error ? result.error : "Failed request");
                    }
                },
                error: function (data, jqxhr, err) {
                    requestError("runJobInline", err);
                }
            });
        }
        function loadedFormSuccess(doShow,id){
            if ($('execFormCancelButton')) {
                Event.observe($('execFormCancelButton'),'click',function(evt) {
                    Event.stop(evt);
                    unloadExec();
                    return false;
                },false);
                $('execFormCancelButton').name = "_x";
            }
            if ($('execFormRunButton')) {
                Event.observe($('execFormRunButton'),'click', function(evt) {
                    Event.stop(evt);
                    execSubmit('execDivContent', appLinks.scheduledExecutionRunJobInline);
                    $('formbuttons').loading(message('job.starting.execution'));
                    return false;
                },false);
            }
            jQuery('#showScheduler').on('shown.bs.popover', function() {
                if ($('scheduleAjaxButton')) {
                    Event.observe($('scheduleAjaxButton'), 'click', function(evt) {
                        Event.stop(evt);
                        if (isValidDate()) {
                            toggleAlert(true);
		                    execSubmit('execDivContent',
                                appLinks.scheduledExecutionScheduleJobInline);
		                    $('formbuttons').loading(message('job.scheduling.execution'));
                        } else {
                            toggleAlert(false);
                        }
                        return false;
                    }, false);
                }
            });

            //setup option handling
            //setup option edit
            var joboptiondata = loadJsonData('jobOptionData');
            var joboptions = new JobOptions(joboptiondata);

            if (document.getElementById('optionSelect')) {
                ko.applyBindings(joboptions, document.getElementById('optionSelect'));
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
            if(doShow){
                jQuery('#execDiv').modal('show');
            }
            $('busy').hide();
        }




        //set box filterselections

        function _setFilterSuccess(data,name){
            if(data){
                var bfilters=data.filterpref;
                //reload page
                document.location=_genUrl(appLinks.menuJobs , bfilters[name] ? {filterName:bfilters[name]} : {});
            }
        }


        /** now running section update */
        function _pageUpdateNowRunning(count){
        }
        var lastRunExec=0;
        /**
         * Handle embedded content updates
         */
        function _updateBoxInfo(name,data){
            if(name==='events' && data.lastDate){
                histControl.setHiliteSince(data.lastDate);
            }
            if (name == 'nowrunning' && data.lastExecId && data.lastExecId != lastRunExec) {
                lastRunExec = data.lastExecId;
            }
        }


        function initJobIdLinks(){
            jQuery('.act_job_action_dropdown').click(function(){
                var id=jQuery(this).data('jobId');
                var el=jQuery(this).parent().find('.dropdown-menu');
                el.load(
                    _genUrl(appLinks.scheduledExecutionActionMenuFragment,{id:id})
                );
            });
        }
         function filterToggle(evt) {
            ['${enc(js:rkey)}filter','${enc(js:rkey)}filter-toggle'].each(Element.toggle);
        }
        function filterToggleSave(evt) {
            ['${enc(js:rkey)}filter','${enc(js:rkey)}fsave'].each(Element.show);
            ['${enc(js:rkey)}filter-toggle','${enc(js:rkey)}fsavebtn'].each(Element.hide);
        }
        function init(){
            <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false',false])}">
            initJobIdLinks();
            </g:if>

            PageActionHandlers.registerHandler('job_delete_single',function(el){
                bulkeditor.activateActionForJob(bulkeditor.DELETE,el.data('jobId'));
            });
            PageActionHandlers.registerHandler('enable_job_execution_single',function(el){
                bulkeditor.activateActionForJob(bulkeditor.ENABLE_EXECUTION,el.data('jobId'));
            });
            PageActionHandlers.registerHandler('disable_job_execution_single',function(el){
                bulkeditor.activateActionForJob(bulkeditor.DISABLE_EXECUTION,el.data('jobId'));
            });
            PageActionHandlers.registerHandler('disable_job_schedule_single',function(el){
                bulkeditor.activateActionForJob(bulkeditor.DISABLE_SCHEDULE,el.data('jobId'));
            });
            PageActionHandlers.registerHandler('enable_job_schedule_single',function(el){
                bulkeditor.activateActionForJob(bulkeditor.ENABLE_SCHEDULE,el.data('jobId'));
            });

            PageActionHandlers.registerHandler('copy_other_project',function(el){
                jQuery('#jobid').val(el.data('jobId'));
                jQuery('#selectProject').modal();
            });



            $$('.obs_filtertoggle').each(function(e) {
                Event.observe(e, 'click', filterToggle);
            });
            $$('.obs_filtersave').each(function(e) {
                Event.observe(e, 'click', filterToggleSave);
            });
        }
        /**
         * Possible actions for bulk edit jobs, to present in modal dialog
         * @constructor
         */
        function BulkEditor(){
            var self=this;
            self.DISABLE_SCHEDULE = 'disable_schedule';
            self.ENABLE_SCHEDULE = 'enable_schedule';
            self.ENABLE_EXECUTION= 'enable_execution';
            self.DISABLE_EXECUTION= 'disable_execution';
            self.DELETE= 'delete';
            self.action=ko.observable(null);
            self.enabled=ko.observable(false);
            self.beginEdit=function(){
                self.expandAllComponents();
                self.enabled(true);
            };
            self.cancelEdit=function(){
                self.enabled(false);
                self.selectNone();
            };
            self.disableSchedule=function(){

                self.action(self.DISABLE_SCHEDULE);
            };
            self.isDisableSchedule=ko.pureComputed(function(){
                return self.action()===self.DISABLE_SCHEDULE;
            });
            self.enableSchedule=function(){
                self.action(self.ENABLE_SCHEDULE);
            };
            self.isEnableSchedule=ko.pureComputed(function(){
                return self.action()===self.ENABLE_SCHEDULE;
            });
            self.enableExecution=function(){
                self.action(self.ENABLE_EXECUTION);
            };
            self.isEnableExecution=ko.pureComputed(function(){
                return self.action()===self.ENABLE_EXECUTION;
            });
            self.disableExecution=function(){
                self.action(self.DISABLE_EXECUTION);
            };
            self.isDisableExecution=ko.pureComputed(function(){
                return self.action()===self.DISABLE_EXECUTION;
            });
            self.actionDelete=function(){
                self.action(self.DELETE);
            };
            self.isDelete=ko.pureComputed(function(){
                return self.action()===self.DELETE;
            });
            self.cancel=function(){
                self.action(null);
            };

            self.setCheckboxValues=function(ids){
                //check only the checkbox with this job id by passing an array
                jQuery('.jobbulkeditfield :input[name=ids]').val(ids);
            };
            self.checkboxesForGroup=function(group){
                return jQuery('.jobbulkeditfield input[type=checkbox][data-job-group="'+group+'"]');
            };
            self.allCheckboxes=function(group){
                return jQuery('.jobbulkeditfield input[type=checkbox]');
            };
            self.jobGroupSelectAll=function(e){
                var jgroup=jQuery(e).data('job-group');
                if(jgroup){
                    self.checkboxesForGroup(jgroup).prop('checked', true);
                }
            };

            self.jobGroupSelectNone=function(e){
                var jgroup=jQuery(e).data('job-group');
                if(jgroup){
                    self.checkboxesForGroup(jgroup).prop('checked', false);
                }
            };
            self.expandAllComponents=function(){
                jQuery('.expandComponent').show();
            };
            self.collapseAllComponents=function(){
                jQuery('.topgroup .expandComponent').hide();
            };
            self.selectAll=function(){
                self.expandAllComponents();
                self.allCheckboxes().prop('checked', true);
            };
            self.selectNone=function(){
                self.expandAllComponents();
                self.allCheckboxes().prop('checked', false);
            };
            self.toggleModal=function(){
                jQuery('#bulk_del_confirm').modal('toggle');
            };
            self.activateActionForJob=function(action,jobid){
                self.setCheckboxValues([jobid]);
                self.beginEdit();
                self.action(action);
                self.toggleModal();
            };

            self.scmExportEnabled = ko.observable(false);
            self.scmImportEnabled = ko.observable(false);
            self.scmStatus = ko.observable(null);
            self.scmImportJobStatus = ko.observable(null);
            self.scmExportStatus = ko.observable(null);
            self.scmImportStatus = ko.observable(null);
            self.scmExportActions = ko.observable(null);
            self.scmImportActions = ko.observable(null);
            self.scmExportRenamed = ko.observable(null);
            self.isExportEnabled=ko.pureComputed(function(){
                return self.scmExportEnabled();
            });

            self.jobSynchState = function(jobid){
                var exportStatus = null;
                var importStatus = null;
                if(self.scmStatus() && self.scmStatus()[jobid]){
                    exportStatus = self.scmStatus()[jobid].synchState.name;
                }
                if(self.scmImportJobStatus() && self.scmImportJobStatus()[jobid]){
                    importStatus = self.scmImportJobStatus()[jobid].synchState.name;
                }
                if(!exportStatus || exportStatus == "CLEAN"){
                    return importStatus;
                }else{
                    return exportStatus
                }
            };

            self.displayBadge = function(jobid){
                var displayExport = false;
                var displayImport = false;
                if(self.scmExportEnabled() || self.scmImportEnabled()){
                    if(self.scmStatus() && self.scmStatus()[jobid]){
                        displayExport = self.scmStatus()[jobid].synchState.name != "CLEAN";
                    }
                    if(self.scmImportJobStatus() && self.scmImportJobStatus()[jobid]){
                        displayImport = self.scmImportJobStatus()[jobid].synchState.name != "CLEAN";
                    }
                }
                return (displayExport || displayImport);
            };

            self.jobText = function(jobid){
                var exportStatus = null;
                var importStatus = null;
                var text = null;
                if(self.scmStatus() && self.scmStatus()[jobid]){
                    exportStatus = self.scmStatus()[jobid].synchState.name;
                    switch(exportStatus) {
                        case "EXPORT_NEEDED":
                            text = "${message(code: "scm.export.status.EXPORT_NEEDED.description")}";
                            break;
                        case "CREATE_NEEDED":
                            text = "${message(code: "scm.export.status.CREATE_NEEDED.description")}";
                            break;
                        case "CLEAN":
                            text = "${message(code: "scm.export.status.CLEAN.description")}";
                            break;
                        default:
                            text = exportStatus;
                    }
                }
                if(self.scmImportJobStatus() && self.scmImportJobStatus()[jobid]){
                    if(text){
                        text +=', ';
                    }else{
                        text = '';
                    }
                    importStatus = self.scmImportJobStatus()[jobid].synchState.name;
                    switch(importStatus) {
                        case "IMPORT_NEEDED":
                            text += "${message(code: "scm.import.status.IMPORT_NEEDED.description")}";
                            break;
                        case "DELETE_NEEDED":
                            text += "${message(code: "scm.import.status.DELETE_NEEDED.description")}";
                            break;
                        case "CLEAN":
                            text += "${message(code: "scm.import.status.CLEAN.description")}";
                            break;
                        case "REFRESH_NEEDED":
                            text += "${message(code: "scm.import.status.REFRESH_NEEDED.description")}";
                            break;
                        case "UNKNOWN":
                            text += "${message(code: "scm.import.status.UNKNOWN.description")}";
                            break;
                        default:
                            text += importStatus;
                    }

                }
                return text;
            };

            self.jobClass = function(jobid){
                switch(self.jobSynchState(jobid)) {
                    case "EXPORT_NEEDED":
                        return "text-info";
                        break;
                    case "CREATE_NEEDED":
                        return "text-success";
                        break;
                    case "UNKNOWN":
                        return "text-primary";
                        break;
                    case "IMPORT_NEEDED":
                        return "text-warning";
                        break;
                    case "REFRESH_NEEDED":
                        return "text-warning";
                        break;
                    case "DELETED":
                        return "text-danger";
                        break;
                    case "CLEAN":
                        return "text-primary";
                        break;
                }
                return 'text-primary';
            };

            self.jobIcon = function(jobid){
                switch(self.jobSynchState(jobid)) {
                    case "EXPORT_NEEDED":
                        return "glyphicon-exclamation-sign";
                        break;
                    case "CREATE_NEEDED":
                        return "glyphicon-exclamation-sign";
                        break;
                    case "UNKNOWN":
                        return "glyphicon-question-sign";
                        break;
                    case "IMPORT_NEEDED":
                        return "glyphicon-exclamation-sign";
                        break;
                    case "REFRESH_NEEDED":
                        return "glyphicon-exclamation-sign";
                        break;
                    case "DELETED":
                        return "glyphicon-minus-sign";
                        break;
                    case "CLEAN":
                        return "glyphicon-ok";
                        break;
                }
                return 'glyphicon-plus';
            };

            self.exportMessage = function(){
                if(self.scmExportStatus()){
                    return self.scmExportStatus().message;
                }
                return null;
            };
            self.importMessage = function(){
                if(self.scmImportStatus()){
                    return self.scmImportStatus().message;
                }
                return null;
            };

            self.exportState = function(){
                if(self.scmExportStatus()){
                    return self.scmExportStatus().state.name;
                }
                return null;
            };
            self.importState = function(){
                if(self.scmImportStatus()){
                    return self.scmImportStatus().state.name;
                }
                return null;
            };

            self.jobCommit = function(jobid){
                return self.scmExportEnabled();
            };

            self.defaultExportText = function(){
                if(self.exportState()) {
                    var text = null;
                    switch(self.exportState()) {
                        case "EXPORT_NEEDED":
                            text = "${message(code: "scm.export.status.EXPORT_NEEDED.display.text")}";
                            break;
                        case "CREATE_NEEDED":
                            text = "${message(code: "scm.export.status.CREATE_NEEDED.display.text")}";
                            break;
                        case "REFRESH_NEEDED":
                            text = "${message(code: "scm.export.status.REFRESH_NEEDED.display.text")}";
                            break;
                        case "DELETED":
                            text = "${message(code: "scm.export.status.DELETED.display.text")}";
                            break;
                        case "CLEAN":
                            text = "${message(code: "scm.export.status.CLEAN.display.text")}";
                            break;
                    }
                    if(!text){
                        text = self.exportState();
                    }
                    return text;
                }
                return null;
            };

            self.defaultImportText = function(){
                if(self.importState()) {
                    var text = null;
                    switch(self.importState()) {
                        case "IMPORT_NEEDED":
                            text = "${message(code: "scm.import.status.IMPORT_NEEDED.display.text")}";
                            break;
                        case "REFRESH_NEEDED":
                            text = "${message(code: "scm.import.status.REFRESH_NEEDED.display.text")}";
                            break;
                        case "UNKNOWN":
                            text = "${message(code: "scm.import.status.UNKNOWN.display.text")}";
                            break;
                        case "CLEAN":
                            text = "${message(code: "scm.import.status.CLEAN.display.text")}";
                            break;
                    }
                    if(!text){
                        text = self.importState();
                    }
                    return text;
                }
                return null;
            };

            self.defaultDisplayText = function(){
                if(self.exportState() != 'CLEAN'){
                    return self.defaultExportText();
                }else{
                    return self.defaultImportText();
                }
            };


            self.displayExport = function(){
                return (self.exportState() && self.exportState() != 'CLEAN');
            };

            self.displayImport = function(){
                return (self.importState() && self.importState() != 'CLEAN');
            };

            self.displaySCMMEssage = function(){
                return (self.displayExport() || self.displayImport());
            };

        }






        var bulkeditor;
        jQuery(document).ready(function () {
            init();
            if (jQuery('#activity_section')) {
                pageActivity = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
                ko.applyBindings(pageActivity, document.getElementById('activity_section'));
                setupActivityLinks('activity_section', pageActivity);
            }
            jQuery(document).on('click','.act_execute_job',function(evt){
                evt.preventDefault();
               loadExec(jQuery(this).data('jobId'));
            });
            $$('#wffilterform input').each(function(elem){
                if(elem.type=='text'){
                    elem.observe('keypress',noenter);
                }
            });
            bulkeditor=new BulkEditor();
            ko.applyBindings(bulkeditor,document.getElementById('bulk_del_confirm'));
            ko.applyBindings(bulkeditor,document.getElementById('bulk_edit_panel'));
            ko.applyBindings(bulkeditor,document.getElementById('job_action_menu'));
            ko.applyBindings(bulkeditor,document.getElementById('job_group_tree'));
            ko.applyBindings(bulkeditor,document.getElementById('group_controls'));
            ko.applyBindings(bulkeditor,document.getElementById('scm_message'));
            ko.applyBindings(bulkeditor,document.getElementById('scmStatusPopoverOK'));




            jQuery(document).on('click','#togglescm',function(evt){
                evt.preventDefault();
                jQuery.ajax({
                    dataType:'json',
                    method: "POST",
                    url:_genUrl(appLinks.togglescm),
                    params:nextScheduled,
                    success:function(data,status,xhr){
                        console.log(data);
                    }
                });
            });

            var pageParams = loadJsonData('pageParams');
            var nextScheduled = loadJsonData('nextScheduled');
            var nextSchedList="";
            for(var i=0; i< nextScheduled.length; i++){
                nextSchedList = nextSchedList+nextScheduled[i]+",";
            }

            jQuery.ajax({
                dataType:'json',
                method: "POST",
                url:_genUrl(appLinks.scmjobs, {nextScheduled:nextSchedList}),
                params:nextScheduled,
                success:function(data,status,xhr){
                    bulkeditor.scmExportEnabled(data.scmExportEnabled);
                    bulkeditor.scmStatus(data.scmStatus);
                    bulkeditor.scmExportStatus(data.scmExportStatus);
                    bulkeditor.scmExportActions(data.scmExportActions);
                    bulkeditor.scmExportRenamed(data.scmExportRenamed);

                    bulkeditor.scmImportEnabled(data.scmImportEnabled);
                    bulkeditor.scmImportJobStatus(data.scmImportJobStatus);
                    bulkeditor.scmImportStatus(data.scmImportStatus);
                    bulkeditor.scmImportActions(data.scmImportActions);
                }
            });
        });


    </script>

    <asset:javascript src="util/yellowfade.js"/>
    <asset:javascript src="menu/joboptions.js"/>
    <style type="text/css">
    .error{
        color:red;
    }

        #histcontent table{
            width:100%;
        }
        .gsp-pager .step { padding: 0 2px; }
        .gsp-pager .currentStep { padding: 0 2px; }
    </style>
</head>
<body>
<div id="page_jobs" class="container-fluid">
  <g:if test="${flash.bulkJobResult?.errors}">
      <div class="alert alert-warning">
          <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
          <ul>
              <g:if test="${flash.bulkJobResult.errors instanceof org.springframework.validation.Errors}">
                  <g:renderErrors bean="${flash.bulkJobResult.errors}" as="list"/>
              </g:if>
              <g:else>
                  <g:each in="${flash.bulkJobResult.errors*.message}" var="message">
                      <li><g:autoLink>${message}</g:autoLink></li>
                  </g:each>
              </g:else>
          </ul>
      </div>
  </g:if>
  <g:if test="${flash.bulkJobResult?.success}">
      <div class="alert alert-info">
          <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
          <ul>
          <g:each in="${flash.bulkJobResult.success*.message}" var="message">
              <li><g:autoLink>${message}</g:autoLink></li>
          </g:each>
          </ul>
      </div>
  </g:if>
  <div class="row">
    <div class="col-xs-12">

      <div class="card card-plain">
          <div class="card-header">
            <h3 class="card-title"><g:message code="Job.plural" /> (<g:enc>${totalauthorized}</g:enc>)</h3>
          </div>
      </div>
      <div class="card">
        <div class="card-content">
          <div class="runbox primary jobs" id="indexMain">
            <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|idlist$/)}"/>
            <g:render template="workflowsFull"
                      model="${[
                          jobExpandLevel    : jobExpandLevel,
                          jobgroups         : jobgroups,
                          wasfiltered       : wasfiltered ? true : false,
                          clusterMap        : clusterMap,
                          nextExecutions    : nextExecutions,
                          jobauthorizations : jobauthorizations,
                          authMap           : authMap,
                          nowrunningtotal   : nowrunningtotal,
                          max               : max,
                          offset            : offset,
                          paginateParams    : paginateParams,
                          sortEnabled       : true,
                          rkey              : rkey,
                          clusterModeEnabled: clusterModeEnabled
                      ]}"/>
              <div id="error" class="alert alert-danger" style="display:none;"></div>
          </div>
          <g:if test="${paginateJobs && !wasfiltered}">
          <div>
            Showing ${offset+max > total ? total : offset+max} of ${total}
          </div>
           <div class="gsp-pager">
            <g:paginate next="Next" prev="Previous" max="${paginateJobsPerPage}"
            controller="menu" maxsteps="10"
            action="jobs" total="${total}" params="${[max:params.max,offset:params.offset,project:params.project]}" />
            </div>
            </g:if>
        </div>

      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-xs-12">
      <div class="card card-plain">
          <div class="card-header">
            <h3 class="card-title"><g:message code="page.section.Activity.for.jobs" /></h3>
          </div>
      </div>
      <div class="card"  id="activity_section">
        <div class="card-content">
          <g:render template="/reports/activityLinks" model="[filter: [projFilter: params.project ?: request.project, jobIdFilter: '!null',], knockoutBinding: true, showTitle:true]"/>
        </div>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="execDiv" role="dialog" aria-labelledby="deleteFilterModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title" id="deleteFilterModalLabel"><g:message code="job.execute.action.button" /></h4>
      </div>
      <div class="" id="execDivContent"></div>
    </div>
  </div>
</div>

<g:render template="/menu/copyModal" model="[projectNames: projectNames]"/>
</body>
</html>
