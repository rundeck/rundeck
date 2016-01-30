//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter
//= require nodeFiltersKO
//= require nodeRemoteEdit

/*
 Manifest for "framework/adhoc.gsp" page
 */
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

function showError(message) {
    appendText($("error"),message);
    $("error").show();
}


var nodeFilter;

var nodespage=0;
var pageParams={};
/**
 * Expand paging results
 * @param page
 * @param elem
 */
function expandResultNodes(page,elem){
    loadNodeFilter(null,nodeFilter.filter(),nodeFilter.filterAll(),elem,page);
}
/**
 * load either filter string or saved filter
 * @param filterName name of saved filter
 * @param filterString string filter
 * @param filterAll if true, "all nodes" was selected
 * @param elem target element
 * @param page number to load
 */
function loadNodeFilter(filterName, filterString,filterAll,elem,page) {
    jQuery('.nodefilterlink').removeClass('active');
    if (!page) {
        page = 0;
    }
    if (!elem) {
        elem = 'nodelist';
    }
    if(!filterName&&!filterString&&null==filterAll){
        filterName=nodeFilter.filterName();
        filterString=nodeFilter.filter();
        filterAll=nodeFilter.filterAll();
    }
    if(!filterName && !filterString){
        //if blank input and no filtername selected, do nothing
        return;
    }
    nodespage = page;
    var view = page == 0 ? 'table' : 'tableContent';
    var data = filterName? {filterName: filterName} : {filter: filterString};
    data.nodeExcludePrecedence='true';
    if(filterName){
        jQuery('a[data-node-filter-name=\''+filterName+'\']').addClass('active');
        jQuery('.hiddenNodeFilter').val(filterString);
        jQuery('.hiddenNodeFilterName').val(filterName);
    }else{
        jQuery('.hiddenNodeFilter').val(filterString );
        jQuery('.hiddenNodeFilterName').val('');
    }
    nodeFilter.filterAll(filterAll);
    nodeFilter.filterName(filterName);
    nodeFilter.filter(filterString);
    nodeFilter.loading(true);
    _updateMatchedNodes(data,elem,pageParams.project,false,{view:view,expanddetail:true,inlinepaging:true,
            page:page,max:pageParams.pagingMax},
        function(xht){
            nodeFilter.loading(false);
        },
        function(response, status, xhr){
            nodeFilter.loading(false);
            if (xhr.getResponseHeader("X-Rundeck-Error-Message")) {
                nodeFilter.error(xhr.getResponseHeader("X-Rundeck-Error-Message"));
            } else {
                nodeFilter.error(xhr.statusText);
            }
        });
}
function _loadNextNodesPageTable(max,total,tbl,elem){
    if(!nodespage){
        nodespage=0;
    }
    var next=nodespage+1;
    if(total<0 || max*next<total){
        //create sibling of elem
        var div= new Element('tbody');
        $(tbl).insert({bottom:div});
        //total < 0 means load all remaining, so invert next page
        expandResultNodes(next* (total<0?-1:1),Element.identify(div));
    }
//            console.log("next: "+(max*(next+1))+", total: "+total);
    var loadCount = max*(next+1);
    if(loadCount>=total || total<0){
        //hide pager button area
        $(elem).hide();
    }else{
        //update moreCount
        setText($('moreCount'),total-loadCount);
        if(total-loadCount<max){
            $('nextPageButton').hide();
        }
    }
}


/**
 * Handle embedded content updates
 */
function _updateBoxInfo(name,data){
    if(name=='nodetable'){
        if(null !=data.total && typeof(nodeFilter)!='undefined'){
            nodeFilter.total(data.total);
        }
        if(null!=data.allcount){
            if(typeof(nodeFilter) != 'undefined'){
                nodeFilter.allcount(data.allcount);
            }
        }
        if(null!=data.filter){
            if (typeof(nodeFilter) != 'undefined') {
                nodeFilter.filter(data.filter);
            }
        }
    }
}
/**
 * pop history state
 * @param event
 */
window.onpopstate = function(event) {
    if(event.state){
        //state should always be set, because we replaceState on firstload
        if(event.state.start){
            filterParams={};
            pageLoad();
        }else {
            nodeFilter.setPageParams(event.state);
            nodeFilter.updateMatchedNodes();
        }
    }
};
var filterParams={};
var nodeSummary;

/**
 * load nodes or summary, depending on request parameters, save first-run state in history
 */
function pageLoad(){
    var pagestate;
    if(filterParams.filterName || filterParams.filter|| filterParams.filterAll ){
        nodeFilter.setPageParams(filterParams);
        nodeFilter.updateMatchedNodes();
        pagestate=nodeFilter.getPageParams();
        nodeSummary.reload();

    }else{
        nodeFilter.reset();
        nodeSummary.reload();
        jQuery('#tab_link_summary > a').tab('show');
        pagestate={start:true};
    }

    if(typeof(history.replaceState)=='function') {
        if (!history.state) {
            //set first page load state
            history.replaceState(pagestate, null, document.location);
        }
    }
}
/**
 * START page init
 */
function init() {
    pageParams = loadJsonData('pageParams');
    filterParams =loadJsonData('filterParamsJSON');
    nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
    nodeFilter = new NodeFilters(
        appLinks.frameworkAdhoc,
        appLinks.scheduledExecutionCreate,
        appLinks.frameworkNodes,
        Object.extend(filterParams,{
            project: pageParams.project,
            paging:true,
            nodesTitleSingular:message('Node'),
            nodesTitlePlural:message('Node.plural'),
            nodeSummary:nodeSummary
        }));
    ko.applyBindings(nodeFilter);
    //show selected named filter
    nodeFilter.filterName.subscribe(function (val) {
        if (val) {
            jQuery('a[data-node-filter-name]').removeClass('active');
            jQuery('a[data-node-filter-name=\'' + val + '\']').addClass('active');
        }
    });
    nodeFilter.loading.subscribe(function(val){
        //select filter results tab whenever loading output
        jQuery('#tab_link_result > a').tab('show');
    });
    jQuery('body').on('click', '.nodefilterlink', function (evt) {
        var isshift=evt.shiftKey;
        evt.preventDefault();
        nodeFilter.selectNodeFilterLink(this,isshift);

        if(typeof(history.pushState)=='function') {
            //push history state
            var oldparams=nodeFilter.getPageParams();
            var href=nodeFilter.getPageUrl();
            history.pushState(oldparams, document.title, href);
        }
    });
    nodeFilter.browse.subscribe(function(val){
        //update browser history when a browse link is used, e.g. page number/page forward
        if(typeof(history.pushState)=='function' && val) {
            //push history state
            var oldparams=nodeFilter.getPageParams();
            var href=nodeFilter.getPageUrl();
            history.pushState(oldparams, document.title, href);
            nodeFilter.browse(false);
        }
    });
    jQuery('#tab_link_summary').on('show.bs.tab',function(e){
        nodeSummary.reload();
    });

    //start state
    pageLoad();
}
jQuery(init);