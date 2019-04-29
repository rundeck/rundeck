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
    <g:set var="projectName" value="${params.project ?: request.project}"></g:set>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}"/>
    <title><g:message code="gui.menu.Adhoc"/> - <g:enc>${projectLabel}</g:enc></title>
    <asset:javascript src="executionState.js"/>
    <asset:javascript src="executionControl.js"/>
    <asset:javascript src="util/yellowfade.js"/>
    <asset:javascript src="pagehistory.js"/>
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
            runCommand:runCommand?:'',
            adhocKillAllowed:auth.adhocAllowedTest(action: AuthConstants.ACTION_KILL,project:params.project)
    ]}"/>
    <g:jsMessages code="Node,Node.plural"/>
</head>
<body>
<div class="container-fluid page-commands">
  <g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
  </g:if>
  <div id="nodesContent" class="row">
    <g:render template="/common/messages"/>
    <div id="tabsarea" class="col-xs-12">
      <div class="card">
        <g:ifExecutionMode active="true" project="${params.project ?: request.project}">
          <div class="card-content">
            <div class="" id="runtab">
              <div class="" id="runbox">
                <g:jsonToken id="adhoc_req_tokens" url="${request.forwardURI}"/>
                  <g:form  action="adhoc" params="[project:params.project]">
                    <div id="nodefiltersHidden">
                      <g:render template="nodeFiltersHidden" model="${[params: params, query: query]}"/>
                    </div>
                    <div id="adhocInput">
                      <span class="input-group multiple-control-input-group">
                        <span class="input-group-addon input-group-addon-title"><g:message code="command.prompt" /></span>
                        <span class="input-group-btn">
                          <button type="button" class="btn btn-default dropdown-toggle act_adhoc_history_dropdown" data-toggle="dropdown">
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
                              <a href="#" data-bind="attr: { href: href, title: filter }, click: fillCommand" class="act_fill_cmd">
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
                      <div class="collapse well well-sm" id="runconfig">
                        <div class="form form-inline">
                            <h5 style="margin-top:0;">
                                <g:message code="node.dispatch.settings" />
                                <div class="pull-right">
                                  <button class="close " data-toggle="collapse" data-target="#runconfig">&times;</button>
                                </div>
                            </h5>
                            <div class="">
                              <div class="form-group has_tooltip" style="margin-top:6px;"
                                   title="${message(code:"maximum.number.of.parallel.threads.to.use")}"
                                   data-placement="bottom">
                                  <g:message code="thread.count" />
                              </div>
                              <div class="form-group" style="margin-top:6px;">
                                <input min="1" type="number" name="nodeThreadcount"
                                       id="runNodeThreadcount"
                                       size="2"
                                       placeholder="${message(code:"maximum.threadcount.for.nodes")}" value="1"
                                       class="form-control  input-sm"/>
                              </div>
                              <div class="form-group" style="margin-top:6px; margin-left:20px">
                                  <g:message code="on.node.failure" />
                              </div>
                              <div class="form-group">
                                <div class="radio">
                                  <input type="radio" name="nodeKeepgoing" value="true" checked/>
                                  <label class="has_tooltip" title="${message(code:"continue.to.execute.on.other.nodes")}" data-placement="bottom">
                                    &nbsp;&nbsp;<g:message code="continue" />
                                  </label>
                                </div>
                              </div>
                              <div class="form-group">
                                <div class="radio">
                                  <input type="radio" name="nodeKeepgoing" value="false"/>
                                  <label class="has_tooltip" title="${message(code:"do.not.execute.on.any.other.nodes")}" data-placement="bottom">
                                    &nbsp;&nbsp;<g:message code="stop" />
                                  </label>
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
                          <div class="col-sm-12">
                            <div class="input-group multiple-control-input-group">
                              <g:render template="nodeFilterInputGroup" model="[filterset: filterset, filtvalue: filtvalue, filterName: filterName, showInputTitle:true]"/>
                            </div>
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
                <div class="row">
                  <div class="col-xs-12">
                    <div class="spacing text-warning" id="emptyerror" style="display: none" data-bind="visible: !loading() && !error() && (!total() || total()==0)">
                      <span class="errormessage">
                        <g:message code="no.nodes.selected.match.nodes.by.selecting.or.entering.a.filter" />
                      </span>
                    </div>
                    <div class="spacing text-danger" id="loaderror2" style="display: none" data-bind="visible: error()">
                      <i class="glyphicon glyphicon-warning-sign"></i>
                      <span class="errormessage" data-bind="text: error()"></span>
                    </div>
                    <div data-bind="visible: total()>0 || loading()" class="well inline">
                      <span data-bind="if: loading()" class="text-info">
                        <i class="glyphicon glyphicon-time"></i>
                        <g:message code="loading.matched.nodes" />
                      </span>
                      <span data-bind="if: !loading() && !error()">
                        <span data-bind="messageTemplate: [ total(), nodesTitle() ]"><g:message code="count.nodes.matched" /></span>.

                        <span data-bind="if: total()>maxShown()">
                        <span data-bind="messageTemplate: [maxShown(), total()]" class="text-primary"><g:message code="count.nodes.shown" /></span>
                        </span>
                        <div class="pull-right" style="margin-top:-5px">
                          <a class="btn btn-default btn-sm" data-bind="click: nodesPageView">
                              <g:message code="view.in.nodes.page.prompt" />
                          </a>
                        </div>

                      </span>
                    </div>
                    <g:render template="nodesEmbedKO"/>
                  </div>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <div class="" id="actionButtonArea" >
                <a class="btn btn-success btn-fill runbutton btn-block" data-bind="attr: { disabled: total()<1 || error() } " onclick="runFormSubmit('runbox');" data-loading-text="${message(code:"running1")}">
                  <span data-bind="messageTemplate: [ total(), nodesTitle() ] "><g:message code="run.on.count.nodes" /></span>
                  <span class="glyphicon glyphicon-play"></span>
                </a>
              </div>
            </div>
          </g:ifExecutionMode>
          <g:ifExecutionMode active="false" project="${params.project ?: request.project}">
            <div class="col-sm-12">
              <div class="alert alert-warning ">
                <g:message code="disabled.execution.run"/>
              </div>
            </div>
          </g:ifExecutionMode>
        </div>
        <div class="row row-space">
          <div class="col-sm-12">
            <div class="alert alert-warning collapse" id="runerror">
              <span class="errormessage"></span>
              <a class="close" data-toggle="collapse" href="#runerror" aria-hidden="true">&times;</a>
            </div>
            <div id="runcontent" class="card card-modified card-grey-header nodes_run_content" style="display: none"></div>
          </div>
        </div>
      </div>
      <div id="activity_section">
        <div class="col-sm-12">
          <div class="card card-plain">
            <div class="card-header">
              <h3 class="card-title">
                <g:message code="page.section.Activity.for.adhoc.commands" />
              </h3>
            </div>
          </div>
          <div class="card">
            <div class="card-content">
              <g:render template="/reports/activityLinks" model="[filter: [
                      jobIdFilter: 'null',
                      userFilter: session.user,
                      projFilter: params.project ?: request.project
              ],
              knockoutBinding:true, showTitle:true]"/>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div id="loaderror"></div>
</div>
</body>
</html>
