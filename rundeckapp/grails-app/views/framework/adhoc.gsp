%{--
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
  --}%

<%@ page import="grails.util.Environment; rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="adhoc"/>
    <title><g:message code="gui.menu.Nodes"/> - <g:enc>${params.project ?: request.project}</g:enc></title>
    <g:javascript library="executionControl"/>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <asset:javascript src="nodeFilters_HistoryKO.js"/>
    <g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
    <g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>
    <script type="text/javascript">
        function showError(message) {
            appendText($("error"),message);
            $("error").show();
        }

        /**
         * START run execution code
         */
        <g:set var="filterParams" value="${query?.properties.findAll{it.key==~/^(node(In|Ex)clude.*|project)$/ &&it.value}}"/>

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
        var running = false;
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
            var data = jQuery('#'+elem+" :input").serialize();
            disableRunBar(true);
            runStarted();
            $('runcontent').loading('Starting Execution…');
            jQuery.ajax({
                type:'POST',
                url:_genUrl(appLinks.scheduledExecutionRunAdhocInline,data),
                beforeSend:_ajaxSendTokens.curry('adhoc_req_tokens'),
                success:function (data,status,xhr) {
                    try {
                        startRunFollow(data);
                    } catch (e) {
                        console.log(e);
                        runError(e);
                    }
                },
                error:function(data,jqxhr,err){
                    requestFailure(jqxhr);
                }
            }).success(_ajaxReceiveTokens.curry('adhoc_req_tokens'));
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
                $('runcontent').loading('Loading Output…');
                jQuery('#runcontent').load(_genUrl(appLinks.executionFollowFragment, {id: data.id, mode: 'tail'}),function(resp,status,jqxhr){
                    if(status=='success'){
                        Element.show('runcontent');
                        continueRunFollow(data);
                    }else{
                        requestFailure(jqxhr);
                    }
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
                extraParams:"<%="true" == params.boolean('disableMarkdown')? '&disableMarkdown=true' : ''%>",
                smallIconUrl: "${resource(dir: 'images', file: 'icon-small')}",
                iconUrl: "${resource(dir: 'images', file: 'icon-small')}",
                lastlines: ${enc(js:params.int('lastlines')?: defaultLastLines)},
                maxLastLines: ${enc(js:params.int('maxlines')?: maxLastLines)},
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
        }


        /**
         * START page init
         */
        function init() {
            jQuery('body').on('click', '.nodefilterlink', function (evt) {
                evt.preventDefault();
                nodeFilter.selectNodeFilterLink(this);
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
            var history = new History(appLinks.reportsEventsAjax,appLinks.menuNowrunningAjax);
            ko.applyBindings(history, document.getElementById('activity_section'));
            setupActivityLinks('activity_section', history);
            //if empty query, automatically load first activity_link
            if("${enc(js:emptyQuery)}"=='true'){
                history.activateNowRunningTab();
            }

            //setup node filters knockout bindings
            var filterParams =loadJsonData('filterParamsJSON');
            nodeFilter = new NodeFilters(
                    appLinks.frameworkAdhoc,
                    appLinks.scheduledExecutionCreate,
                    appLinks.frameworkNodes,
                    Object.extend(filterParams, {
                        elem: '${ukey}nodeForm',
                        view: 'embed',
                        maxShown:20,
                        emptyMode:'blank',
                        project: '${enc(js:params.project?:request.project)}',
                        nodesTitleSingular: "${enc(js:g.message(code:'Node',default:'Node'))}",
                        nodesTitlePlural: "${enc(js:g.message(code:'Node.plural',default:'Nodes'))}"
                    }));
            ko.applyBindings(nodeFilter,document.getElementById('tabsarea'));

            //show selected named filter
            nodeFilter.filterName.subscribe(function(val){
                if(val){
                    jQuery('a[data-node-filter-name]').removeClass('active');
                    jQuery('a[data-node-filter-name=\'' + val + '\']').addClass('active');
                }
            });
            nodeFilter.updateMatchedNodes();
        }
        jQuery(document).ready(init);

    </script>
    <g:embedJSON id="filterParamsJSON" data="${[filterName: params.filterName, filter: query?.filter, filterAll: params.showall in ['true', true]]}"/>
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


    <g:render template="/common/messages"/>
        <div id="tabsarea">
            <div class="row ">
                <div class="col-sm-10" >
                    <div class="" id="runtab">
                            <div class="form form-horizontal clearfix" id="runbox">
                                <g:jsonToken id="adhoc_req_tokens" url="${request.forwardURI}"/>
                                <g:form  action="adhoc" params="[project:params.project]">
                                <g:render template="nodeFiltersHidden"
                                          model="${[params: params, query: query]}"/>
                                <div class="form-group ">
                                <label class="col-sm-2 text-right form-control-static" for="runFormExec">Command:</label>
                                <div class=" col-sm-10">
                                    <span class="input-group">
                                    <g:textField name="exec" size="50" placeholder="Enter a command"
                                                 value="${runCommand}"
                                                 id="runFormExec"
                                                 class="form-control"
                                                 autofocus="true"/>
                                    <g:hiddenField name="doNodedispatch"  value="true"/>

                                    <span class="input-group-btn">
                                        <button class="btn btn-default has_tooltip" type="button"
                                                title="Node Dispatch Settings"
                                                data-placement="left"
                                                data-container="body"
                                                data-toggle="collapse" data-target="#runconfig">
                                            <i class="glyphicon glyphicon-cog"></i>
                                        </button>

                                    </span>
                                    </span>

                                <div class="collapse well well-sm inline form-inline" id="runconfig">
                                    <div class="row">
                                        <div class="col-sm-12">
                                            <div class="form-group text-muted ">Node Dispatch Settings:</div>

                                            <div class="form-group has_tooltip"
                                                 title="Maximum number of parallel threads to use"
                                                 data-placement="bottom">
                                                Thread count
                                            </div>

                                            <div class="form-group">
                                                <input min="1" type="number" name="nodeThreadcount"
                                                       id="runNodeThreadcount"
                                                       size="2"
                                                       placeholder="Maximum threadcount for nodes" value="1"
                                                       class="form-control  input-sm"/>
                                            </div>

                                            <div class="form-group">On node failure:</div>

                                            <div class="radio">
                                                <label class="has_tooltip"
                                                       title="Continue to execute on other nodes"
                                                       data-placement="bottom">
                                                    <input type="radio" name="nodeKeepgoing"
                                                           value="true"
                                                           checked/> <strong>Continue</strong>
                                                </label>
                                            </div>

                                            <div class="radio">
                                                <label class="has_tooltip"
                                                       title="Do not execute on any other nodes"
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
                            </div>
                            </g:form>
                            </div>

                    </div>
                    <div class="${emptyQuery ? 'active' : ''}" id="nodeFilterInline">
                        <div class="spacing">
                        <div class="">
                        <g:form action="adhoc" class="form form-horizontal" name="searchForm" >
                        <g:hiddenField name="max" value="${max}"/>
                        <g:hiddenField name="offset" value="${offset}"/>
                        <g:hiddenField name="formInput" value="true"/>
                        <g:set var="filtvalue" value="${query?.('filter')}"/>

                            <div class="form-group">
                                <label class="col-sm-2 text-right form-control-static" for="schedJobNodeFilter">Nodes:</label>
                                <div class="col-sm-10">
                                <span class=" input-group" >
                                    <g:render template="nodeFilterInputGroup"
                                              model="[filterset: filterset, filtvalue: filtvalue, filterName: filterName]"/>
                                </span>
                                </div>
                            </div>
                        </g:form>

                        <div class=" collapse" id="queryFilterHelp">
                            <div class="help-block">
                                <g:render template="/common/nodefilterStringHelp"/>
                            </div>
                        </div>
                        </div>
                        </div>

                    </div>

                    <div class="row row-space">
                        <div class="col-sm-10 col-sm-offset-2">
                            <div class="spacing text-warning" id="emptyerror"
                                 style="display: none"
                                 data-bind="visible: !error() && (!allcount() || allcount()==0)">
                                <span class="errormessage">
                                    No nodes selected. Match nodes by selecting or entering a filter.
                                </span>
                            </div>
                            <div class="spacing text-danger" id="loaderror2"
                                 style="display: none"
                                 data-bind="visible: error()">
                                <i class="glyphicon glyphicon-warning-sign"></i>
                                <span class="errormessage" data-bind="text: error()">

                                </span>
                            </div>
                            <div data-bind="visible: allcount()>0 || loading()" class="well well-sm inline">
                                <span data-bind="if: loading()" class="text-info">
                                    <i class="glyphicon glyphicon-time"></i>
                                    <g:message code="loading.matched.nodes" />
                                </span>
                                <span data-bind="if: !loading() && !error()">
                                <span data-bind="text: allcount()">0</span>
                                <span data-bind="text: nodesTitle">Nodes</span> Matched.
                                <a class="textbtn textbtn-default pull-right" data-bind="click: nodesPageView">
                                    View in Nodes Page &raquo;
                                </a>
                                </span>
                            </div>
                            <span id="${enc(attr:ukey)}nodeForm">
                            </span>
                        </div>
                    </div>
                </div>
                <div class="col-sm-2" >

                    <button class="btn btn-success runbutton pull-right"
                            data-bind="attr: { disabled: allcount()<1 || error() } "
                            onclick="runFormSubmit('runbox');" data-loading-text="Running…">
                        Run on <span data-bind="text: allcount">0</span> <span data-bind="text: nodesTitle">Nodes</span> <span class="glyphicon glyphicon-play"></span>
                    </button>
                </div>


            </div>


    <div class="row row-space">
        <div class="col-sm-12">

            <div class=" alert alert-warning collapse" id="runerror">
                <span class="errormessage"></span>
                <a class="close" data-toggle="collapse" href="#runerror"
                   aria-hidden="true">&times;</a>
            </div>

            <div id="runcontent" class="panel panel-default nodes_run_content"
                 style="display: none"></div>
        </div>
    </div>

        </div>




    <div class="row" id="activity_section">
    <div class="col-sm-12">
        <h4 class="text-muted"><g:message code="page.section.Activity.for.adhoc.commands" /></h4>
        <g:render template="/reports/activityLinks" model="[filter: [
                jobIdFilter: 'null',
                userFilter: session.user,
                projFilter: params.project ?: request.project
        ],
        knockoutBinding:true, showTitle:true]"/>
    </div>
    </div>

</div>
<div id="loaderror"></div>
</body>
</html>
