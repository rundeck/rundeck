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
    <title><g:message code="gui.menu.Nodes"/> - <g:enc>${params.project ?: request.project}</g:enc></title>
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
<div id="nodesContent">
    <g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN,project: params.project ?: request.project)}"/>
    <g:set var="job_create_authorized" value="${auth.resourceAllowedTest(kind:'job', action: AuthConstants.ACTION_CREATE,project: params.project ?: request.project)}"/>
    <g:render template="/common/messages"/>

    <div class=" collapse" id="queryFilterHelp">
        <div class="help-block">
            <g:render template="/common/nodefilterStringHelp"/>
        </div>
    </div>

    %{--Form for saving/deleting node filters--}%

        <g:form class="form form-horizontal" useToken="true">
            <g:hiddenField name="project" value="${params.project}"/>
            <g:render template="nodeFiltersHidden"/>
            <g:render template="/common/queryFilterManagerModal"
                      model="${[rkey: ukey, filterName: filterName, filterset: filterset,
                                filterLinks: true,
                                formId: "${ukey}filter",
                                ko: true,
                                deleteActionSubmit: 'deleteNodeFilter', storeActionSubmit: 'storeNodeFilter']}"/>
        </g:form>

        <ul class="nav nav-tabs">
            <li class="active" id="tab_link_summary">
                <a href="#summary" data-toggle="tab">
                    <g:message code="browse" />
                </a>
            </li>

            <li>
                <span class="tabs-sibling tabs-sibling-compact form-inline">
                    <div class="form-group ">
                        <span class="input-group" >
                            <g:render template="nodeFilterInputGroup" model="[filterset: filterset, filtvalue:filtvalue,filterName:filterName]"/>


                        </span>

                    </div>
                </span>

            </li>
            <li id="tab_link_result" data-bind="visible: filterIsSet()||allcount()>=0">
                <a href="#result" data-toggle="tab" data-bind="visible: filterIsSet() ">
                    <g:message code="result" />
                    <span data-bind="visible: allcount()>=0">
                        <span data-bind="text: allcount" class="text-info">${total}</span>
                        <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span>
                    </span>
                    <span data-bind="visible: allcount()<0" class="text-muted">&hellip;</span>

                </a>
                <a href="#" data-bind="visible: !filterIsSet() ">
                    <g:message code="enter.a.filter" />
                </a>
            </li>
            <li data-bind="visible: filterIsSet()||allcount()>=0" class="pull-right">
                <span class="tabs-sibling tabs-sibling-compact ">
                <div class=" btn-group pull-right ">
                    <button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
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
                               class="textbtn textbtn-default"
                               data-bind="click: nodeSummary().removeDefault">
                                <i class="glyphicon glyphicon-ban-circle"></i>
                                <g:message code="remove.default.filter" />
                            </a>
                        </li>
                        <li class="divider" ></li>
                        <g:if test="${g.executionMode(is:'active')}">

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

        <div class="row row-space">
        <div class="col-sm-12">
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
                            %{--<span class="text-muted">no tags</span>--}%
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

                <div class="row row-space">

                    <div class="col-sm-5 col-sm-offset-1">
                        <span class="text-uppercase text-muted small"><i class="glyphicon glyphicon-tags  " ></i> <g:message code="resource.metadata.entity.tags" /></span>
                        <ul data-bind="foreach: nodeSummary().tags" class="list-unstyled">
                            <li>
                                <node-filter-link params="
                                    filterkey: 'tags',
                                    filterval: tag,
                                    tag: tag
                                ">
                                </node-filter-link>

                                <span data-bind="text: count" class="text-muted"></span>
                            </li>
                        </ul>
                        <div data-bind="visible: !nodeSummary().tags">
                            <g:message code="none" />
                        </div>
                    </div>

                    <div class="col-sm-5">

                        <span class="text-uppercase text-muted small"><i class="glyphicon glyphicon-filter  " ></i> <g:message code="filters" /></span>
                        <ul class="list-unstyled">
                            <li>
                                <a href="#"
                                   class="nodefilterlink textbtn textbtn-default" data-node-filter=".*"
                                   data-node-filter-all="true">
                                    <g:message code="all.nodes" /></a>

                                <span data-bind="text: nodeSummary().totalCount" class="text-info">...</span>

                                <div class="btn-group">
                                    <button type="button"
                                            class="btn btn-default btn-sm btn-link dropdown-toggle"
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
                                <a
                                        href="#"
                                        class=" nodefilterlink textbtn textbtn-default"
                                        data-bind="attr: {
                                                        'data-node-filter-name': name(),
                                                        'data-node-filter': filter(),
                                                        'title': filter(),
                                                        'href': $root.nodeSummary().linkForFilterName($data)
                                                    },

                             "
                                >
                                    <span data-bind="text: name"></span>
                                </a>
                                <div class="btn-group">
                                    <button type="button"
                                            class="btn btn-default btn-sm btn-link dropdown-toggle"
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
</body>
</html>
