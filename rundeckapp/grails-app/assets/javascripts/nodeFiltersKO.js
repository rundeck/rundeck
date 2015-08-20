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
var NODE_FILTER_ALL='.*';
function NodeSummary(data){
    var self=this;
    self.error=ko.observable();
    self.tags=ko.observableArray();
    self.filters=ko.observableArray();
    self.defaultFilter=ko.observable();
    self.totalCount=ko.observable(0);
    self.baseUrl=data.baseUrl?data.baseUrl:'';
    self.filterToDelete=ko.observable();
    
    self.reload=function(){
      jQuery.ajax({
          url:_genUrl(appLinks.frameworkNodeSummaryAjax),
          type:'GET',

          error:function(data,jqxhr,err){
              self.error('Recent commands list: request failed for '+requrl+': '+err+", "+jqxhr);
          }
      }).success(function(data){
          ko.mapping.fromJS(data,{},self);
      });
    };
    self.linkForTagFilter=function(tag){
        return _genUrl(self.baseUrl,{filter: 'tags:'+tag.tag()});
    };
    self.linkForFilterName=function(filter){
        return _genUrl(self.baseUrl,{filterName: filter.name()});
    };
    /**
     * Generate URL for the NodeFilters object
     * @param nodefilters
     * @returns {*}
     */
    self.linkForNodeFilters=function(nodefilters){
        return _genUrl(self.baseUrl,nodefilters.getPageParams());
    };
    self.findFilterByName=function(name){
        var found=ko.utils.arrayFilter(self.filters(),function(e){return e.name()==name;});
        if(found && found.length==1){
            return found[0];
        }else{
            return null;
        }
    };
    self.removeDefault=function(){
        setFilter('nodes','!').success(function(data, status, jqxhr){
            self.defaultFilter(null);
        });
    };
    self.setDefaultAll=function(){
        self.setDefault(NODE_FILTER_ALL);
    };

    self.setDefault=function(filter){
        var fname=null;
        if(typeof(filter)=='string'){
            if(filter==NODE_FILTER_ALL){
                fname=filter;
            }else {
                filter = self.findFilterByName(filter);
                if (!filter) {
                    return;
                }
                fname = filter.name();
            }
        }else{
            fname=filter.name();
        }
        setFilter('nodes',fname).success(function(data, status, jqxhr){
            self.defaultFilter(fname);
        });
    };
    self.deleteFilterConfirm=function(filter){
        if(typeof(filter)=='string'){
            filter = self.findFilterByName(filter);
            if(!filter){
                return;
            }
        }
        self.filterToDelete(filter);
        jQuery('#deleteFilterKOModal').modal('show');
    };
    self.deleteFilter=function(filter){

        jQuery('#deleteFilterKOModal').modal('hide');
        jQuery.ajax({
            url:_genUrl(appLinks.frameworkDeleteNodeFilterAjax,{filtername:filter.name()}),
            beforeSend: _ajaxSendTokens.curry('ajaxDeleteFilterTokens')
        }).success(function (resp, status, jqxhr) {
            self.filterToDelete(null);
            self.filters.remove(filter);
        }).success(_ajaxReceiveTokens.curry('ajaxDeleteFilterTokens'));
    };
    if(data) {
        ko.mapping.fromJS(data, {}, self);
    }
}

function NodeFilters(baseRunUrl, baseSaveJobUrl, baseNodesPageUrl, data) {
    var self = this;
    self.baseRunUrl = baseRunUrl;
    self.baseSaveJobUrl = baseSaveJobUrl;
    self.baseNodesPageUrl = baseNodesPageUrl;
    self.filterName = ko.observable(data.filterName);
    self.filter = ko.observable(data.filter);
    self.filterAll = ko.observable(data.filterAll);
    self.nodeExcludePrecedence = ko.observable(null== data.nodeExcludePrecedence || data.nodeExcludePrecedence?'true':'false');
    self.nodefilterLinkId=data.nodefilterLinkId;
    self.total = ko.observable(0);
    self.allcount = ko.observable(0);
    self.loading=ko.observable(false);
    self.error=ko.observable(null);
    self.project=ko.observable(data.project);
    self.page=ko.observable(data.page?data.page:0);
    self.pagingMax=ko.observable(data.pagingMax?data.pagingMax:20);
    self.paging=ko.observable(data.paging != null ? (data.paging ? true : false) : false);
    self.maxShown=ko.observable(data.maxShown);
    self.elem=ko.observable(data.elem);
    self.tableElem=ko.observable(data.tableElem?data.tableElem:'nodesTable');
    self.pagingElem=ko.observable(data.pagingElem);
    self.view=ko.observable(data.view);
    self.emptyMode=ko.observable(data.emptyMode?data.emptyMode:'localnode');
    self.emptyMessage=ko.observable(data.emptyMessage?data.emptyMessage:'No match');
    self.hideAll=ko.observable(data.hideAll!=null?(data.hideAll?true:false):false);
    self.nodeSummary=ko.observable(data.nodeSummary?data.nodeSummary:null);

    self.isFilterNameAll=ko.pureComputed(function(){
        return self.filterName()==NODE_FILTER_ALL;
    });
    self.filterNameDisplay=ko.pureComputed(function(){
       return self.isFilterNameAll()?'All Nodes':self.filterName();
    });
    self.canSaveFilter=ko.pureComputed(function(){
       return !self.filterName() && self.filterWithoutAll();
    });
    self.canDeleteFilter=ko.pureComputed(function(){
       return self.filterName() && !self.isFilterNameAll();
    });
    self.canSetDefaultFilter=ko.pureComputed(function(){
       return self.filterName() && self.filterName()!=self.nodeSummary().defaultFilter();
    });
    self.canRemoveDefaultFilter=ko.pureComputed(function(){
       return self.filterName() && self.filterName()==self.nodeSummary().defaultFilter();
    });
    self.deleteFilter=function(){
        self.nodeSummary().deleteFilterConfirm(self.filterName());
    };
    self.setDefaultFilter=function(){
        self.nodeSummary().setDefault(self.filterName());
    };
    self.pageRemaining=ko.computed(function(){
        if(self.total()<=0 || self.page()<0){
            return 0;
        }
        return self.allcount()-(self.page()+1)*self.pagingMax();
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
    self.filterWithoutAll = ko.computed({
        read: function () {
            if (self.filterAll() && self.filter() == NODE_FILTER_ALL && self.hideAll()) {
                return '';
            }
            return self.filter();
        },
        write: function (value) {
            self.filter(value);
        },
        owner: this
    });
    self.filterIsSet=ko.pureComputed(function(){
        return !!self.filterWithoutAll() || !!self.filterName();
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
        self.allcount(-1);
    };
    /**
     * clear filters and results count
     */
    self.reset=function(){
        self.clear();
        self.filterAll(false);
        self.filterWithoutAll(null);
        self.filterName(null);
    };
    /**
     * Use a specific filter string and update
     * @param filter the filter string
     */
    self.useFilterString=function(filter){
        self.filterAll(false);
        self.filterWithoutAll(filter);
        self.filterName(null);
        self.clear();
        self.updateMatchedNodes();
    };
    /**
     * Generate state object for the current filter
     * @returns {{filter: (*|string), filterName: (*|string), filterAll: (*|string)}}
     */
    self.getPageParams=function(){
        return {
            filter: self.filter()||'',
            filterName: self.filterName()||'',
            filterAll: self.filterAll()||''
        };
    };
    /**
     * generate URL for the current filters
     */
    self.getPageUrl=function(){
        return self.nodeSummary().linkForNodeFilters(self);
    };
    /**
     * Update to match state parameters
     * @param data
     */
    self.setPageParams=function(data){
        self.filterAll(data.filterAll);
        self.filter(data.filter);
        self.filterName(data.filterName);
        self.clear();
        self.updateMatchedNodes();
    };
    self.selectNodeFilterLink=function(link,isappend){
        var oldfilter = self.filter();
        var filterName = jQuery(link).data('node-filter-name');
        var filterString = jQuery(link).data('node-filter');
        var filterTag = jQuery(link).data('node-tag');
        if(filterString && filterString.indexOf("&")>=0){
            filterString = html_unescape(filterString);
        }
        var filterAll = jQuery(link).data('node-filter-all');
        var v=oldfilter?oldfilter.indexOf('tags: '):-1;
        if(isappend && filterTag && v>=0){
            var first=oldfilter.substring(0, v);
            var rest=oldfilter.substring(v + 6);
            var last='';
            while(rest.indexOf(" ")==0){
                rest = rest.substring(1);
            }
            v = rest.indexOf(" ");
            if(v>0){
                last = rest.substring(v);
                rest = rest.substring(0,v);
            }
            filterString = first + 'tags: ' + rest + '+' + filterTag + last ;
        }else if (filterString && !filterName && oldfilter && !filterAll && oldfilter != NODE_FILTER_ALL) {
            filterString = oldfilter + ' ' + filterString;
        } else if (filterAll) {
            filterString = NODE_FILTER_ALL;
        }
        self.filterAll(filterAll);
        self.filter(filterString);
        self.filterName(filterName);
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
        if(!self.filter()){
            return;
        }
        var project=self.project();
        if (!project) {
            return;
        }
        var filterdata = self.filterName() ? {filterName: self.filterName()} : self.filter()?{filter: self.filter()}:{};
        var page = self.page();
        var loadTarget = '#'+self.elem();
        var needsBinding = true;
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
