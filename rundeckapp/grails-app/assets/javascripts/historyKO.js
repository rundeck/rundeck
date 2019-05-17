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
//= require knockout-foreachprop
//= require knockout-mapping


function Report(data) {
    var self = this;
    self.status = ko.observable();
    self.dateCompleted = ko.observable();
    self.dateStarted = ko.observable();
    self.execution = ko.observable();
    self.executionHref = ko.observable();
    self.jobId = ko.observable();
    self.jobDeleted = ko.observable(false);
    self.executionId = ko.observable();
    self.reportId = ko.observable();
    self.title = ko.observable();
    self.jobAverageDuration = ko.observable(0);
    self.duration = ko.observable(0);
    self.timeNow = ko.observable(new Date());
    window.setInterval(function() { self.timeNow(new Date()) }, 5000);
    //true if checked for bulk edit
    self.bulkEditSelected=ko.observable(false);

    self.durationSimple = ko.computed(function () {
        return MomentUtil.formatDurationSimple(self.duration());
    });
    self.durationHumanize = ko.computed(function () {
        return MomentUtil.formatDurationHumanize(self.duration());
    });
    self.startTimeFormat = function (format) {
        return MomentUtil.formatTime(self.dateStarted(), format);
    };
    self.endTimeSimple = ko.computed(function () {
        return MomentUtil.formatTimeSimple(self.dateCompleted());
    });
    self.timeToStart = ko.computed(function () {
        self.timeNow();
        return moment(self.dateStarted()).fromNow();
    });
    self.statusList = ['scheduled','running','succeed','succeeded','failed',
        'cancel','aborted','retry','timedout','timeout','fail'];

    self.isCustomStatus = ko.computed(function () {
        return self.statusList.indexOf(self.status()) < 0 ;
    });
    self.customStatusString = ko.computed(function () {
        return self.execution()?self.execution().status():status();
    });
    self.isRetry = ko.computed(function () {
        return self.execution() && self.execution().retry && self.execution().retry();
    });
    self.hasRetryExec = ko.computed(function () {
        return self.execution() && self.execution().retryExecutionId && self.execution().retryExecutionId();
    });
    self.wasTimedOut = ko.computed(function () {
        return self.execution() && self.execution().timedOut && self.execution().timedOut();
    });
    self.css = ko.computed(function () {
        if (self.status() == 'scheduled') {
            return 'scheduled';
        }
        if (self.status() == 'succeed' || self.status() == 'succeeded') {
            return 'succeeded';
        }
        if (self.status() == 'fail' || self.status() == 'failed') {
            return 'failed';
        }
        if (self.status() == 'cancel' || self.status() == 'aborted') {
            return 'aborted';
        }
        if (self.status() == 'running') {
            return 'running';
        }
        if (self.status() == 'timedout') {
            return 'timedout';
        }
        if (self.status() == 'retry') {
            return 'failed-with-retry';
        }
        return 'other';
    });
    self.executionState = ko.computed(function () {
        var css = self.css();
        var exec = self.execution();

        if (self.hasRetryExec()) {
            return 'FAILED-WITH-RETRY';
        }
        if (self.wasTimedOut()) {
            return 'TIMEDOUT';
        }

        return css.toUpperCase();
    });
    self.isJob = ko.computed(function () {
        var id = self.jobId();
        var deleted=self.jobDeleted();
        return id || deleted;
    });
    self.isAdhoc = ko.computed(function () {
        return !self.isJob();
    });

    self.jobPercentage = ko.computed(function () {
        if (self.jobAverageDuration() > 0) {
            return 100 * (self.duration() / self.jobAverageDuration());
        } else {
            return -1;
        }
    });
    self.jobPercentageFixed = ko.computed(function () {
        var pct = self.jobPercentage();
        if (pct >= 0) {
            return pct.toFixed(0)
        } else {
            return '0';
        }
    });
    self.jobOverrunDuration = ko.computed(function () {
        var jobAverageDuration = self.jobAverageDuration();
        var execDuration = self.duration();
        if (jobAverageDuration > 0 && execDuration > jobAverageDuration) {
            return MomentUtil.formatDurationSimple(execDuration - jobAverageDuration);
        } else {
            return '';
        }
    });
    self.endTimeFormat = function (format) {
        var value = self.dateCompleted();
        return MomentUtil.formatTime(value, format);
    };
    self.nodeFailCount = ko.computed(function () {
        var ncount = ko.utils.unwrapObservable(self.node);
        if(ncount){
            var ns = ncount.split('/');
            if (ns.length == 3) {
                return parseInt(ns[1]);
            }
        }
    });

    self.nodeSucceedCount = ko.computed(function () {
        var ncount = ko.utils.unwrapObservable(self.node);
        if(ncount){

        var ns = ncount.split('/');
        if (ns.length == 3) {
            return parseInt(ns[0]);
        }
        }
    });

    self.textJobRef = function (schedId) {
        if(self.jobId() != schedId){
            return '(Referenced)'
        }
    };
    ko.mapping.fromJS(data, {}, self);
}
function History(ajaxHistoryLink,ajaxNowRunningLink,ajaxBulkDeleteLink) {
    var self = this;
    self.ajaxHistoryLink = ajaxHistoryLink;
    self.ajaxNowRunningLink = ajaxNowRunningLink;
    self.ajaxBulkDeleteLink = ajaxBulkDeleteLink? ajaxBulkDeleteLink: typeof(appLinks)=='object'? appLinks.apiExecutionsBulkDelete:null;
    self.reports = ko.observableArray([]);
    self.nowrunning = ko.observableArray([]);
    self.showReports=ko.observable(false);
    self.nowRunningEnabled=ko.observable(true);
    self.href = ko.observable();
    self.selected = ko.observable(false);
    self.max = ko.observable(20);
    self.total = ko.observable(0);
    self.offset = ko.observable(0);
    self.params = ko.observable();
    self.reloadInterval=ko.observable(0);
    self.highlightExecutionId=ko.observable();
    //bulk edit mode
    self.bulkEditMode=ko.observable(false);
    self.bulkEditPrevious=ko.observable(-1);
    self.bulkEditResults=ko.observable();
    self.bulkEditProgress=ko.observable(false);
    self.selectedIds=[];
    self.selectedIdsPreviousPage=[];
    self.changingPage=false;
    self.results=ko.computed(function(){
       if(self.showReports()){
           return self.reports()
       } else{
           return self.nowrunning()
       }
    });
    self.count = ko.computed(function () {
        return self.reports().length + self.offset() * self.max();
    });
    self.pageCount=ko.computed(function(){
        return totalPageCount(self.max(),self.total());
    });
    self.pages=ko.computed(function(){
        var total = self.total();
        var offset = self.offset();
        var max = self.max();
        var href = self.href();
        if (total < 1 || !href) {
            return '';
        }
        var pages = [];
        //remove offset/max params from href
        href=href.replace(/[&\?](offset|max)=\d+/ig,'');

        foreachPage(offset, max, total, {maxSteps:10}, function (pg) {
            var a;
            var url = _genUrl(href, {offset: pg.offset, max: pg.max});
            var label = pg.prevPage ? 'Previous' : pg.nextPage ? 'Next' : pg.skipped? '…' : (pg.page);
            pages.push(ko.utils.extend({url:url,label:label},pg));
        });

        return pages;
    });
    self.bulkSelectedIds=ko.computed(function(){
        var ids =  [];
        ko.utils.arrayPushAll(ids, self.selectedIds);
        ko.utils.arrayForEach(self.reports(),function(el){
            if(el.bulkEditSelected()){
                ko.utils.addOrRemoveItem(self.selectedIds, el.executionId(), el.bulkEditSelected());
               ko.utils.addOrRemoveItem(ids, el.executionId(), el.bulkEditSelected());
            }
        });

        return ids;

    });
    //array of selected ids for bulk edit
    self.bulkEditIds=ko.computed(function(){
        
       var ids=[];
        ko.utils.arrayForEach(self.reports(),function(el){
            if(el.bulkEditSelected()){
               ids.push(el.executionId());
           }
        });
        return ids;
    });
    self.visitPage=function(page){
        loadHistoryLink(self,self.ajaxHistoryLink,page.url, null, true);
    };
    self.activateNowRunningTab=function() {
        jQuery('ul.activity_links > li:first-child').addClass('active');
        jQuery('ul.activity_links > li:first-child > a').each(function (e) {
            loadHistoryLink(self, self.ajaxNowRunningLink, this.getAttribute('href'), jQuery(this).data('auto-refresh'));
        });
    };

    self.toggleBulkEdit=function(){
        self.selectedIds = [];
        self.bulkEditMode(!self.bulkEditMode());
    };

    //report was clicked
    self.rowClicked=function(report, multiselect){
        if (!self.bulkEditMode()) {
            document.location=report.executionHref();
        } else {
            //select default input checkbox
            //if shift key held down
            var index = ko.utils.arrayIndexOf(self.reports(), report);
            if(multiselect && self.bulkEditPrevious()>=0){
                var prevVal = self.bulkEditPrevious();
                var a = prevVal < index ? prevVal : index;
                var b = prevVal < index ? index : prevVal;
                var c;
                var prevSel=report.bulkEditSelected();

                //traverse reports
                for(c=b;c>=a && c>=0;c--){
                    var x=self.reports()[c];
                    x.bulkEditSelected(!prevSel);
                    
                    ko.utils.addOrRemoveItem(self.selectedIds, x.executionId(), !prevSel)
                }
            }else{
                report.bulkEditSelected(!report.bulkEditSelected());
                
                ko.utils.addOrRemoveItem(self.selectedIds, report.executionId(), report.bulkEditSelected());
                
            }
            self.bulkEditPrevious(index);
        }
    };
    self.bulkEditToggleAll=function(){
        ko.utils.arrayForEach(self.reports(),function(e){
            e.bulkEditSelected(!e.bulkEditSelected());
        });
    };
    self.bulkEditSelectAll=function(){
        ko.utils.arrayForEach(self.reports(),function(e){
            if(!self.selectedIds) self.selectedIds = [];
            ko.utils.addOrRemoveItem(self.selectedIds, e.executionId(), true);
            e.bulkEditSelected(true);
        });
    };
    self.bulkEditDeselectAllPages=function(){
        self.selectedIds = [];
        self.bulkEditDeselectAll();
    };
    self.bulkEditDeselectAll=function(){
        jQuery("#cleanselections").modal('hide');
        ko.utils.arrayForEach(self.reports(),function(e){
            if(self.selectedIds){
                ko.utils.addOrRemoveItem(self.selectedIds, e.executionId(), false);
            }
            e.bulkEditSelected(false);
        });
    };

    //bulk delete invoked
    self.doBulkDelete=function(modal,resultmodal){
        jQuery(modal).modal('hide');
        self.bulkEditProgress(true);
        jQuery.ajax({
            url: self.ajaxBulkDeleteLink,
            type:'post',
            data: JSON.stringify({"ids": self.bulkSelectedIds()}),
            contentType: 'application/json',
            dataType:'json',
            beforeSend:_createAjaxSendTokensHandler('history_tokens'),
            success:function(data,status,xhr){
                self.selectedIds = [];
                self.bulkEditProgress(false);
                self.bulkEditResults(data);
                if(data.allsuccessful){
                    self.bulkEditMode(false);
                }else{
                    jQuery(resultmodal).modal('show');
                }
                self.reloadData();
            },
            error:function(xhr,status,err){
                self.selectedIds = [];
                self.bulkEditProgress(false);
                self.bulkEditResults({error:"Request did not succeed: "+((xhr.responseJSON && xhr.responseJSON.message)? xhr.responseJSON.message:err)});
                jQuery(resultmodal).modal('show');
            }
        }).success(_createAjaxReceiveTokensHandler('history_tokens'));
    };

    //load dataset again
    self.reloadData=function(){
        loadHistoryLink(self,self.ajaxHistoryLink,self.href());
    }
}

var binding = {
    'reports': {
        key: function (data) {
            return ko.utils.unwrapObservable(data.id);
        },
        create: function (options) {
            return new Report(options.data);
        }
    },
    'nowrunning': {
        key: function (data) {
            return ko.utils.unwrapObservable(data.id);
        },
        create: function (options) {
            return new Report(jQuery.extend({execution: options.data},options.data));
        }
    }
};
function loadHistoryLink(history, ajaxBaseUrl, href,reload, keepSelections) {
    var params = href.substring(href.indexOf('?')+1);
    var url = ajaxBaseUrl.indexOf("?")>0? ajaxBaseUrl+'&' + params : ajaxBaseUrl+'?' + params;

    var handleResult;
    
    
    if(keepSelections){
        if(!history.selectedIds) {
            history.selectedIds = [];
        }
        ko.utils.arrayForEach(history.reports(),function(el){
            if(el.bulkEditSelected()){
                ko.utils.addOrRemoveItem(history.selectedIds, el.executionId(), true)
            }
        });
    }
    var load=function(){
        history.href(href);
        jQuery.getJSON(url, handleResult);
    };
    handleResult= function (data) {
        history.selected(true);
        ko.mapping.fromJS(jQuery.extend(data, { params: params }), binding, history);
        setTimeout(function(){
            if (reload && history.href() == href) {
                load();
            }
        }, reload * 1000);
        
        
        if(keepSelections){
            ko.utils.arrayForEach(history.reports(),function(el){
                if(ko.utils.arrayIndexOf(history.selectedIds, el.executionId()) > -1){
                    el.bulkEditSelected(true);
                }
            });
        }
    };
    
    
    load();
}

function setupActivityLinks(id, history) {
    var activitysection=jQuery('#' + id);
    activitysection.on('click','a.activity_link',function (e) {
        e.preventDefault();
        var me = jQuery(this)[0];
        jQuery('#' + id + ' .activity_links > li').removeClass('active');
        jQuery(me.parentNode).addClass('active');
        history.showReports(true);
        loadHistoryLink(history, history.ajaxHistoryLink, me.getAttribute('href'),jQuery(this).data('auto-refresh'));
    });
    activitysection.on('click','a.running_link',function (e) {
        e.preventDefault();
        var me = jQuery(this)[0];
        if(history.nowRunningEnabled()){
            jQuery('#' + id + ' .activity_links > li').removeClass('active');
            jQuery(me.parentNode).addClass('active');
            history.showReports(false);

            loadHistoryLink(history, history.ajaxNowRunningLink, me.getAttribute('href'), jQuery(this).data('auto-refresh'));
        }
    });

    //click handler for rows, and prevent shift-click selection in bulk edit mode
    activitysection.on('click', '.autoclick .autoclickable',function (e) {
        e.preventDefault();
        history.rowClicked(ko.dataFor(this), e.shiftKey);
        return false;
    }).on('mousedown', '.autoclick .autoclickable', function (e) {
        if (history.bulkEditMode()) {
            //prevent text selection for shift click
            e.preventDefault();
        }
    });
}
