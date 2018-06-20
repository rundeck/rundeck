%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<%@ page import="grails.util.Environment; rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <g:set var="projectName" value="${params.project ?: request.project}"></g:set>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}"/>
    <title><g:message code="gui.menu.Nodes"/> - <g:enc>${projectLabel}</g:enc></title>
    <asset:javascript src="framework/nodes.js"/>
    <g:if test="${grails.util.Environment.current==grails.util.Environment.DEVELOPMENT}">
        <asset:javascript src="nodeFiltersKOTest.js"/>
    </g:if>
    <g:embedJSON id="filterParamsJSON"
                 data="${[filterName: params.filterName, filter: query?.filter, filterAll: params.showall in ['true', true]]}"/>
    <g:embedJSON id="pageParams"
                 data="${[pagingMax:params.int('max')?:20, project:params.project?:request.project]}"/>

    <g:jsMessages code="Node,Node.plural"/>
</head>
<body>

<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>

<g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN,project: params.project ?: request.project)}"/>
<g:set var="job_create_authorized" value="${auth.resourceAllowedTest(kind:'job', action: AuthConstants.ACTION_CREATE,project: params.project ?: request.project)}"/>

<div id="nodesContent">

  <g:render template="/common/messages"/>

  <div class=" collapse" id="queryFilterHelp">
      <div class="help-block">
          <g:render template="/common/nodefilterStringHelp"/>
      </div>
  </div>

  <div class="container-fluid">
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <!-- <div class="card-header">
            <h4 class="card-title">
              <g:message code="browse" />
            </h4>
          </div> -->
          <div class="card-content">
            <div class="row">
              <div class="col-xs-12">
                <div class="input-group multiple-control-input-group input-group-lg" style="margin-bottom:0;">
                  <g:render template="nodeFilterInputGroup" model="[filterset: filterset, filtvalue:filtvalue,filterName:filterName]"/>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <div class="card-content vue-tabs">
<!-- NAV TABS -->
        <div class="nav-tabs-navigation">
          <div class="nav-tabs-wrapper" style="width:100%;">
            <ul class="nav nav-tabs">
                <li class="active" id="tab_link_summary">
                    <a href="#summary" data-toggle="tab">
                        <g:message code="browse" />
                    </a>
                </li>
                <li id="tab_link_result" data-bind="visible: filterIsSet()||allcount()>=0">
                    <a href="#result" data-toggle="tab" data-bind="visible: filterIsSet() ">
                        <g:message code="result" />
                        <span data-bind="visible: allcount()>=0">
                            <span data-bind="text: allcount" class="text-info">${total}</span>
                            <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span>
                        </span>
                        <span data-bind="visible: allcount()<0" class="text-primary">&hellip;</span>

                    </a>
                    <a href="#" data-bind="visible: !filterIsSet() ">
                        <g:message code="enter.a.filter" />
                    </a>
                </li>
                <li data-bind="visible: filterIsSet()||allcount()>=0" class="pull-right">
                    <span class="tabs-sibling tabs-sibling-compact">
                      <div class="btn-group pull-right ">
                          <button class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                              <g:message code="actions" /> <span class="caret"></span>
                          </button>
                          <ul class="dropdown-menu" role="menu">

                              <li class="dropdown-header" data-bind="visible: filterName()">
                                  <g:message code="filter" /><span data-bind="text: filterNameDisplay()"></span>
                              </li>
                              <li data-bind="visible: canSaveFilter">
                                  <a href="#"
                                     data-toggle="modal"
                                     data-target="#saveFilterModal">
                                      <i class="glyphicon glyphicon-plus"></i>
                                      <g:message code="save.filter.ellipsis" />
                                  </a>
                              </li>
                              <li data-bind="visible: canDeleteFilter">
                                  <a href="#"
                                     class="textbtn textbtn-danger"
                                     data-bind="click: deleteFilter">
                                      <i class="glyphicon glyphicon-remove"></i>
                                      <g:message code="delete.this.filter.ellipsis" />
                                  </a>
                              </li>
                              <li data-bind="visible: canSetDefaultFilter">
                                  <a href="#"
                                     class="textbtn textbtn-success"
                                     data-bind="click: setDefaultFilter">
                                      <i class="glyphicon glyphicon-filter"></i>
                                      <g:message code="set.as.default.filter" />
                                  </a>
                              </li>
                              <li data-bind="visible: canRemoveDefaultFilter">
                                  <a href="#"
                                     class="btn btn-default"
                                     data-bind="click: nodeSummary().removeDefault">
                                      <i class="glyphicon glyphicon-ban-circle"></i>
                                      <g:message code="remove.default.filter" />
                                  </a>
                              </li>
                              <li class="divider" ></li>
                              <g:if test="${g.executionMode(is:'active',project:params.project)}">

                                  <li data-bind="visible: hasNodes()" class="${run_authorized?'':'disabled'}">
                                      <a href="#" data-bind="${run_authorized?'click: runCommand':''}"
                                         title="${run_authorized ? '' : message(code:"not.authorized")}"
                                         class="${run_authorized ? '' : 'has_tooltip'}"
                                         data-placement="left"
                                      >
                                          <i class="glyphicon glyphicon-play"></i>
                                          <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="run.a.command.on.count.nodes.ellipsis" /></span>
                                      </a>
                                  </li>
                              </g:if>
                              <g:else>

                                  <li data-bind="visible: hasNodes()" class="disabled">
                                      <a href="#"
                                         title="${g.message(code:'disabled.execution.run')}"
                                         class="has_tooltip"
                                         data-placement="left"
                                      >
                                          <i class="glyphicon glyphicon-play"></i>
                                          <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="run.a.command.on.count.nodes.ellipsis" /></span>
                                      </a>
                                  </li>
                              </g:else>

                              <li class="${job_create_authorized?'':'disabled'}">
                                  <a href="#" data-bind="${job_create_authorized?'click: saveJob':''}"
                                     title="${job_create_authorized?'':message(code:"not.authorized")}"
                                     class="${job_create_authorized?'':'has_tooltip'}"
                                     data-placement="left"
                                  >
                                      <i class="glyphicon glyphicon-plus"></i>
                                      <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="create.a.job.for.count.nodes.ellipsis" /></span>
                                  </a>
                              </li>
                          </ul>
                      </div>
                    </span>
                </li>
            </ul>
          </div>
        </div>



<!-- TABS -->
            <div class="tab-content">
                <div class="tab-pane " id="result">
                    <div class="row row-space">
                        <div class="col-sm-12">

                            <span data-bind="if: error()"  class="text-danger">
                                <i class="glyphicon glyphicon-warning-sign"></i>
                                <span data-bind="text: error()"></span>
                            </span>
                            <g:if test="${tagsummary}">
                                <g:render template="tagsummary"
                                          model="${[hidetop:!summaryOnly,tagsummary: tagsummary, link: [action: 'nodes', controller: 'framework', param: 'nodeIncludeTags']]}"/>
                            </g:if>
                            <g:elseif test="${tagsummary?.size() == 0}">
                            %{--<span class="text-primary">no tags</span>--}%
                        </g:elseif>

                    </div>
                    <g:form class="form form-inline" action="adhoc" controller="framework" method="get" name="runform">
                        <g:hiddenField name="project" value="${params.project ?: request.project}"/>
                        <g:render template="nodeFiltersHidden" model="${[params: params, query: query]}"/>
                    </g:form>

                </div>
                <div class=" clear matchednodes " id="nodelist" >
                </div>
                <div class="clear matchednodes" id="nodeview">
                    <g:render template="allnodesKO" />
                </div>
            </div>
            <div class="tab-pane active" id="summary">

                <div class="row">

                    <div class="col-xs-6">
                        <h5 class="column-title text-uppercase text-primary">
                          <g:message code="resource.metadata.entity.tags" />
                        </h5>
                        <ul data-bind="foreach: nodeSummary().tags" class="list-unstyled">
                            <li style="display:inline;">
                                <node-filter-link class="label label-default" params="
                                    filterkey: 'tags',
                                    filterval: tag,
                                    tag: tag,
                                    count: count
                                ">
                                </node-filter-link>
                            </li>
                        </ul>
                        <div data-bind="visible: !nodeSummary().tags">
                            <g:message code="none" />
                        </div>
                    </div>

                    <div class="col-xs-6">

                        <h5 class="column-title text-uppercase text-primary">
                          <g:message code="filters" />
                        </h5>

                        <ul class="list-unstyled">
                            <li>
                                <a href="#" class="nodefilterlink btn btn-default btn-xs" data-node-filter=".*" data-node-filter-all="true">
                                  <g:message code="all.nodes" />
                                  <span data-bind="text: nodeSummary().totalCount">0</span>
                                </a>
                                <div class="btn-group">
                                    <button type="button"
                                            class="btn btn-default btn-xs btn-simple dropdown-toggle"
                                            style="padding: 0 5px;"
                                            title="Filter Actions"
                                            data-toggle="dropdown"
                                            aria-expanded="false">
                                        <span class="caret"></span>
                                    </button>
                                    <ul class="dropdown-menu" role="menu">
                                        <li data-bind="visible: '.*'!=$root.nodeSummary().defaultFilter()">
                                            <a href="#"
                                               data-bind="click: $root.nodeSummary().setDefaultAll">

                                                <i class="glyphicon glyphicon-filter"></i>
                                                <g:message code="set.all.nodes.as.default.filter" />
                                            </a>
                                        </li>
                                        <li data-bind="visible: '.*'==$root.nodeSummary().defaultFilter()">
                                            <a href="#"
                                               data-bind="click: $root.nodeSummary().removeDefault">
                                                <i class="glyphicon glyphicon-ban-circle"></i>
                                                <g:message code="remove.all.nodes.as.default.filter" />
                                            </a>
                                        </li>
                                    </ul>
                                </div>
                            </li>

                            <!-- ko foreach: nodeSummary().filters -->
                            <li>
                                <a href="#" class=" nodefilterlink btn btn-default" data-bind="attr: { 'data-node-filter-name': name(), 'data-node-filter': filter(), 'title': filter(), 'href': $root.nodeSummary().linkForFilterName($data)}">
                                    <span data-bind="text: name"></span>
                                </a>
                                <div class="btn-group">
                                    <button type="button"
                                            class="btn btn-default btn-sm btn-simple dropdown-toggle"
                                            title="Filter Actions"
                                            data-toggle="dropdown"
                                            aria-expanded="false">
                                        <span class="caret"></span>
                                    </button>
                                    <ul class="dropdown-menu" role="menu">
                                        <li>
                                            <a href="#"
                                               data-bind="click: $root.nodeSummary().deleteFilterConfirm">
                                                <i class="glyphicon glyphicon-remove"></i>
                                                <g:message code="delete.this.filter.ellipsis" />
                                            </a>
                                        </li>
                                        <li data-bind="visible: name()!=$root.nodeSummary().defaultFilter()">
                                            <a href="#"
                                               data-bind="click: $root.nodeSummary().setDefault">

                                                <i class="glyphicon glyphicon-filter"></i>
                                                <g:message code="set.as.default.filter" />
                                            </a>
                                        </li>
                                        <li data-bind="visible: name()==$root.nodeSummary().defaultFilter()">
                                            <a href="#"
                                               data-bind="click: $root.nodeSummary().removeDefault">
                                                <i class="glyphicon glyphicon-ban-circle"></i>
                                                <g:message code="remove.default.filter" />
                                            </a>
                                        </li>
                                    </ul>
                                </div>

                            </li>
                            <!-- /ko -->
                        </ul>
                        <div data-bind="visible: !nodeSummary().filters">
                            <g:message code="none" />
                        </div>

                    </div>
                </div>
            </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  </div>
</div>

    %{--Form for saving/deleting node filters--}%

        <g:form class="form form-horizontal" useToken="true">
            <g:hiddenField name="project" value="${params.project}"/>
            <g:render template="nodeFiltersHidden"/>
            <g:render template="/common/queryFilterManagerModal"
                      model="${[rkey: ukey, filterName: filterName, filterset: filterset, filterLinks: true, formId: '${ukey}filter', ko: true, deleteActionSubmit: 'deleteNodeFilter', storeActionSubmit: 'storeNodeFilter']}"/>
        </g:form>
</body>
</html>
