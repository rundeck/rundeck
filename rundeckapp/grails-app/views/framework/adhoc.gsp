<%@ page import="grails.util.Environment; rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="adhoc"/>
    <title><g:message code="gui.menu.Nodes"/> - ${session.project.encodeAsHTML()}</title>
    <g:javascript library="executionControl"/>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <g:if test="${grails.util.Environment.current == Environment.DEVELOPMENT}">
        <g:javascript src="knockout-3.0.0.debug.js"/>
    </g:if>
    <g:else>
        <g:javascript src="knockout-3.0.0-min.js"/>
    </g:else>
    <g:javascript src="knockout.mapping-latest.js"/>
    <g:javascript src="moment.min.js"/>
    <asset:javascript src="momentutil.js"/>
    <g:javascript src="historyKO.js"/>
    <asset:javascript src="nodeFiltersKO.js"/>
    <g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
    <g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>
    <script type="text/javascript">
        function showError(message) {
            $("error").innerHTML += message;
            $("error").show();
        }

        /**
         * START run execution code
         */
        <g:set var="filterParams" value="${query?.properties.findAll{it.key==~/^(node(In|Ex)clude.*|project)$/ &&it.value}}"/>

        var nodeFilterData_${ukey}=${filterParams.encodeAsJSON()};
        var nodespage=0;
        var pagingMax=20;
        function expandResultNodes(page,elem){
            if(!page){
                page=0;
            }
            nodespage=page;
            if(!elem){
                elem='nodelist';
            }
            var view=page==0?'table':'tableContent';
            _updateMatchedNodes(nodeFilterData_${ukey},elem,'${session.project}',false,{view:view,expanddetail:true,inlinepaging:true,page:page,max:pagingMax});
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
        var _runBtnHtml;
        function disableRunBar(runnning){
            var runbox = jQuery('#runbox');
            runbox.find('input[type="text"]').prop('disabled', true);
            runbox.find('button.runbutton').prop('disabled', true).addClass('disabled');
            if(runnning){
                runbox.find('button.runbutton').button('loading');
            }
        }
        function enableRunBar(){
            var runbox = jQuery('#runbox');
            runbox.find('input[type="text"]').prop('disabled',false);
            runbox.find('button.runbutton')
                    .prop('disabled', false)
                    .removeClass('disabled')
                    .button('reset');
        }
        function runStarted(){
            running=true;
        }
        function afterRun(){
            running=false;
            jQuery('.execRerun').show();
            jQuery('#runFormExec').focus();
        }
        function runError(msg){
            jQuery('.errormessage').html(msg);
            jQuery('#runerror').collapse('show');
            jQuery('#runcontent').hide();
            onRunComplete();
        }
        function requestFailure(trans){
            runError("Request failed: "+trans.statusText);
        }
        var running=false;
        /**
         * Run the command
         * @param elem
         */
        function runFormSubmit(elem){
            if(running || !$F('runFormExec')){
                return false;
            }
            if(!nodeFilter.filter() && !nodeFilter.filterName()){
                //no node filter
                return false;
            }
            var data = Form.serialize(elem);
            disableRunBar(true);
            runStarted();
            $('runcontent').loading('Starting Execution&hellip;');
            new Ajax.Request("${createLink(controller:'scheduledExecution',action:'runAdhocInline')}",{
                parameters:data,
                evalScripts:true,
                evalJSON:true,
                onSuccess: function(transport) {
                    var data =transport.responseJSON;
//                    alert("data: "+data);
                    try{
                    startRunFollow(data);
                    }catch(e){
                        console.log(e);
                        runError(e);
                    }
                },
                onFailure:requestFailure
            });
            return false;
        }
        /**
         * Load content view to contain output
         * @param data
         */
        function startRunFollow(data){
            if(data.error){
                runError(data.error);
            }else if(!data.id){
                runError("Server response was invalid: "+data.toString());
            }else {
                $('runcontent').loading('Loading Output&hellip;');
                new Ajax.Updater('runcontent',"${createLink(controller:'execution',action:'followFragment')}",{
                parameters:{id:data.id,mode:'tail'},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('runcontent');
//                        try{
                        continueRunFollow(data);
//                        }catch(e){
//                            console.log(e,e);
//                            runError(e);
//                        }
                    }
                },
                onFailure:requestFailure
            });
            }
        }
        /**
         * Start following the output
         * @param data
         */
        function continueRunFollow(data){
             var followControl = new FollowControl(data.id,'runcontent',{
                 parentElement: 'commandPerform',
                 viewoptionsCompleteId: 'viewoptionscomplete',
                 cmdOutputErrorId: 'cmdoutputerror',
                 outfileSizeId: 'outfilesize',
                extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>",
                smallIconUrl: "${resource(dir: 'images', file: 'icon-small')}",
                iconUrl: "${resource(dir: 'images', file: 'icon-small')}",
                lastlines: ${params.lastlines ? params.lastlines : defaultLastLines},
                maxLastLines: ${maxLastLines},
                 showFinalLine: {value: false, changed: false},
                 colStep:{value:false},
                tailmode: true,
                 taildelay:1,
                 truncateToTail:true,
                execData: {node:"test"},
                appLinks:appLinks,
                onComplete:onRunComplete,
                dobind:true
            });
            followControl.beginFollowingOutput(data.id);
        }
        function onRunComplete(){
            enableRunBar();
            afterRun();
        }

        var nodeFilter;

        /**
         * Handle embedded content updates
         */
        function _updateBoxInfo(name,data){
            if(name=='nodetable'){
                if(data.total && data.total!="0" && !running){
                    enableRunBar();
                }else if(!running){
                    disableRunBar(false);
                }
                if (null != data.total && typeof(nodeFilter) != 'undefined') {
                    nodeFilter.total(data.total);
                }
                if (null != data.allcount) {
                    if (typeof(nodeFilter) != 'undefined') {
                        nodeFilter.allcount(data.allcount);
                    }
                }
                if (null != data.filter) {
                    if (typeof(nodeFilter) != 'undefined') {
                        nodeFilter.filter(data.filter);
                    }
                }
                if(null !=data.total){
                    $$('.obs_nodes_page_total').each(function(e){
                        e.innerHTML=data.total;
                    });
                    $$('.obs_nodes_allcount').each(function(e){
                        e.innerHTML=data.total;
                    });
                    $$('.obs_nodes_allcount_plural').each(function (e) {
                        e.innerHTML = data.total == 1 ? '' : 's';
                    });
                }
            }
        }

        /**
         * override action of the filter input text field to load via ajax
         * @private
         */
        function _matchNodes(){
            //use form field
            loadNodeFilter(null, nodeFilter.filter(), nodeFilter.filterAll());
            return false;
        }
        /**
         * node filter link action
         * @param e
         */
        function selectNodeFilterLink(e){
            jQuery(e).addClass('active');
            var filterName = jQuery(e).data('node-filter-name');
            var filterString = jQuery(e).data('node-filter');
            var filterAll = jQuery(e).data('node-filter-all');
            loadNodeFilter(filterName,filterString,filterAll);
        }
        /**
         * load either filter string or saved filter
         * @param filterName name of saved filter
         * @param filterString string filter
         * @param filterAll if true, "all nodes" was selected
         * @param elem target element
         * @param page number to load
         */
        function loadNodeFilter(filterName, filterString, filterAll, elem, page) {
            jQuery('.nodefilterlink').removeClass('active');
            if (!page) {
                page = 0;
            }
            if (!elem) {
                elem = '${ukey}nodeForm';
            }
            if (!filterName && !filterString && null == filterAll) {
                filterName = nodeFilter.filterName();
                filterString = nodeFilter.filter();
                filterAll = nodeFilter.filterAll();
            }
            if (!filterName && !filterString) {
                //if blank input and no filtername selected, do nothing
                return;
            }
            nodespage = page;
            var view = page == 0 ? 'table' : 'tableContent';
            var data = filterName ? {filterName: filterName} : {filter: filterString};
            if (filterName) {
                jQuery('a[data-node-filter-name=\'' + filterName + '\']').addClass('active');
                jQuery('.hiddenNodeFilter').val(filterString);
                jQuery('.hiddenNodeFilterName').val(filterName);
            } else {
                jQuery('.hiddenNodeFilter').val(filterString);
                jQuery('.hiddenNodeFilterName').val('');
            }
            nodeFilter.filterAll(filterAll);
            nodeFilter.filterName(filterName);
            nodeFilter.filter(filterString);
            _updateMatchedNodes(data, elem, '${session.project}', false, {view: view, expanddetail: true, inlinepaging: true,
                page: page, max: pagingMax}, function (xht) {
            });
        }

        /**
         * START page init
         */
        function init() {
            jQuery('.act_setinlinenodefilter').click(function (e) {
                //apply new filter
                _matchNodes();
            });
            jQuery('#nodesContent').on('click','.nodefilterlink',function (evt) {
                evt.preventDefault();
                selectNodeFilterLink(this);
            });
            jQuery('#nodesContent').on('click', '.closeoutput', function (evt) {
                evt.preventDefault();
                jQuery('#runcontent').hide();
            });
            $$('#runbox input').each(function(elem){
                if(elem.type=='text'){
                    elem.observe('keypress',function(evt){
                        if(!noenter(evt)){
                            runFormSubmit('runbox');
                            return false;
                        }else{
                            return true;
                        }
                    });
                }
            });

            //history tabs binding
            var ajaxHistoryLink="${g.createLink(controller: 'reports', action: 'eventsAjax', absolute: true)}";
            var history = new History(ajaxHistoryLink);
            ko.applyBindings(history, document.getElementById('activity_section'));
            setupActivityLinks('activity_section', history, ajaxHistoryLink);
            //if empty query, automatically load first activity_link
            if("${emptyQuery}"=='true'){
                jQuery('ul.activity_links > li:first-child').addClass('active');
                jQuery('ul.activity_links > li:first-child > a').each(function(e){
                    loadHistoryLink(history, ajaxHistoryLink, this.getAttribute('href'));
                });
            }

            //setup node filters knockout bindings
            var filterParams =${[filterName:params.filterName,filter:query?.filter,filterAll:params.showall in ['true',true]].encodeAsJSON()};
            nodeFilter = new NodeFilters("${g.createLink(action: 'adhoc',controller: 'framework',params:[project:session.project])}",
                    "${g.createLink(action: 'create',controller: 'scheduledExecution',params:[project:session.project])}",
                    Object.extend(filterParams, {
                        nodesTitleSingular: "${g.message(code:'Node',default:'Node')}",
                        nodesTitlePlural: "${g.message(code:'Node.plural',default:'Nodes')}"
                    }));
            ko.applyBindings(nodeFilter,document.getElementById('tabsarea'));
            nodeFilter.filter.subscribe(function (newValue) {
                if (newValue == '') {
                    nodeFilter.filterAll(true);
                }
            });
            nodeFilter.runCommand = function () {
                //select run tab
                jQuery('ul > li > a[href=#runtab]').click();
                jQuery('#runFormExec').focus();
            };
            jQuery('#searchForm').submit(_matchNodes);
            _matchNodes();
        }
        jQuery(document).ready(init);

    </script>
    <style type="text/css">
        #runerror{
            margin:5px 0;
        }

        .commandcontent{
            margin:0;
        }

        table.execoutput {
            font-size: 100%;
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
        <div>
            <div class="row ">
                <div class="col-sm-12" id="tabsarea">
                    <ul class="nav nav-tabs">
                        <li class="${emptyQuery?'active':''}">
                            <a href="#nodeFilterInline" data-toggle="tab" >
                                <span data-bind="text: allcount()"></span>
                                <span data-bind="text: nodesTitle()">Node</span>
                            </a>
                        </li>
                        <li class="${emptyQuery ? '' : 'active'}">
                            <a href="#runtab" data-toggle="tab">Run</a>
                        </li>
                    </ul>
                    <div class="tab-content ">
                    <div class="tab-pane  ${emptyQuery ? 'active' : ''}" id="nodeFilterInline">
                        <div class="row row-space">
                        <div class="col-sm-12">
                        <g:form action="adhoc" class="form form-inline" name="searchForm">
                        <g:hiddenField name="max" value="${max}"/>
                        <g:hiddenField name="offset" value="${offset}"/>
                        <g:hiddenField name="formInput" value="true"/>
                        <g:set var="filtvalue"
                               value="${query?.('filter')?.encodeAsHTML()}"/>

                        <div class="form-group">
                                <span class="input-group">
                                    %{--Filter navigation/selection dropdown--}%
                                    <span class="input-group-btn">
                                        <button type="button" class="btn btn-default dropdown-toggle"
                                                data-toggle="dropdown">Filter <span
                                                class="caret"></span></button>
                                        <ul class="dropdown-menu">

                                            <li>
                                                <g:link class="nodefilterlink"
                                                        action="nodes" controller="framework"
                                                        data-node-filter=".*"
                                                        data-node-filter-all="true"
                                                        params="[showall: 'true']">
                                                    All nodes
                                                </g:link>
                                            </li>
                                            <li class="divider"></li>
                                            <li class="dropdown-header"><i
                                                    class="glyphicon glyphicon-filter"></i> Saved Filters</li>
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
                                           value="${filtvalue}" id="schedJobNodeFilter"/>


                                    <span class="input-group-btn">
                                        <a class="btn btn-info" data-toggle='popover-for'
                                           data-target="#schedJobNodeFilter">
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
                        </div>
                        <div class="row row-space">
                            <div class="col-sm-12">
                                <span data-bind="if: allcount()>0" class="pull-right">
                                    <a href="#" data-bind="click: runCommand" class="btn btn-sm btn-default">
                                        <i class="glyphicon glyphicon-play"></i>
                                        Run a command on <span data-bind="text: allcount">${total}</span>
                                        <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span> …
                                    </a>
                                </span>
                                <span id="${ukey}nodeForm" >
                                </span>
                            </div>
                        </div>
                    </div>
                    <div class="tab-pane ${emptyQuery ? '' : 'active'} " id="runtab">
                        <div class="row row-space">
                            <div class="col-sm-12">
                            <g:if test="${run_authorized}">


                                <div class=" form-inline clearfix" id="runbox">
                                        <g:hiddenField name="project" value="${session.project}"/>

                                        <g:render template="nodeFiltersHidden" model="${[params: params, query: query]}"/>
                                    <div class="input-group">
                                        <g:textField name="exec" size="50" placeholder="Enter a shell command"
                                                     value="${runCommand}"
                                                     id="runFormExec"
                                                     class="form-control"
                                                     autofocus="true"/>

                                        <span class="input-group-btn">
                                            <button class="btn btn-default has_tooltip" type="button"
                                                    title="Node Dispatch Settings"
                                                    data-placement="left"
                                                    data-container="body"
                                                    data-toggle="collapse" data-target="#runconfig">
                                                <i class="glyphicon glyphicon-cog"></i>
                                            </button>

                                            <button class="btn btn-success runbutton " onclick="runFormSubmit('runbox');" data-loading-text="Running…">
                                                Run <span class="glyphicon glyphicon-play"></span>
                                            </button>
                                        </span>
                                    </div>

                                <div class="collapse well well-sm " id="runconfig">
                                        <div class="row">
                                            <div class="col-sm-12">
                                                <div class="form-group text-muted ">Node Dispatch Settings:</div>

                                                <div class="form-group has_tooltip"
                                                     title="Maximum number of parallel threads to use"
                                                     data-placement="bottom">
                                                    Thread count
                                                </div>

                                                <div class="form-group">
                                                    <input min="1" type="number" name="nodeThreadcount" id="runNodeThreadcount"
                                                           size="2"
                                                           placeholder="Maximum threadcount for nodes" value="1"
                                                           class="form-control  input-sm"/>
                                                </div>

                                                <div class="form-group">On node failure:</div>

                                                <div class="radio">
                                                    <label class="has_tooltip" title="Continue to execute on other nodes"
                                                           data-placement="bottom">
                                                        <input type="radio" name="nodeKeepgoing"
                                                               value="true"
                                                               checked/> <strong>Continue</strong>
                                                    </label>
                                                </div>

                                                <div class="radio">
                                                    <label class="has_tooltip" title="Do not execute on any other nodes"
                                                           data-placement="bottom">
                                                        <input type="radio" name="nodeKeepgoing"
                                                               value="false"/> <strong>Stop</strong>
                                                    </label>
                                                </div>

                                                <div class="pull-right">
                                                    <button class="close " data-toggle="collapse"
                                                            data-target="#runconfig">&times;</button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class=" alert alert-warning collapse" id="runerror" >
                                    <span class="errormessage"></span>
                                    <a class="close" data-toggle="collapse" href="#runerror" aria-hidden="true">&times;</a>
                                </div>
                                <div class="spacing alert alert-warning" id="emptyerror"
                                    style="display: none"
                                     data-bind="visible: !allcount() || allcount()==0">
                                    <span class="errormessage">
                                        No nodes selected. Select nodes by choosing a filter in the Nodes tab.
                                    </span>
                                </div>

                                <div id="runcontent" class="panel panel-default nodes_run_content" style="display: none"></div>
                            </g:if>
                            </div>
                        </div>
                    </div>
                </div>


            </div>
        </div>




    <g:if test="${run_authorized}">
    <div class="row" id="activity_section">
    <div class="col-sm-12">
        <h4 class="text-muted"><g:message code="page.section.Activity.for.adhoc.commands" /></h4>
        <g:render template="/reports/activityLinks" model="[filter: [
                jobIdFilter: 'null',
                userFilter: session.user,
                projFilter: session.project
        ],
        knockoutBinding:true, showTitle:true]"/>
    </div>
    </div>
    </g:if>

</div>
<div id="loaderror"></div>
</body>
</html>
