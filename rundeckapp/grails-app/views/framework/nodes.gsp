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
    <script type="text/javascript">
        function showError(message) {
            $("error").innerHTML += message;
            $("error").show();
        }

        //set box filterselections
        var _setFilterSuccess=function(response,name){
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data['filterpref'];
                //reload page
                document.location="${createLink(controller:'framework',action:'nodes')}"+(bfilters[name]?"?filterName="+encodeURIComponent(bfilters[name]):'');
            }
        };

        //method called by _nodeFilterInputs
        function _matchNodes(){
            expandResultNodes()
        }
        function _submitNodeFilters(){
            _matchNodes();
            return false;
        }

        /*********
         *  remote editor
         *********/
        
        var remoteSite;
        var remoteEditStarted=false;
        var remoteEditExpect=false;
        var projectname;

        /**
         * Start remote editor for node with url
         * @param name node name
         * @param url url
         */
        function doRemoteEdit(name,project,url){
            _remoteEditClear();

            projectname=project;
            $('editNodeIdent').innerHTML=name;

            //create iframe for url
            var ifm = document.createElement('iframe');
            ifm.width="640px";
            ifm.height="480px";
            _remoteEditExpect(url);
            ifm.src=url;

            $('remoteEditTarget').appendChild(ifm);

            _remoteEditShow();
        }

        //setup functions

        /**
         * Begin listener for message protocol from origin url
         * @param originUrl
         */
        function _remoteEditExpect(originUrl){
            remoteEditExpect=true;
            remoteSite=originUrl;
            remoteEditStarted=false;

            Event.observe(window,'message', _rdeckNodeEditOnmessage);
        }

        /**
         * Stop listener for message protocol
         */
        function _remoteEditStop(){
            remoteEditExpect=false;
            remoteSite=null;
            remoteEditStarted=false;
            projectname=null;

            Event.stopObserving(window,'message', _rdeckNodeEditOnmessage);
        }

        /**
         * Clear/reset remote editor dom content
         */
        function _remoteEditClear(){
            _remoteEditStop();
            
            _clearTarget();

            shouldrefresh=false;

            $('editNodeIdent').innerHTML='';
            var errhold=$('remoteEditError');
            errhold.innerHTML="";
            errhold.hide();
        }

        /**
         * Show remote editor dom content
         */
        function _remoteEditShow(){
            $('remoteEditTarget').show();
            $('remoteEditholder').show();

            $('remoteEditResultHolder').hide();
            $('nodesTable').hide();
            $('${ukey}nodesfilterholder').hide();
        }

        /**
         * Hide remote editor dom content
         */
        function _remoteEditHide(){
            $('remoteEditholder').hide();
            $('remoteEditTarget').hide();

            $('remoteEditToolbar').show();
            $('nodesTable').show();
            $('${ukey}nodesfilterholder').show();
        }

        /**
         * Clear iframe holder
         */
        function _clearTarget(){
            $('remoteEditTarget').innerHTML="";
            $('remoteEditTarget').hide();

        }
        /**
         * Finish all node editor stuff, and hide it
         */
        function _remoteEditCompleted(){
            _remoteEditStop();
            _remoteEditHide();
            _remoteEditClear();
        }

        var shouldrefresh=false;
        /**
         * If necessary, reload the nodes page
         */
        function _remoteEditContinue(){
            if(shouldrefresh){
                document.location='${createLink(controller:"framework",action:"nodes")}';
            }else{
                _remoteEditCompleted();
            }
        }

        /**
         * Perform Ajax request to tell server to re-fetch the nodes data for the project
         */
        function _remoteEditDidSave(){
            if(projectname){
                new Ajax.Request('${createLink(controller:"framework",action:"reloadNodes")}.json',{
                    parameters:{project:projectname},
                    evalJSON:true,
                    onSuccess:function(req){
                        var data=req.responseJSON;
                        if(data.success){
                            shouldrefresh=true;
                        }
                    },
                    onFailure:function(e){
                        //console.log(e);
                    }
                });
            }
        }

        //protocol handler functions//


        /**
         * handler for :finished message
         * @param changed true if changes were saved
         */
        function _rdeckNodeEditFinished(changed){
            
            if(changed){
                $('remoteEditResultText').innerHTML="Node changes were saved successfully.";
                _remoteEditDidSave();
            }else{
                $('remoteEditResultText').innerHTML="Node changes were not saved.";
            }
            _remoteEditStop();
            _clearTarget();
            
            $('remoteEditToolbar').hide();
            $('remoteEditResultHolder').show();
        }

        /**
         * handler for error message
         * @param origin
         * @param msg
         */
        function _rdeckNodeEditError(origin,msg){
            _remoteEditStop();
            _clearTarget();
            
            var errhold=$('remoteEditError');
            errhold.innerHTML=(origin?origin+" reported an error: ":"")+msg;
            errhold.show();
        }

        /**
         * handler for :started message
         */
        function _rdeckNodeEditStarted(){
            remoteEditStarted=true;
        }

        var PROTOCOL='rundeck:node:edit';
        /**
         * onmessage handler
         * @param msg
         */
        function _rdeckNodeEditOnmessage(msg){
            if(!remoteEditExpect || !remoteSite || !remoteSite.startsWith(msg.origin+"/")){
                return;
            }
            var data=msg.data;
            if(!remoteEditStarted && PROTOCOL+':started'==data){
                _rdeckNodeEditStarted();
            }else if(PROTOCOL+':error'==data || data.startsWith(PROTOCOL+':error:')){
                var err=data.substring((PROTOCOL+':error').length);
                if(err.startsWith(":")){
                    err=err.substring(1);
                }
                _rdeckNodeEditError(msg.origin,err?err:"(No message)");
            }else if (remoteEditStarted){
                if(PROTOCOL+':finished:true'==data){
                    _rdeckNodeEditFinished(true);
                }else if(PROTOCOL+':finished:false'==data){
                    _rdeckNodeEditFinished(false);
                }else{
                    _rdeckNodeEditError(null,"Unexpected message received from ["+msg.origin+"]: "+data);
                }
            }
        }
        /**
         * END remote edit code
         */

        function NodeFilters(filterName, filterString) {
            var self = this;
            self.filterName = ko.observable(filterName);
            self.filter = ko.observable(filterString);
            self.total = ko.observable();
            self.allcount = ko.observable();
            self.hasNodes=ko.computed(function(){
                return 0!=self.allcount();
            });
            self.runCommand=function(){
                document.location=_genUrl("${g.createLink(action: 'adhoc',controller: 'framework',params:[project:session.project])}",{
                    filter:self.filter(),
                    filterName:self.filterName()
                });
            };
            self.saveJob=function(){
                document.location = _genUrl("${g.createLink(action: 'create',controller: 'scheduledExecution',params:[project:session.project])}", {
                    filter: self.filter(),
                    filterName: self.filterName()
                });
            };
        }
        var nodeFilter;
        /**
         * node filter link action
         * @param e
         */
        function selectNodeFilterLink(e) {
            var filterName = jQuery(e).data('node-filter-name');
            var filterString = jQuery(e).data('node-filter');
            loadNodeFilter(filterName, filterString);
        }
        /**
         * find all node filter links within the element and set handlers to load the filter
         * @param elem
         */
        function setNodeFilterLinkAction(elem) {
            elem.find('.nodefilterlink').click(function (evt) {
                evt.preventDefault();
                selectNodeFilterLink(this);
            });
        }

        var nodespage=0;
        var pagingMax=20;
        function expandResultNodes(page,elem){
            var filterString=$F('schedJobNodeFilter');
            var filterName=null;
            loadNodeFilter(filterName,filterString,elem,page);
        }
            /**
             * load either filter string or saved filter
             * @param filterName
             * @param filterString
             */
        function loadNodeFilter(filterName, filterString,elem,page) {
            jQuery('.nodefilterlink').removeClass('active');
            if (!page) {
                page = 0;
            }
            if (!elem) {
                elem = 'nodelist';
            }
            nodespage = page;
            var view = page == 0 ? 'table' : 'tableContent';
            var data = filterName? {filterName: filterName} : {filter: filterString};
            if(filterName){
                jQuery('a[data-node-filter-name=\''+filterName+'\']').addClass('active');
                jQuery('.hiddenNodeFilter').val(filterString);
//                jQuery('.schedJobNodeFilter').val(filterString);
                jQuery('.hiddenNodeFilterName').val(filterName);
            }else{
                jQuery('.hiddenNodeFilter').val(filterString );
//                jQuery('.schedJobNodeFilter').val(filterString);
                jQuery('.hiddenNodeFilterName').val('');
            }
            _updateMatchedNodes(data,elem,'${session.project}',false,{view:view,expanddetail:true,inlinepaging:true,
                page:page,max:pagingMax},function(xht){
                setNodeFilterLinkAction(jQuery($(elem)));
                nodeFilter.filterName(filterName);
                nodeFilter.filter(filterString);
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
         * START tag filter link code
         */
        function setTagFilter(value){
            if($('schedJobNodeIncludeTags').value){
                $('schedJobNodeIncludeTags').value+=","+value;
            }else{
                $('schedJobNodeIncludeTags').value=value;

            }
            $('${ukey}filter').down('form').submit();
        }

        /**
         * filter toggle
         */
        function filterToggle(evt) {
            ['${ukey}filter','${ukey}filterdispbtn'].each(Element.toggle);
            var isvis= $('${ukey}filter').visible();
            $$('.obs_filtertoggle').each(isvis?Element.hide:Element.show);
        }
        function filterToggleSave(evt) {
            ['${ukey}filter','${ukey}fsave'].each(Element.show);
            ['${ukey}filterdispbtn','${ukey}fsavebtn'].each(Element.hide);
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
                    $$('.obs_nodes_allcount_plural').each(function (e) {
                        e.innerHTML = data.allcount==1?'':'s';
                    });
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
            jQuery('.act_filtertoggle').click( filterToggle);

            $$('#${ukey}filter div.filter input').each(function(elem) {
                if (elem.type == 'text') {
                    elem.observe('keypress', function(evt) {
                        if (!noenter(evt)) {
                            $('nodefiltersubmit').click();
                            return false;
                        } else {
                            return true;
                        }
                    });
                }
            });
            var filterParams =${[filterName:params.filterName,filter:query?.filter].encodeAsJSON()};
            nodeFilter = new NodeFilters(filterParams.filterName, filterParams.filter);
            ko.applyBindings(nodeFilter);

            setNodeFilterLinkAction(jQuery('#nodesContent'));
            loadNodeFilter(filterParams.filterName, filterParams.filter);
            jQuery('#searchForm').submit(_submitNodeFilters);

        }
        jQuery(document).ready(init);

    </script>
    <style type="text/css">
        .detail_content{
            padding:4px 10px;
        }
        .filterSetButtons{
            width:200px;
            line-height:24px;
            margin-right:10px;
        }
        .filterSetButtons .button{
            white-space:nowrap;
        }

        #remoteEditholder{
            margin: 0px 20px 0 20px;

        }
        #remoteEditholder iframe{
            border:0;
        }
        #remoteEditholder .toolbar{
            margin:4px;
        }

        .commandcontent{
            margin:0;
        }

        table.execoutput {
            font-size: 100%;
        }
        div.header{
            padding:3px 10px;
            background: #eee;
            border-top: 1px solid #ddd;
            border-left: 1px solid #ddd;
            border-right: 1px solid #ddd;
            color: black;
            font-weight:bold;
        }
        #histcontent div.jobsreport{
            margin:0;
            padding:0;
        }
        #histcontent table.queryTable > tbody > tr > td{
            padding:0;
            margin:0;
        }
        #histcontent table{
            width:100%;
        }
        #nodesPaging{
            margin-top:5px;
        }
    </style>
</head>
<body>

<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>
<div id="nodesContent">
    <g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN)}"/>


    <g:render template="/common/messages"/>

<div class="row ">
%{--<g:if test="${grails.util.Environment.current == Environment.DEVELOPMENT}">--}%
    %{--<div class="col-sm-12">--}%
        %{--<span data-bind="text: filter"></span>--}%
        %{--<span data-bind="text: filterName"></span>--}%
        %{--<span data-bind="text: total"></span>--}%
        %{--<span data-bind="text: allcount"></span>--}%
    %{--</div>--}%
%{--</g:if>--}%
<div  class="col-sm-6">
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
                        <li data-bind="visible: !filterName()">
                            <a href="#"
                               data-toggle="modal"
                               data-target="#saveFilterModal">
                                Save this filter&hellip;
                            </a>
                        </li>

                        <li class="divider" data-bind="visible: !filterName()"></li>
                        <li >
                            <g:link class="nodefilterlink"
                                    action="nodes" controller="framework"
                                    data-node-filter=".*"
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
                    data-bind="value: filter()"
                       placeholder="Enter a node filter"
                       data-toggle='popover'
                       data-popover-content-ref="#queryFilterHelp"
                       data-placement="bottom"
                       data-trigger="manual"
                       data-container="body"
                       value="${filtvalue}" id="schedJobNodeFilter" onchange="_matchNodes();"/>


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
                <g:if test="${!params.nofilters}">
                <div id="${ukey}nodesfilterholder" >

                    <div class="obs_filtertoggle" style="${!filtersOpen ? '' : 'display:none;'}">
                        <span id='${ukey}filterdispbtn' style="${!filtersOpen ? '' : 'display:none;'}" >
                            <span title="Click to modify filter" class="textbtn textbtn-default query  act_filtertoggle" >
                                <g:if test="${filterName}">
                                    ${filterName.encodeAsHTML()}
                                </g:if>
                                <g:elseif test="${!summaryOnly}">
                                    <g:render template="displayNodeFilters" model="${[displayParams:query]}"/>
                                </g:elseif>
                                <g:else>
                                    Enter a Filter
                                </g:else>
                                <b class="glyphicon glyphicon-chevron-right"></b>
                            </span>
                        </span>

                    </div>

                </div>
                </g:if>

        </div>

    <div class="col-sm-3">
        <div class="well well-sm">
        <div data-bind="if: filterName()">

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
        <div data-bind="if: !filterName()">
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
        <div class="col-sm-9">
            <span class="h4">
                <g:if test="${summaryOnly}">
                    <span data-bind="text: allcount">${total}</span>
                    Node<span class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span>
                </g:if>
                <g:else>
                    <span data-bind="text: allcount">${total}</span>
                    Node<span class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span> matching filter
                </g:else>
            </span>
            <g:if test="${tagsummary}">
                <g:render template="tagsummary"
                          model="${[hidetop:!summaryOnly,tagsummary: tagsummary, link: [action: 'nodes', controller: 'framework', param: 'nodeIncludeTags']]}"/>
            </g:if>
            <g:elseif test="${tagsummary?.size() == 0}">
            %{--<span class="text-muted">no tags</span>--}%
            </g:elseif>
            <div class=" btn-group ">
                <button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
                    Action <span class="caret"></span>
                </button>
                <ul class="dropdown-menu" role="menu">
                    <g:if test="${session.project && run_authorized}">
                        <li data-bind="visible: hasNodes()">
                            <a href="#" data-bind="click: runCommand">
                                <i class="glyphicon glyphicon-play"></i>
                                Run a command on <span data-bind="text: allcount">${total}</span> Node<span
                                    class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span> …
                            </a>
                        </li>
                    </g:if>
                    <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_CREATE}">
                    <li >
                        <a href="#" data-bind="click: saveJob">
                            <i class="glyphicon glyphicon-plus"></i>
                            Create a job for <span data-bind="text: allcount">${total}</span> Node<span
                                class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span> …
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
        <div class="col-sm-3">

        </div>

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
