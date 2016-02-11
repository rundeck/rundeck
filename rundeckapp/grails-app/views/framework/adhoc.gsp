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

<%@ page import="grails.util.Environment; rundeck.User" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="adhoc"/>
    <title><g:message code="gui.menu.Adhoc"/> - <g:enc>${params.project ?: request.project}</g:enc></title>
    <g:javascript library="executionControl"/>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <asset:javascript src="framework/adhoc.js"/>
    <g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
    <g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>
    <g:embedJSON id="filterParamsJSON" data="${[filterName: params.filterName, filter: query?.filter, filterAll: params.showall in ['true', true]]}"/>
    <g:embedJSON id="pageParams" data="${[
            disableMarkdown: params.boolean('disableMarkdown') ? '&disableMarkdown=true' :'',
            smallIconUrl:resource(dir: 'images', file: 'icon-small'),
            iconUrl:resource(dir: 'images', file: 'icon-small'),
            lastlines:params.int('lastlines')?: defaultLastLines,
            maxLastLines:params.int('maxlines')?: maxLastLines,
            emptyQuery:emptyQuery?:null,
            ukey:ukey,
            project:params.project?:request.project,
            runCommand:runCommand?:''
    ]}"/>
    <g:jsMessages code="Node,Node.plural"/>
</head>
<body>

<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>

<div id="nodesContent">


    <g:render template="/common/messages"/>
        <div id="tabsarea">
            <div class="row ">
                <g:ifExecutionMode active="true">
                <div class="col-sm-10" >
                    <div class="" id="runtab">
                            <div class="form form-horizontal clearfix" id="runbox">
                                <g:jsonToken id="adhoc_req_tokens" url="${request.forwardURI}"/>
                                <g:form  action="adhoc" params="[project:params.project]">
                                    <div id="nodefiltersHidden">
                                <g:render template="nodeFiltersHidden"
                                          model="${[params: params, query: query]}"/>
                                    </div>
                                <div class="form-group ">
                                <label class="col-sm-2 text-right form-control-static" for="runFormExec"><g:message code="command.prompt" /></label>
                                <div class=" col-sm-10"  id="adhocInput">
                                    <span class="input-group">
                                        <span class="input-group-btn">
                                            <button type="button"
                                                    class="btn btn-default dropdown-toggle act_adhoc_history_dropdown"
                                                    data-toggle="dropdown">
                                                <g:message code="recent" /> <span class="caret"></span>
                                            </button>
                                            <ul class="dropdown-menu" >

                                                <!-- ko if: recentCommandsNoneFound() -->
                                                    <li role="presentation" class="dropdown-header"><g:message code="none" /></li>
                                                <!-- /ko -->

                                                <!-- ko if: !recentCommandsLoaded() -->
                                                    <li role="presentation" class="dropdown-header"><g:message code="loading.text" /></li>
                                                <!-- /ko -->

                                                <!-- ko if: recentCommandsLoaded() && !recentCommandsNoneFound() -->
                                                <li role="presentation" class="dropdown-header"><g:message code="your.recently.executed.commands" /></li>
                                                <!-- /ko -->

                                                <!-- ko foreach: recentCommands -->
                                                <li>
                                                    <a href="#"
                                                       data-bind="attr: { href: href, title: filter }, click: fillCommand"
                                                       class="act_fill_cmd">

                                                        <i class="exec-status icon" data-bind="css: statusClass"></i>
                                                        <span data-bind="text: title"></span>
                                                    </a>
                                                </li>
                                                <!-- /ko -->

                                            </ul>
                                        </span>
                                    <g:textField name="exec" size="50" placeholder="${message(code:'enter.a.command')}"
                                                 value="${runCommand}"
                                                 id="runFormExec"
                                                 class="form-control"
                                                 data-bind="value: commandString, valueUpdate: 'keyup', enable: allowInput"
                                                 autofocus="true"/>
                                    <g:hiddenField name="doNodedispatch"  value="true"/>

                                    <span class="input-group-btn">
                                        <button class="btn btn-default has_tooltip" type="button"
                                                title="${message(code:"node.dispatch.settings")}"
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
                                            <div class="form-group text-muted "><g:message code="node.dispatch.settings" />:</div>

                                            <div class="form-group has_tooltip"
                                                 title="${message(code:"maximum.number.of.parallel.threads.to.use")}"
                                                 data-placement="bottom">
                                                <g:message code="thread.count" />
                                            </div>

                                            <div class="form-group">
                                                <input min="1" type="number" name="nodeThreadcount"
                                                       id="runNodeThreadcount"
                                                       size="2"
                                                       placeholder="${message(code:"maximum.threadcount.for.nodes")}" value="1"
                                                       class="form-control  input-sm"/>
                                            </div>

                                            <div class="form-group"><g:message code="on.node.failure" /></div>

                                            <div class="radio">
                                                <label class="has_tooltip"
                                                       title="${message(code:"continue.to.execute.on.other.nodes")}"
                                                       data-placement="bottom">
                                                    <input type="radio" name="nodeKeepgoing"
                                                           value="true"
                                                           checked/> <strong><g:message code="continue" /></strong>
                                                </label>
                                            </div>

                                            <div class="radio">
                                                <label class="has_tooltip"
                                                       title="${message(code:"do.not.execute.on.any.other.nodes")}"
                                                       data-placement="bottom">
                                                    <input type="radio" name="nodeKeepgoing"
                                                           value="false"/> <strong><g:message code="stop" /></strong>
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
                    <div id="nodefilterViewArea">
                    <div class="${emptyQuery ? 'active' : ''}" id="nodeFilterInline">
                        <div class="spacing">
                        <div class="">
                        <g:form action="adhoc" class="form form-horizontal" name="searchForm" >
                        <g:hiddenField name="max" value="${max}"/>
                        <g:hiddenField name="offset" value="${offset}"/>
                        <g:hiddenField name="formInput" value="true"/>
                        <g:set var="filtvalue" value="${query?.('filter')}"/>

                            <div class="form-group">
                                <label class="col-sm-2 text-right form-control-static" for="schedJobNodeFilter"><g:message code="nodes" /></label>
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
                                 data-bind="visible: !loading() && !error() && (!total() || total()==0)">
                                <span class="errormessage">
                                    <g:message code="no.nodes.selected.match.nodes.by.selecting.or.entering.a.filter" />
                                </span>
                            </div>
                            <div class="spacing text-danger" id="loaderror2"
                                 style="display: none"
                                 data-bind="visible: error()">
                                <i class="glyphicon glyphicon-warning-sign"></i>
                                <span class="errormessage" data-bind="text: error()">

                                </span>
                            </div>
                            <div data-bind="visible: total()>0 || loading()" class="well well-sm inline">
                                <span data-bind="if: loading()" class="text-info">
                                    <i class="glyphicon glyphicon-time"></i>
                                    <g:message code="loading.matched.nodes" />
                                </span>
                                <span data-bind="if: !loading() && !error()">

                                    <span data-bind="messageTemplate: [ total(), nodesTitle() ]"><g:message code="count.nodes.matched" /></span>.

                                    <span data-bind="if: total()>maxShown()">
                                    <span data-bind="messageTemplate: [maxShown(), total()]" class="text-muted"><g:message code="count.nodes.shown" /></span>
                                    </span>
                                    <a class="textbtn textbtn-default pull-right" data-bind="click: nodesPageView">
                                        <g:message code="view.in.nodes.page.prompt" />
                                    </a>
                                </span>
                            </div>
                            <span >
                                <g:render template="nodesEmbedKO"/>
                            </span>
                        </div>
                    </div>
                    </div>
                </div>
                <div class="col-sm-2" id="actionButtonArea" >

                    <button class="btn btn-success runbutton pull-right"
                            data-bind="attr: { disabled: total()<1 || error() } "
                            onclick="runFormSubmit('runbox');" data-loading-text="${message(code:"running1")}">

                        <span data-bind="messageTemplate: [ total(), nodesTitle() ] "><g:message code="run.on.count.nodes" /></span>

                        <span class="glyphicon glyphicon-play"></span>
                    </button>
                </div>
                </g:ifExecutionMode>
                <g:ifExecutionMode active="false">
                    <div class="col-sm-12">
                        <div class="alert alert-warning ">
                            <g:message code="disabled.execution.run"/>
                        </div>
                    </div>
                </g:ifExecutionMode>


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
