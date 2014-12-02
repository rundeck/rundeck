//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter
/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

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

function NodeFilters(baseRunUrl, baseSaveJobUrl, baseNodesPageUrl, data) {
    var self = this;
    self.baseRunUrl = baseRunUrl;
    self.baseSaveJobUrl = baseSaveJobUrl;
    self.baseNodesPageUrl = baseNodesPageUrl;
    self.filterName = ko.observable(data.filterName);
    self.filter = ko.observable(data.filter);
    self.nodeExcludePrecedence = ko.observable(null== data.nodeExcludePrecedence || data.nodeExcludePrecedence?'true':'false');
    self.nodefilterLinkId=data.nodefilterLinkId;
    self.total = ko.observable(0);
    self.allcount = ko.observable(0);
    self.loading=ko.observable(false);
    self.error=ko.observable(null);
    self.project=ko.observable(data.project);
    self.page=ko.observable(data.page?data.page:0);
    self.pagingMax=ko.observable(data.pagingMax?data.pagingMax:20);
    self.paging=ko.observable(data.paging != null ? (data.paging ? true : false) : false)
    self.maxShown=ko.observable(data.maxShown)
    self.elem=ko.observable(data.elem);
    self.tableElem=ko.observable(data.tableElem?data.tableElem:'nodesTable');
    self.pagingElem=ko.observable(data.pagingElem);
    self.view=ko.observable(data.view);
    self.emptyMode=ko.observable(data.emptyMode?data.emptyMode:'localnode');
    self.emptyMessage=ko.observable(data.emptyMessage?data.emptyMessage:'No match');
    self.hideAll=ko.observable(data.hideAll!=null?(data.hideAll?true:false):false);

    self.pageRemaining=ko.computed(function(){
        if(self.total()<=0 || self.page()<0){
            return 0;
        }
        return self.total()-(self.page()+1)*self.pagingMax();
    });
    self.hasMoreNodes=ko.computed(function(){
        return self.pageRemaining()>0;
    });
    self.hasMultiplePages=ko.computed(function(){
        return self.pageRemaining()>self.pagingMax();
    });
    self.nodesTitle = ko.computed(function () {
        return self.allcount() == 1 ?
            data.nodesTitleSingular || 'Node' :
            data.nodesTitlePlural || 'Nodes';
    });
    self.filterAll = ko.observable(data.filterAll);
    self.filterWithoutAll = ko.computed({
        read: function () {
            if (self.filterAll() && self.filter() == '.*' && self.hideAll()) {
                return '';
            }
            return self.filter();
        },
        write: function (value) {
            self.filter(value);
        },
        owner: this
    });
    self.filter.subscribe(function (newValue) {
        if (newValue == '' && self.hideAll()) {
            self.filterAll(true);
        }
    });
    self.filter.subscribe(function (newValue) {
        if (newValue == '' && self.emptyMode() == 'blank') {
            jQuery('#'+self.elem()).empty();
            self.clear();
        }
    });
    self.filter.subscribe(function (newValue) {
        if (newValue != '' && self.filterName()) {
            self.filterName(null);
        }
    });
    self.nodeExcludePrecedence.subscribe(function(newValue){
        self.updateMatchedNodes();
    });
    self.hasNodes = ko.computed(function () {
        return 0 != self.allcount();
    });
    self.runCommand = function () {
        document.location = _genUrl(self.baseRunUrl, {
            filter: self.filter(),
            filterName: self.filterName() ? self.filterName() : ''
        });
    };
    self.saveJob = function () {
        document.location = _genUrl(self.baseSaveJobUrl, {
            filter: self.filter(),
            filterName: self.filterName() ? self.filterName() : ''
        });
    };
    self.nodesPageView=function(){
        document.location = _genUrl(self.baseNodesPageUrl, {
            filter: self.filter(),
            filterName: self.filterName()? self.filterName():''
        });
    };
    self.clear=function(){
        self.page(0);
        self.total(0);
        self.allcount(0);
    };
    self.selectNodeFilterLink=function(link){
        var oldfilter = self.filter();
        var filterName = jQuery(link).data('node-filter-name');
        var filterString = jQuery(link).data('node-filter');
        if(filterString.indexOf("&")>=0){
            filterString = html_unescape(filterString);
        }
        var filterAll = jQuery(link).data('node-filter-all');
        if (filterString && !filterName && oldfilter && !filterAll && oldfilter != '.*') {
            filterString = oldfilter + ' ' + filterString;
        } else if (filterAll) {
            filterString = '.*'
        }
        self.filterAll(filterAll);
        self.filterName(filterName);
        self.filter(filterString);
        self.clear();
        self.updateMatchedNodes();
    };
    self.updateNodesNextPage=function(){
        if(!self.page()){
            self.page(0);
        }
        self.page(self.page()+1);
        self.updateMatchedNodes();
    };
    self.updateNodesRemainingPages=function(){
        self.page(-1);
        self.updateMatchedNodes();
    };
    self.updateMatchedNodes= function () {
        var page = self.page();
        var loadTarget = '#'+self.elem();
        var needsBinding = true;
        var project=self.project();
        var view = self.view() ? self.view() : 'table';
        var basedata = {view: view, declarenone: true, fullresults: true, expanddetail: true, inlinepaging: false, nodefilterLinkId: self.nodefilterLinkId};
        var clearContent=true;
        if(self.paging()){
            basedata.page = page;
            basedata.max = self.pagingMax();
            basedata.inlinepaging=true;
            if (page != 0) {
                clearContent=false;
                var tbody = document.createElement('tbody');
                jQuery('#'+ self.tableElem()).append(tbody);
                loadTarget=tbody;
                needsBinding = true;
                basedata.view= 'tableContent';
            }
        }
        if(self.maxShown()){
            basedata.maxShown=self.maxShown();
        }
        if(clearContent){
            var div = document.createElement('div');
            jQuery('#' + self.elem()).empty().append(div);
            loadTarget = div;
        }
        var filterdata = self.filterName() ? {filterName: self.filterName()} : self.filter()?{filter: self.filter()}:{};
        var i;
        if (!project) {
            return;
        }
        var params = Object.extend(basedata, filterdata);
        if(self.emptyMode()=='localnode' && !self.filter()){
            params.localNodeOnly = 'true';
        }else if(self.emptyMode()=='blank' && !self.filter()){
            jQuery(loadTarget).empty();
            self.clear();
            return;
        }
        var exclude=self.nodeExcludePrecedence();
        if (typeof(exclude) == 'string' && exclude === "false"
            || typeof(exclude) == 'boolean' && !exclude) {
            params.nodeExcludePrecedence = "false";
        } else {
            params.nodeExcludePrecedence = "true";
        }
        self.loading(true);
        jQuery(loadTarget).load(
            _genUrl(appLinks.frameworkNodesFragment, params),
            function (response, status, xhr) {
                self.loading(false);
                if (status == 'success') {
                    if(needsBinding){
                        ko.applyBindings(self,jQuery(loadTarget)[0]);
                    }
                    var headerVal = xhr.getResponseHeader('X-rundeck-data-id');
                    if(headerVal){
                        var values= headerVal.split(', ');
                        ko.utils.arrayForEach(values,function(dataId){
                            var data = loadJsonData(dataId);
                            if(data && data.name=='nodes' && data.content){
                                ko.mapping.fromJS({allcount:data.content.allcount,total:data.content.total},{},self);
                            }
                        })
                    }
                } else if (typeof(errcallback) == 'function') {
                    if (xhr.getResponseHeader("X-Rundeck-Error-Message")) {
                        self.error(xhr.getResponseHeader("X-Rundeck-Error-Message"));
                    } else {
                        self.error(xhr.statusText);
                    }
                }
        });
    };
}
