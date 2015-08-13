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
function AdhocHistory(data,nodefilter) {
    var self = this;
    self.nodefilter=nodefilter;
    self.loadMax=20;
    self.loaded=ko.observable(false);
    self.links=ko.observableArray([]);
    var mapping = {
        'links': {
            create: function (options) {
                return new AdhocLink(options.data,self.nodefilter);
            }
        }
    };
    self.noneFound=ko.pureComputed(function(){
        return self.links().length<1 && self.loaded();
    });
    self.reload=function(){
        var requrl=_genUrl(appLinks.adhocHistoryAjax,{max:self.loadMax});
        jQuery.ajax({
            type:'GET',
            url:requrl,
            success:function (data,status,xhr) {
                self.loaded(true);
                try {
                    ko.mapping.fromJS(data,mapping,self);
                } catch (e) {
                    console.log('Recent commands list: error receiving data',e);
                    runError('Recent commands list: error receiving data: '+e);
                }
            },
            error:function(data,jqxhr,err){
                runError('Recent commands list: request failed for '+requrl+': '+err+", "+jqxhr);
            }
        })
    };
    if(data){
        ko.mapping.fromJS(data,{},self);
    }
}