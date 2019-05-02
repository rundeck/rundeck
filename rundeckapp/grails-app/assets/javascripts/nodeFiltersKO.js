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

//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter
//= require knockout-foreachprop
//= require knockout-node-filter-link
//= require ko/binding-popover
//= require ko/binding-message-template

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
            beforeSend: _createAjaxSendTokensHandler('ajaxDeleteFilterTokens')
        }).success(function (resp, status, jqxhr) {
            self.filterToDelete(null);
            self.filters.remove(filter);
        }).success(_createAjaxReceiveTokensHandler('ajaxDeleteFilterTokens'));
    };
    if(data) {
        ko.mapping.fromJS(data, {}, self);
    }
}
var CSSColors='aliceblue antiquewhite aqua aquamarine azure beige bisque black blanchedalmond blue blueviolet brown burlywood cadetblue chartreuse chocolate coral cornflowerblue cornsilk crimson cyan darkblue darkcyan darkgoldenrod darkgray darkgreen darkkhaki darkmagenta darkolivegreen darkorange darkorchid darkred darksalmon darkseagreen darkslateblue darkslategray darkturquoise darkviolet deeppink deepskyblue dimgray dodgerblue firebrick floralwhite forestgreen fuchsia gainsboro ghostwhite gold goldenrod gray green greenyellow honeydew hotpink indianred indigo ivory khaki lavender lavenderblush lawngreen lemonchiffon lightblue lightcoral lightcyan lightgoldenrodyellow lightgreen lightgrey lightpink lightsalmon lightseagreen lightskyblue lightslategray lightsteelblue lightyellow lime limegreen linen magenta maroon mediumaquamarine mediumblue mediumorchid mediumpurple mediumseagreen mediumslateblue mediumspringgreen mediumturquoise mediumvioletred midnightblue mintcream mistyrose moccasin navajowhite navy oldlace olive olivedrab orange orangered orchid palegoldenrod palegreen paleturquoise palevioletred papayawhip peachpuff peru pink plum powderblue purple red rosybrown royalblue saddlebrown salmon sandybrown seagreen seashell sienna silver skyblue slateblue slategray snow springgreen steelblue tan teal thistle tomato turquoise violet wheat white whitesmoke yellow yellowgreen'.split(' ');
function NodeEntry(data){
    var self=this;

    if(data) {
        ko.mapping.fromJS(data, {}, self);
    }
}
function TagSummary(data){
    var self=this;
    self.tag=ko.observable(data.tag);
    self.value=ko.observable(data.value);
    if(data){
        ko.mapping.fromJS(data, {}, self);
    }
}
/**
 * The set of node results
 * @param data
 * @constructor
 */
function NodeSet(data) {
    var self = this;
    self.nodes=ko.observableArray([]);
    self.tagsummary=ko.observableArray([]);

    var mapping = {

        'nodes': {
            //nb: don't use a key: function because in some cases
            //we need a new dom element every time we redraw nodes,
            //and a key function will cause re-use of the element

            create: function (options) {
                return new NodeEntry(options.data);
            }
        }
        ,
        'tagsummary': {
            key: function (data) {
                return ko.utils.unwrapObservable(data.tag);
            },
            create: function (options) {
                return new TagSummary(options.data);
            }
        }
    };

    self.glyphiconCss=function(name){
        if(name.match(/^glyphicon-[a-z-]+$/)){
            return 'glyphicon '+name;
        }else if(name.match(/^fa-[a-z-]+$/)){
            return 'fas '+name;
        }else if(name.match(/^fab-[a-z-]+$/)){
            return 'fab fa-'+name.substring(4);
        }
        return '';
    };
    self.glyphiconBadges=function(attributes){
        var badges=[];
        if(attributes['ui:badges']){
            var found=attributes['ui:badges']().split(/,\s*/g);
            for(var i=0;i<found.length;i++){
                if(found[i].match(/^glyphicon-[a-z-]+$/)){
                    badges.push(found[i]);
                }else if(found[i].match(/^fa-[a-z-]+$/)){
                    badges.push(found[i]);
                }else if(found[i].match(/^fab-[a-z-]+$/)){
                    badges.push(found[i]);
                }
            }
        }

        return badges;
    };
    self.isAnsiFg=function(str){
        return str!=null && typeof(str)=='string' && str.match(/^ansi-fg-(light-)?(black|green|red|yellow|blue|magenta|cyan|white)$/);
    };
    self.isStyleFg=function(str){
        return str!=null && typeof(str)=='string' && str.match(/^#[0-9a-fA-F]{3,6}$/) || CSSColors.indexOf(str)>=0;
    };
    self.isAnsiBg=function(str){
        return str!=null && typeof(str)=='string' && str.match(/^ansi-bg-(black|green|red|yellow|blue|magenta|cyan|white|default)$/);
    };
    self.isStyleBg=function(str){
        return str!=null && typeof(str)=='string' && str.match(/^#[0-9a-fA-F]{3,6}$/)|| CSSColors.indexOf(str)>=0;
    };
    self.iconFgCss=function(attrs,attrName){
        var uiIconColor = attrs[attrName]?attrs[attrName]():null;
        var uiColor = attrs['ui:color']?attrs['ui:color']():null;
        if(self.isAnsiFg(uiIconColor)){
            return uiIconColor;
        }else if(self.isAnsiFg(uiColor)){
            return uiColor;
        }
        return null;
    };
    self.iconBgCss=function(attrs,attrName){
        var uiIconBgcolor = attrs['ui:icon:bgcolor']?attrs['ui:icon:bgcolor']():null;
        var uiBgcolor = attrs['ui:bgcolor']?attrs['ui:bgcolor']():null;
        if(self.isAnsiBg(uiIconBgcolor)){
            return uiIconBgcolor;
        }else if(self.isAnsiBg(uiBgcolor)){
            return uiBgcolor;
        }
        return null;
    };
    self.statusIconCss=function(attrs){
        var classnames=[];
        var fgColor= self.iconFgCss(attrs,'ui:status:color');
        if(fgColor){
            classnames.push(fgColor);
        }
        var bgColor = self.iconBgCss(attrs,'ui:status:bgcolor');
        if(bgColor){
            classnames.push(bgColor);
        }
        return classnames.join(' ');
    };
    self.iconCss=function(attrs){
        var classnames=[];
        var fgColor= self.iconFgCss(attrs,'ui:icon:color');
        if(fgColor){
            classnames.push(fgColor);
        }
        var bgColor = self.iconBgCss(attrs,'ui:icon:bgcolor');
        if(bgColor){
            classnames.push(bgColor);
        }
        return classnames.join(' ');
    };
    self.nodeFgCss=function(attrs) {
        var uiColor = attrs['ui:color'] ? attrs['ui:color']() : null;
        if (self.isAnsiFg(uiColor)) {
            return uiColor;
        }
        return null
    };
    self.nodeBgCss=function(attrs) {
        var uiBgcolor = attrs['ui:bgcolor']?attrs['ui:bgcolor']():null;
        if(self.isAnsiBg(uiBgcolor)){
            return uiBgcolor;
        }
        return null
    };
    self.nodeCss=function(attrs){
        var classnames=[];
        var uiColor = self.nodeFgCss(attrs);
        if(uiColor){
            classnames.push(uiColor);
        }
        var uiBgcolor = self.nodeBgCss(attrs);
        if(uiBgcolor){
            classnames.push(uiBgcolor);
        }
        return classnames.join(' ');
    };
    self.iconStyle=function(attrs){
        var styles={};
        if(!self.iconFgCss(attrs,'ui:icon:color')) {
            var uiIconColor = attrs['ui:icon:color']?attrs['ui:icon:color']():null;
            var uiColor = attrs['ui:color']?attrs['ui:color']():null;
            if (self.isStyleFg(uiIconColor)){
                styles['color']=uiIconColor;
            }else if(self.isStyleFg(uiColor)){
                styles['color']=uiColor;
            }
        }
        if(!self.iconBgCss(attrs,'ui:icon:bgcolor')) {
            var uiIconBgcolor = attrs['ui:icon:bgcolor']?attrs['ui:icon:bgcolor']():null;
            var uiBgcolor = attrs['ui:bgcolor']?attrs['ui:bgcolor']():null;
            if (self.isStyleBg(uiIconBgcolor)){
                styles['background-color']=uiIconBgcolor;
            }else if(self.isStyleBg(uiBgcolor)){
                styles['background-color']=uiBgcolor;
            }
        }
        return styles;
    };
    self.statusIconStyle=function(attrs){
        var styles={};
        if(!self.iconFgCss(attrs,'ui:status:color')) {
            var uiIconColor = attrs['ui:status:color']?attrs['ui:status:color']():null;
            var uiColor = attrs['ui:color']?attrs['ui:color']():null;
            if (self.isStyleFg(uiIconColor)){
                styles['color']=uiIconColor;
            }else if(self.isStyleFg(uiColor)){
                styles['color']=uiColor;
            }
        }
        if(!self.iconBgCss(attrs,'ui:status:bgcolor')) {
            var uiIconBgcolor = attrs['ui:status:bgcolor']?attrs['ui:status:bgcolor']():null;
            var uiBgcolor = attrs['ui:bgcolor']?attrs['ui:bgcolor']():null;
            if (self.isStyleBg(uiIconBgcolor)){
                styles['background-color']=uiIconBgcolor;
            }else if(self.isStyleBg(uiBgcolor)){
                styles['background-color']=uiBgcolor;
            }
        }
        return styles;
    };
    self.nodeStyle=function(attrs){

        var styles={};
        var uiColor = attrs['ui:color']?attrs['ui:color']():null;
        if(!self.nodeFgCss(attrs) && self.isStyleFg(uiColor)){
            styles['color']=uiColor;
        }
        var uiBgcolor = attrs['ui:bgcolor']?attrs['ui:bgcolor']():null;
        if(!self.nodeBgCss(attrs) && self.isStyleBg(uiBgcolor)){
            styles['background-color']=uiBgcolor;
        }
        return styles;
    };
    self.startsWith = function (a, b) {
        return ( a.length >= (b.length)) && a.substring(0, b.length) == b;
    };

    self.attributesInNamespace=function(attrs,ns){
        var result=[];
        for(var e in attrs){
            if(self.startsWith(e, ns+':') && attrs[e]()){
                result.push({name:e,value:attrs[e](),shortname: e.substring(ns.length+1)});
            }
        }
        result.sort(function(a,b){return a.shortname.localeCompare(b.shortname);});
        return result;
    };
    self.attributeNamespaceRegex=/^(.+?):.+$/;
    self.attributeNamespaceNames=function(attrs){
        var namespaces=[];
        for(var e in attrs){
            var found=e.match(self.attributeNamespaceRegex);
            if(found && found.length>1){
                if(namespaces.indexOf(found[1])<0){
                    namespaces.push(found[1]);
                }
            }
        }
        namespaces.sort();
        return namespaces;
    };
    self.attributeNamespaces=function(attrs){
        var index={};
        var names=[];
        for(var e in attrs){
            var found=e.match(self.attributeNamespaceRegex);
            if(found && found.length>1){
                if(!index[found[1]]){
                    index[found[1]]=[];
                    names.push(found[1]);
                }
                index[found[1]].push({
                    name:e,
                    value:ko.utils.unwrapObservable(attrs[e]),
                    shortname: e.substring(found[1].length+1)
                });
            }
        }
        names.sort();

        var results=[];
        for(var i=0;i<names.length;i++){
            var values=index[names[i]];
            values.sort(function(a,b){return a.shortname.localeCompare(b.shortname);});
            results.push({ns:names[i],values:values});
        }

        return results;
    };
    self.OsAttributeNames='nodename hostname username description tags osFamily osName osVersion osArch'.split(' ');
    /**
     * Return an object with only attributes for display, excluding ui: namespace, and osAttrs
     *
     * @param attrs
     */
    self.displayAttributes = function (attrs) {
        var result = {};
        for(var e in attrs){
            if(e.indexOf(':')<0 && self.OsAttributeNames.indexOf(e)<0){
                result[e]=attrs[e];
            }
        }
        return result;
    };
    /**
     * Replace context variables in the string for the node details.
     * @param attrs
     * @param str
     */
    self.expandNodeAttributes=function(attrs,str){
        return str.replace(/\$\{node\.([a-zA-Z-.]+)\}/g,function(match, g1, offset, string){
            if(attrs[g1]){
                return attrs[g1]()||'';
            }else{
                return string;
            }
        });
    };
    self.convertTagSummary=function(obj){
        var arr=[];
        for(var e in obj){
            arr.push({tag:e,value:obj[e]});
        }
        return arr;
    };
    self.loadJS=function(data){
        ko.mapping.fromJS({
            nodes:data.nodes,
            tagsummary:self.convertTagSummary(data.tagsummary)
        },mapping,self);
    };
    if(data){
        self.loadJS(data);
    }
}
function NodeFilters(baseRunUrl, baseSaveJobUrl, baseNodesPageUrl, data) {
    var self = this;
    self.baseRunUrl = baseRunUrl;
    self.baseSaveJobUrl = baseSaveJobUrl;
    self.baseNodesPageUrl = baseNodesPageUrl;
    self.filterName = ko.observable(data.filterName);
    self.filterExcludeName = ko.observable(data.filterExcludeName);
    self.filter = ko.observable(data.filter);
    self.filterExclude = ko.observable(data.filterExclude);
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
    self.view=ko.observable(data.view);
    self.emptyMode=ko.observable(data.emptyMode?data.emptyMode:'localnode');
    self.emptyMessage=ko.observable(data.emptyMessage?data.emptyMessage:'No match');
    self.hideAll=ko.observable(data.hideAll!=null?(data.hideAll?true:false):false);
    self.nodeSummary=ko.observable(data.nodeSummary?data.nodeSummary:null);
    self.nodeSet=ko.observable(new NodeSet());
    self.truncated=ko.observable(false);
    self.loaded=ko.observable(false);
    self.excludeFilterUncheck = ko.observable(data.excludeFilterUncheck?'true':'false');

    /**
     *
     * can be subscribed to for browser history updating, will be changed to 'true'
     * when a browse event should be recorded, the listener should set it to false.
     */
    self.browse=ko.observable(false);
    /**
     * Names of node attributes to show as columns in table results
     */
    self.colkeys=ko.observableArray([]);

    /**
     * Display value of page number, indexed starting at 1,
     */
    this.pageDisplay = ko.computed({
        read: function () {
            return self.page()+1;
        },
        write: function (value) {
            self.page(value-1);
            self.browse(true);
            self.updateMatchedNodes();
        },
        owner: self
    });
    /**
     * Names of node attributes to show as columns removing ui: namespace
     */
    self.filterColumns=ko.pureComputed(function(){
       return ko.utils.arrayFilter(self.colkeys(),function(el){
           return !self.nodeSet().startsWith(el,'ui:');
       });
    });

    self.useDefaultColumns=ko.pureComputed(function(){
       return self.filterColumns().size()<1;
    });

    /**
     * Total column count for table view
     */
    self.totalColumnsCount=ko.pureComputed(function(){
       return self.useDefaultColumns()? 5 : 2 + self.filterColumns().size();
    });

    self.isFilterNameAll=ko.pureComputed(function(){
        return self.filterName()==NODE_FILTER_ALL;
    });
    self.isFilterExcludeNameAll=ko.pureComputed(function(){
        return self.filterExcludeName()==NODE_FILTER_ALL;
    });
    self.filterNameDisplay=ko.pureComputed(function(){
       return self.isFilterNameAll()?'All Nodes':self.filterName();
    });
    self.filterExcludeNameDisplay=ko.pureComputed(function(){
        return self.isFilterExcludeNameAll()?'All Nodes':self.filterExcludeName();
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
    self.hasPaging=ko.computed(function(){
        return self.paging() && self.total()>self.pagingMax();
    });
    /**
     * highest number of pages
     */
    self.maxPages = ko.pureComputed(function(){
        return self.paging() && Math.ceil(self.total()/self.pagingMax());
    });
    self.pageNumbersSkipped=ko.computed(function(){
        var arr=[];
        var cur = self.page();
        var maxPages = self.maxPages();
        var buffer=3;
        for(var i=0;i< maxPages;i++){
            if(i>=(cur-buffer) && i<=(cur+buffer) || i<buffer || i>=(maxPages-buffer)) {
                arr.push(i);
            }else if(i==(cur-buffer-1) || i==(cur+buffer+1)) {
                arr.push('..');
            }
        }
        return arr;
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
    self.filterExcludeWithoutAll = ko.computed({
        read: function () {
            if (self.filterExclude() == NODE_FILTER_ALL && self.hideAll()) {
                return '';
            }
            return self.filterExclude();
        },
        write: function (value) {
            self.filterExclude(value);
        },
        owner: this
    });
    self.filterIsSet=ko.pureComputed(function(){
        return !!self.filterWithoutAll() || !!self.filterName();
    });
    self.filter.subscribe(function (newValue) {
        if (newValue == '' && self.hideAll()) {
            self.filterAll(true);
        }else if(newValue==NODE_FILTER_ALL){
            self.filterAll(true);
        }
    });
    self.filter.subscribe(function (newValue) {
        if (newValue == '' && self.emptyMode() == 'blank') {
            self.clear();
        }
    });
    self.filter.subscribe(function (newValue) {
        if (newValue === NODE_FILTER_ALL) {
            self.filterName(NODE_FILTER_ALL);
        }else if (newValue != '' && self.filterName()) {
            self.filterName(null);
        }
    });
    self.filterExclude.subscribe(function (newValue) {
        if (newValue == '' && self.hideAll()) {
            self.filterAll(true);
        }else if(newValue==NODE_FILTER_ALL){
            self.filterAll(true);
        }
    });
    self.filterExclude.subscribe(function (newValue) {
        if (newValue == '' && self.emptyMode() == 'blank') {
            self.clear();
        }
    });
    self.filterExclude.subscribe(function (newValue) {
        if (newValue === NODE_FILTER_ALL) {
            self.filterExcludeName(NODE_FILTER_ALL);
        }else if (newValue != '' && self.filterExcludeName()) {
            self.filterExcludeName(null);
        }
    });
    self.nodeExcludePrecedence.subscribe(function(newValue){
        self.updateMatchedNodes();
    });
    self.excludeFilterUncheck.subscribe(function(newValue){
        self.updateMatchedNodes();
    });
    self.loading.subscribe(function(newValue){
        if(!newValue){
            self.loaded(true);
        }
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
            filterAll: self.filterAll()||'',
            page:self.page()||0,
            max:self.pagingMax()||20
        };
    };
    /**
     * generate URL for the current filters
     */
    self.getPageUrl=function(){
        return self.nodeSummary().linkForNodeFilters(self);
    };
    /**
     * Generate filter string for params
     * @param params
     */
    self.escapeFilter=function(str){

        if(str != '' && !(str.match(/[ '"\r\n]/))){
            return str==null?'':str;
        }
        var outstr='"';
        if(null!=str) {
            outstr += str.replace(/"/g, '""');
        }
        outstr+='"';
        return outstr;
    };
    /**
     * Generate filter string for params
     * @param params
     */
    self.paramsAsfilter=function(params){
        var comps=[];
        for (var e in params) {
            comps.push(self.escapeFilter(e + ":"));
            comps.push(self.escapeFilter(params[e]));
        }
        return comps.join(" ");
    };
    self.linkForFilterString=function(filter){
        return _genUrl(self.baseNodesPageUrl,{filter:filter});
    };
    /**
     * Generate URL for some filter params
     * @param params
     * @returns {*}
     */
    self.linkForFilterParams=function(params,extra){
        var filter;
        if(typeof(params) == 'object'){
            filter=self.paramsAsfilter(params);
        }else if(typeof(params)=='string' && typeof(extra)=='string'){
            var nparams={};
            nparams[params]=extra;
            filter=self.paramsAsfilter(nparams);
        }
        return self.linkForFilterString(filter);
    };
    self.triggerNodeRemoteEdit=function(node){
        doRemoteEdit(node.nodename(),self.project(),self.nodeSet().expandNodeAttributes(node.attributes,node.attributes['remoteUrl']()))
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
        self.page(data.page||0);
        self.pagingMax(data.max||20);
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
    self.selectNodeFilterExcludeLink=function(link,isappend){

        var oldfilter = self.filterExclude();
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
        self.filterExclude(filterString);
        self.filterExcludeName(filterName);
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
    self.browseNodesPage=function(newpage){
        if(self.page()==newpage){
            return;
        }
        if (!newpage || newpage >= self.maxPages() || newpage < 0) {
            newpage=0;
        }
        self.pageDisplay(newpage+1);
    };
    self.browseNodesPageNext=function(){
        if(self.page()+ 1<self.maxPages()) {
            self.browseNodesPage(self.page() + 1);
        }
    };
    self.browseNodesPagePrev=function(){
        if(self.page()>0) {
            self.browseNodesPage(self.page() - 1);
        }
    };
    self.browseNodesPageUrl=function(page){
        var pageParams = self.getPageParams();
        if (page >= 0 && page < self.maxPages()) {
            pageParams.page = page;
        }else{
            pageParams.page=0;
        }
        return _genUrl(self.baseNodesPageUrl, pageParams);
    };
    self.browseNodesPagePrevUrl=function(){
        return self.browseNodesPageUrl(self.page()-1);
    };
    self.browseNodesPageNextUrl=function(){
        return self.browseNodesPageUrl(self.page()+1);
    };
    self.updateNodesRemainingPages=function(){
        self.page(-1);
        self.updateMatchedNodes();
    };
    self.loadJS=function(data){
        ko.mapping.fromJS(data,{},self);
    };
    self.pagingMax.subscribe(function (){
        self.updateMatchedNodes();
    });
    self.newFilterText=function(){
        self.page(0);
        self.updateMatchedNodes();
    };
    self.updateMatchedNodes= function () {
        if(!self.filter() && self.emptyMode()=='blank'){
            return;
        }
        var project=self.project();
        if (!project) {
            return;
        }
        var filterdata = self.filterName() ? {filterName: self.filterName()} : self.filter()?{filter: self.filter()}:{};
        var filterExcludedata = self.filterExcludeName() ? {filterExcludeName: self.filterExcludeName()} : self.filterExclude()?{filterExclude: self.filterExclude()}:{};
        var excludeFilterUncheck =  self.excludeFilterUncheck()
        var page = self.page();
        var view = self.view() ? self.view() : 'table';
        var basedata = {view: view, declarenone: true, fullresults: true, expanddetail: true, inlinepaging: false, nodefilterLinkId: self.nodefilterLinkId, excludeFilterUncheck: self.excludeFilterUncheck()};
        if(self.paging()){
            basedata.page = page;
            basedata.max = self.pagingMax();
            basedata.inlinepaging=true;
            if (page != 0) {
                basedata.view= 'tableContent';
            }
        }
        if(self.maxShown()){
            basedata.maxShown=self.maxShown();
        }
        var params = jQuery.extend(basedata, filterdata, filterExcludedata);
        if(self.emptyMode()=='localnode' && !self.filter()){
            params.localNodeOnly = 'true';
        }else if(self.emptyMode()=='blank' && !self.filter()){
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
        var requrl=_genUrl(appLinks.frameworkNodesQueryAjax, params);
        jQuery.ajax({
            type:'GET',
            dataType:'json',
            url:requrl,

            error:function(data,jqxhr,err){
                self.loading(false);
                if (typeof(errcallback) == 'function') {
                    self.error('Failed '+requrl+': '+err+", "+jqxhr);
                }
            },
            success:function(data,status,jqxhr){
                self.loading(false);
                self.nodeSet().loadJS(
                    {
                        nodes:data.allnodes,
                        tagsummary:data.tagsummary
                    });
                self.loadJS({
                    allcount:data.allcount,
                    total:data.total,
                    truncated:data.truncated,
                    colkeys:data.colkeys

                });

            }
        });
    };
}
