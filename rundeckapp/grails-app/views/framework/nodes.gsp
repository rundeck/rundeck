<%@ page import="grails.util.Environment; rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="gui.menu.Nodes"/> - ${session.project.encodeAsHTML()}</title>
    <g:javascript library="yellowfade"/>
    <g:if test="${grails.util.Environment.current == Environment.DEVELOPMENT}">
        <g:javascript src="knockout-3.0.0.debug.js"/>
    </g:if>
    <g:else>
        <g:javascript src="knockout-3.0.0-min.js"/>
    </g:else>
    <g:javascript src="knockout.mapping-latest.js"/>
    <asset:javascript src="nodeFiltersKO"/>
    <script type="text/javascript">
        function showError(message) {
            $("error").innerHTML += message;
            $("error").show();
        }

        //method called by _nodeFilterInputs
        function _matchNodes(){
            expandResultNodes()
        }
        function _submitNodeFilters(){
            _matchNodes();
            return false;
        }

        var nodeFilter;
        /**
         * node filter link action
         * @param e
         */
        function selectNodeFilterLink(e) {
            var filterName = jQuery(e).data('node-filter-name');
            var filterString = jQuery(e).data('node-filter');
            var filterAll = jQuery(e).data('node-filter-all')?true:false;
            loadNodeFilter(filterName, filterString,filterAll);
        }

        var nodespage=0;
        var pagingMax=20;
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
            _updateMatchedNodes(data,elem,'${session.project}',false,{view:view,expanddetail:true,inlinepaging:true,
                page:page,max:pagingMax},function(xht){
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
                $('moreCount').innerHTML=total-loadCount;
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
         * START page init
         */
        function init() {
            var filterParams =${[filterName:params.filterName,filter:query?.filter,filterAll:params.showall in ['true',true]].encodeAsJSON()};
            nodeFilter = new NodeFilters(
                    "${g.createLink(action: 'adhoc',controller: 'framework',params:[project:session.project])}",
                    "${g.createLink(action: 'create',controller: 'scheduledExecution',params:[project:session.project])}",
                    "${g.createLink(action: 'nodes',controller: 'framework',params:[project:session.project])}",
                    Object.extend(filterParams,{
                        nodesTitleSingular:"${g.message(code:'Node',default:'Node')}",
                        nodesTitlePlural:"${g.message(code:'Node.plural',default:'Nodes')}"
                    }));
            ko.applyBindings(nodeFilter);
            jQuery('#nodesContent').on('click', '.nodefilterlink', function (evt) {
                evt.preventDefault();
                selectNodeFilterLink(this);
            });
            jQuery('#searchForm').submit(_submitNodeFilters);
            loadNodeFilter();
        }
        jQuery(document).ready(init);

    </script>

</head>
<body>

<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>
<div id="nodesContent">
    <g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN)}"/>

    <g:render template="/common/messages"/>

<div class="row ">
<div  class="col-sm-9">
    <g:set var="wasfiltered" value="${ paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|project$/)||(query && !query.nodeFilterIsEmpty() && !summaryOnly)}"/>
    <g:set var="filtersOpen" value="${summaryOnly || showFilter||params.createFilters||params.editFilters||params.saveFilter || filterErrors?true:false}"/>

    <div id="${ukey}filter">
        <g:form action="nodes" controller="framework" class="form form-inline" name="searchForm">
            <g:hiddenField name="max" value="${max}"/>
            <g:hiddenField name="offset" value="${offset}"/>
            <g:hiddenField name="formInput" value="true"/>
            <g:set var="filtvalue"
                   value="${query?.('filter')?.encodeAsHTML()}"/>

            <div class="form-group">
                <span class="input-group" >
                    %{--Filter navigation/selection dropdown--}%
                <span class="input-group-btn" >
                    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">Filter <span
                            class="caret"></span></button>
                    <ul class="dropdown-menu">

                        <li >
                            <g:link class="nodefilterlink"
                                    action="nodes" controller="framework"
                                    data-node-filter=".*"
                                    data-node-filter-all="true"
                                    params="[showall: 'true']">
                                Show all nodes
                            </g:link>
                        </li>
                        <li class="divider"></li>
                        <li class="dropdown-header"><i class="glyphicon glyphicon-filter"></i> Saved Filters</li>
                        <g:if test="${filterset}">
                            <g:render template="/common/selectFilter"
                                      model="[filterList: true, filterset: filterset, filterName: filterName, prefName: 'nodes', noSelection: filterName ? '-All Nodes-' : null]"/>
                        </g:if>
                    </ul>
                </span>
                <input type='search' name="filter" class="schedJobNodeFilter form-control"
                    data-bind="value: filterWithoutAll"
                       placeholder="Enter a node filter"
                       data-toggle='popover'
                       data-popover-content-ref="#queryFilterHelp"
                       data-placement="bottom"
                       data-trigger="manual"
                       data-container="body"
                       value="${filtvalue}" id="schedJobNodeFilter" />


                <span class="input-group-btn">
                    <a class="btn btn-info" data-toggle='popover-for' data-target="#schedJobNodeFilter">
                        <i class="glyphicon glyphicon-question-sign"></i>
                    </a>
                    <button class="btn btn-success" type="submit">
                        <i class="glyphicon glyphicon-search"></i>
                    </button>
                </span>
                </span>
            </div>




        </g:form>

    <div class=" collapse" id="queryFilterHelp">
        <div class="help-block">
            <g:render template="/common/nodefilterStringHelp"/>
        </div>
    </div>
    </div>



                <g:set var="adminauth"
                       value="${auth.resourceAllowedTest(kind:'node',action:[AuthConstants.ACTION_REFRESH])}"/>
                <g:if test="${adminauth}">
                    <g:if test="${selectedProject && selectedProject.shouldUpdateNodesResourceFile()}">
                        <span class="floatr"><g:link action="reloadNodes" params="${[project:selectedProject.name]}" class="btn btn-sm btn-default" title="Click to update the resources.xml file from the source URL, for project ${selectedProject.name}" onclick="\$(this.parentNode).loading();">Update Nodes for project ${selectedProject.name}</g:link></span>
                    </g:if>
                </g:if>
        </div>

    <div class="col-sm-3" data-bind="visible: filterName() || filterWithoutAll()">
        <div class="well well-sm inline ">
            <div data-bind="visible: filterName()">

                Selected Filter:  <strong data-bind="text: filterName()">${filterName.encodeAsHTML()}</strong>
                        <span data-bind="visible: filterName()">
                            <a href="#"
                                class="textbtn textbtn-danger"
                               data-toggle="modal"
                               data-target="#deleteFilterModal">
                                <i class="glyphicon glyphicon-remove"></i>
                                delete &hellip;
                            </a>
                        </span>

            </div>
            <div data-bind="visible: !filterName() && filterWithoutAll()">
                    <a href="#"
                        class="textbtn textbtn-success"
                       data-toggle="modal"
                       data-target="#saveFilterModal">
                        <i class="glyphicon glyphicon-plus"></i>
                        Save this filter&hellip;
                    </a>
            </div>
        </div>
    </div>

    %{--Form for saving/deleting node filters--}%
    <g:form class="form form-horizontal">
        <g:render template="nodeFiltersHidden"/>
        <g:render template="/common/queryFilterManagerModal"
                  model="${[rkey: ukey, filterName: filterName, filterset: filterset,
                          filterLinks: true,
                          formId: "${ukey}filter",
                          ko: true,
                          deleteActionSubmit: 'deleteNodeFilter', storeActionSubmit: 'storeNodeFilter']}"/>
    </g:form>
</div>
    <div class="row row-space">
        <div class="col-sm-12">
            <span class="h4">
                <g:if test="${summaryOnly}">
                    <span data-bind="text: allcount">${total}</span>
                    <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span>
                </g:if>
                <g:else>
                    <span data-bind="text: allcount">${total}</span>
                    <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span> matching filter
                </g:else>
            </span>
            <g:if test="${tagsummary}">
                <g:render template="tagsummary"
                          model="${[hidetop:!summaryOnly,tagsummary: tagsummary, link: [action: 'nodes', controller: 'framework', param: 'nodeIncludeTags']]}"/>
            </g:if>
            <g:elseif test="${tagsummary?.size() == 0}">
            %{--<span class="text-muted">no tags</span>--}%
            </g:elseif>
            <div class=" btn-group pull-right ">
                <button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
                   Node Actions <span class="caret"></span>
                </button>
                <ul class="dropdown-menu" role="menu">
                    <g:if test="${session.project && run_authorized}">
                        <li data-bind="visible: hasNodes()">
                            <a href="#" data-bind="click: runCommand">
                                <i class="glyphicon glyphicon-play"></i>
                                Run a command on <span data-bind="text: allcount">${total}</span>
                                <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span> …
                            </a>
                        </li>
                    </g:if>
                    <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_CREATE}">
                    <li >
                        <a href="#" data-bind="click: saveJob">
                            <i class="glyphicon glyphicon-plus"></i>
                            Create a job for <span data-bind="text: allcount">${total}</span>
                            <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span> …
                        </a>
                    </li>
                    </auth:resourceAllowed>
                </ul>
            </div>
        </div>
        <g:form class="form form-inline" action="adhoc" controller="framework" method="get" name="runform">
            <g:hiddenField name="project" value="${session.project}"/>
            <g:render template="nodeFiltersHidden" model="${[params: params, query: query]}"/>
        </g:form>

    </div>

    <div class="row row-space">
        <div class="col-sm-12">

            <div class=" clear matchednodes " id="nodelist" >
                <g:if test="${!summaryOnly}">
                    <g:render template="allnodes" model="${[nodeview:'table', expanddetail: true,allnodes: allnodes, totalexecs: totalexecs, jobs: jobs, params: params, total: total, allcount: allcount, page: page, max: max, nodeauthrun: nodeauthrun, tagsummary: null]}" />
                </g:if>
            </div>

        </div>
    </div>


</div>
</body>
</html>
