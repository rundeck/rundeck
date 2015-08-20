//= require knockout.min
//= require knockout-mapping
/*
 Copyright 2015 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
function AdhocLink(data,nodefilter) {
    var self = this;
    self.nodefilter=nodefilter;
    self.href=ko.observable(null);
    self.title=ko.observable(null);
    self.execid=ko.observable(null);
    self.filter=ko.observable(null);

    self.status=ko.observable(null);
    self.succeeded=ko.observable(null);

    var statusMap={
        'running':'running',
        'succeed':'succeed',
        'succeeded':'succeed',
        'fail':'fail',
        'failed':'fail',
        'cancel':'aborted',
        'aborted':'aborted',
        'retry':'failedretry',
        'timedout':'timedout',
        'timeout':'timedout'
    };

    self.statusClass=ko.pureComputed(function(){
        var css=statusMap[self.status()];
        return css?css:'other';
    });

    //set the command and filter strings
    self.fillCommand=function(){
        jQuery('#runFormExec').val(self.title());
        self.nodefilter.useFilterString(self.filter());
    };

    ko.mapping.fromJS(data,{},self);
}
function AdhocCommand(data,nodefilter) {
    var self = this;
    self.nodefilter=nodefilter;
    self.loadMax=20;
    self.recentCommandsLoaded=ko.observable(false);
    self.recentCommands=ko.observableArray([]);
    self.commandString=ko.observable();
    self.commandStringDelayed = ko.pureComputed(this.commandString)
        .extend({ rateLimit: { method: "notifyWhenChangesStop", timeout: 1000 } });
    self.running=ko.observable(false);
    self.canRun=ko.observable(false);
    self.allowInput = ko.pureComputed(function(){
       return !self.running() && self.canRun();
    });
    self.followControl=null;
    var mapping = {
        'recentCommands': {
            create: function (options) {
                return new AdhocLink(options.data,self.nodefilter);
            }
        }
    };
    self.recentCommandsNoneFound=ko.pureComputed(function(){
        return self.recentCommands().length<1 && self.recentCommandsLoaded();
    });
    self.loadList=function(params){
        var requrl=_genUrl(appLinks.adhocHistoryAjax,jQuery.extend({max:self.loadMax},params));
        return jQuery.ajax({
            type:'GET',
            url:requrl,

            error:function(data,jqxhr,err){
                runError('Recent commands list: request failed for '+requrl+': '+err+", "+jqxhr);
            }
        });
    };
    self.loadRecentCommands=function(){
        self.loadList({}).done(function (data,status,xhr) {
            self.recentCommandsLoaded(true);
            try {
                ko.mapping.fromJS({recentCommands:data.executions},mapping,self);
            } catch (e) {
                console.log('Recent commands list: error receiving data',e);
                runError('Recent commands list: error receiving data: '+e);
            }
        });
    };
    //self.commandStringDelayed.subscribe(function(newval){
    //    if(newval && newval.length>3) {
    //        self.loadList({query: newval}).done(function (data, status, xhr) {
    //            console.log('new query: ' + newval, data);
    //        });
    //    }
    //});
    if(data){
        ko.mapping.fromJS(data,{},self);
    }
}