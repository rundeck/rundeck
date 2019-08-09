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

//= require momentutil
//= require knockout.min
//= require knockout-mapping
//= require knockout-foreachprop
//= require nodeFiltersKO
//= require executionOptions
//= require jobFiltersKO
//= require knockout-onenter
//= require knockout-node-filter-link
//= require menu/job-remote-options
//= require menu/joboptions
//= require ko/binding-popover
//= require ko/binding-datetimepicker
//= require ko/handler-bootstrapPopover
//= require ko/handler-bootstrapTooltip
//= require ko/handler-messageTemplate
//= require ko/binding-css2
//= require scheduledExecution/jobRunFormOptionsKO
//= require koBind

/*
 Manifest for "menu/jobs.gsp" page
 */
//TODO: refactor menu/jobs.gsp to move javascript here
/**
 * Possible actions for bulk edit jobs, to present in modal dialog
 * @constructor
 */
function BulkEditor(data){
    var self=this;
    self.messages=data.messages||{};
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
        jQuery('.topgroup .expandComponent').show();
        jQuery('#job_group_tree .expandComponent .expandComponentControl [data-expanding]').each(function(i,e){
            if(jQuery(e).data('expanderOpenClass')){
                jQuery(e).addClass(jQuery(e).data('expanderOpenClass'))
            }
            if(jQuery(e).data('expanderCloseClass')){
                jQuery(e).removeClass(jQuery(e).data('expanderCloseClass'))
            }
        });
        jQuery('#job_group_tree .expandComponent .expandComponentHolder').addClass('expanded');
    };
    self.collapseAllComponents=function(){
        jQuery('.topgroup .expandComponent').hide();
        jQuery('#job_group_tree .expandComponent .expandComponentControl [data-expanding]').each(function(i,e){
            if(jQuery(e).data('expanderOpenClass')){
                jQuery(e).removeClass(jQuery(e).data('expanderOpenClass'))
            }
            if(jQuery(e).data('expanderCloseClass')){
                jQuery(e).addClass(jQuery(e).data('expanderCloseClass'))
            }
        });
        jQuery('#job_group_tree .expandComponent .expandComponentHolder').removeClass('expanded');
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
                    text = self.messages['scm.export.status.EXPORT_NEEDED.description'];
                    break;
                case "CREATE_NEEDED":
                    text = self.messages['scm.export.status.CREATE_NEEDED.description'];
                    break;
                case "CLEAN":
                    text = self.messages['scm.export.status.CLEAN.description'];
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
                    text += self.messages['scm.import.status.IMPORT_NEEDED.description'];
                    break;
                case "DELETE_NEEDED":
                    text += self.messages['scm.import.status.DELETE_NEEDED.description'];
                    break;
                case "CLEAN":
                    text += self.messages['scm.import.status.CLEAN.description'];
                    break;
                case "REFRESH_NEEDED":
                    text += self.messages['scm.import.status.REFRESH_NEEDED.description'];
                    break;
                case "UNKNOWN":
                    text += self.messages['scm.import.status.UNKNOWN.description'];
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
                    text = self.messages['scm.export.status.EXPORT_NEEDED.display.text'];
                    break;
                case "CREATE_NEEDED":
                    text = self.messages['scm.export.status.CREATE_NEEDED.display.text'];
                    break;
                case "REFRESH_NEEDED":
                    text = self.messages['scm.export.status.REFRESH_NEEDED.display.text'];
                    break;
                case "DELETED":
                    text = self.messages['scm.export.status.DELETED.display.text'];
                    break;
                case "CLEAN":
                    text = self.messages['scm.export.status.CLEAN.display.text'];
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
                    text = self.messages['scm.import.status.IMPORT_NEEDED.display.text'];
                    break;
                case "REFRESH_NEEDED":
                    text = self.messages['scm.import.status.REFRESH_NEEDED.display.text'];
                    break;
                case "UNKNOWN":
                    text = self.messages['scm.import.status.UNKNOWN.display.text'];
                    break;
                case "CLEAN":
                    text = self.messages['scm.import.status.CLEAN.display.text'];
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

