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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/4/13
  Time: 10:23 AM
  To change this template use File | Settings | File Templates.
--%>

<%@ page import="grails.converters.JSON; org.rundeck.core.auth.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>

    <meta name="layout" content="base"/>
    <meta name="tabpage" content="home"/>
    <title><g:appTitle/></title>
    <g:if test="${!projectNames}">
        <g:embedJSON data="${[projectNames:[],projectNamesTotal:-1]}" id="projectNamesData"/>
    </g:if>
    <g:elseif test="${projectNames && projectNames.size()<50}">
        <g:embedJSON data="${[projectNames:projectNames,projectNamesTotal:projectNames.size()]}" id="projectNamesData"/>
    </g:elseif>
    <g:elseif test="${projectNames}">
        <g:embedJSON data="${[projectNames:projectNames[0..49],projectNamesTotal:projectNames.size()]}" id="projectNamesData"/>
    </g:elseif>
    <g:embedJSON data="${[loaded:statsLoaded,execCount:execCount,totalFailedCount:totalFailedCount,recentUsers:recentUsers,recentProjects:recentProjects]}" id="statsData"/>

    <g:embedJSON data="${[
            detailBatchMax        : params.getInt('detailBatchMax')?:cfg.getInteger(config: 'gui.home.projectList.detailBatchMax', default: 15),
            summaryRefresh        : cfg.getBoolean(config: 'gui.home.projectList.summaryRefresh', default: true),
            refreshDelay          : cfg.getInteger(config: 'gui.home.projectList.summaryRefreshDelay', default: 30000),
            detailBatchDelay      : params.getInt('detailBatchDelay')?:cfg.getInteger(config: 'gui.home.projectList.detailBatchDelay', default: 1000).toInteger(),
            pagingEnabled         : params.getBoolean('pagingEnabled',cfg.getBoolean(config: 'gui.home.projectList.pagingEnabled',default: true)),
            pagingMax             : params.getInt('pagingMax')?:cfg.getInteger(config: 'gui.home.projectList.pagingMax', default: 30),
    ]}" id="homeDataPagingParams"/>

    <!-- VUE JS REQUIREMENTS -->
    <g:loadEntryAssets entry="components/ko-paginator" />
    <!-- /VUE JS REQUIREMENTS -->

    <asset:javascript src="menu/home.js"/>

    <g:loadEntryAssets entry="pages/login" />
    <style type="text/css">
    .project_list_item_link{
        display:inline-block;
        width: calc(100% - 20px);
        padding:8px 0;
    }
    </style>
</head>
<body>
<div class="content">
<div id="layoutBody">
  <div class="row">
    <div class="col-sm-12">
      <g:render template="/common/messages"/>
    </div>
  </div>
  <div class="container-fluid" data-bind="if: projectCount()>0 || !loadedProjectNames()">
    <div class="row">
      <g:if test="${isFirstRun}">
        <div id="first-run-message" class="col-sm-12">
          <div class="card">
            <div class="card-header">
              <h2 class="card-title">
                <g:message code="app.firstRun.title" args="${[g.appTitle(),grailsApplication.metadata['build.ident']]}"/>
              </h2>
            </div>
            <div class="card-content">
              <g:set var="logoImage" value="${"static/img/${g.appLogo()}"}"/>
              <asset:image src="${logoImage}" alt="${[g.appTitle()]}" style="width: 400px; padding-bottom: 10px" onload="onSvgLoaded(this)"/>
              <g:markdown><g:autoLink>${message(code: "app.firstRun.md")}</g:autoLink></g:markdown>
              <p class="h6 text-strong" style="margin-top:1em;">
                <g:message code="you.can.see.this.message.again.by.clicking.the" />
                <g:link action="welcome" controller="menu"><g:message code="version.number" /></g:link>
                <g:message code="in.the.page.footer" />
              </p>
            </div>
          </div>
        </div>
      </g:if>
    </div>
    <div class="row">
      <div class="flex justify-between">
        <div style="margin-left:15px;margin-right:15px;" class="col-sm-12 col-md-5 card">
          <div class="card-content">
            <span class="text-h3" data-bind="if: loadedProjectNames()">
              <span data-bind="messageTemplate: projectNamesTotal, messageTemplatePluralize:true">
                <g:message code="page.home.section.project.title" />|<g:message code="page.home.section.project.title.plural" />
              </span>
            </span>
            <span class="text-h3 text-muted" data-bind="if: !loadedProjectNames()">
                <b class="fas fa-spinner fa-spin loading-spinner"></b>
                <g:message code="page.home.loading.projects" />
            </span>
            <auth:resourceAllowed action="${AuthConstants.ACTION_CREATE}" kind="${AuthConstants.TYPE_PROJECT}" context="${AuthConstants.CTX_APPLICATION}">
              <g:link controller="framework" action="createProject" class="btn  btn-primary pull-right">
                <g:message code="page.home.new.project.button.label" />
                <b class="glyphicon glyphicon-plus"></b>
              </g:link>
            </auth:resourceAllowed>
          </div>
        </div>
        <div style="margin-left:15px;margin-right:15px;" class="col-sm-12 col-md-7 card">
          <div class="card-content flex flex--direction-col flex--justify-center h-full">
            <span data-bind="if: !loaded()" class="text-muted">
              ...
            </span>
            <div data-bind="if: projectCount() > 0 && loaded()">
              %{--app summary info--}%
                <span class="h4">
                  <span class="summary-count" data-bind="css: { 'text-info': execCount()>0, 'text-strong': execCount()<1 }">
                    <span data-bind="text: execCount"></span>
                  </span>
                  <span data-bind="messageTemplate: execCount, messageTemplatePluralize:true">
                    <g:message code="Execution" />|<g:message code="Execution.plural" />
                  </span>
                  <g:message code="page.home.duration.in.the.last.day" />
                    <span class="summary-count" data-bind="css: { 'text-warning': totalFailedCount()>0, 'text-strong': totalFailedCount()<1 }">
                      <span data-bind="messageTemplate: totalFailedCount">
                        <g:message code="page.home.project.executions.0.failed.parenthetical" />
                      </span>
                    </span>
                  </span>
                  <div data-bind="if: recentProjectsCount()>1">
                    <g:message code="in" />
                    <span class="text-info" data-bind="text: recentProjectsCount()"></span>
                    <g:message code="Project.plural" />:
                    <span data-bind="foreach: recentProjects">
                      <a href="${g.createLink(action:'index',controller:'menu',params:[project:'<$>'])}" data-bind="urlPathParam: $data, text: $data"></a>
                    </span>
                  </div>
                  <div data-bind="if: recentUsersCount()>0">
                    <g:message code="by" />
                    <span class="text-info" data-bind="text: recentUsersCount"></span>
                    <span data-bind="messageTemplate: recentUsersCount(),messageTemplatePluralize:true">
                        <g:message code="user" />:|<g:message code="user.plural" />:
                    </span>
                    <span data-bind="text: recentUsers().join(', ')"></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="container-fluid" data-bind="if: projectCount() == 0">
        <auth:resourceAllowed action="${AuthConstants.ACTION_CREATE}" kind="${AuthConstants.TYPE_PROJECT}" context="${AuthConstants.CTX_APPLICATION}" has="true">
          <div id="firstRun"></div>
        </auth:resourceAllowed>
      </div>
      <div class="container-fluid" data-bind="if: projectCount()<1 && loadedProjectNames()">
        <div class="row">
          <auth:resourceAllowed action="${AuthConstants.ACTION_CREATE}" kind="${AuthConstants.TYPE_PROJECT}" context="${AuthConstants.CTX_APPLICATION}" has="false">
          <div class="col-sm-12">
            <div class="card">
              <div class="card-content">
                <div class="well">
                  <g:set var="roles" value="${request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class)?.collect { it.name }?.join(", ")}"/>
                  <h2 class="text-warning">
                    <g:message code="no.authorized.access.to.projects" />
                  </h2>
                  <p>
                    <g:message code="no.authorized.access.to.projects.contact.your.administrator.user.roles.0" args="[roles]" />
                  </p>
                </div>
              </div>
            </div>
          </div>
          </auth:resourceAllowed>
        </div>
      </div>
      <div class="container-fluid">
        <div class="row">
          <div class="col-xs-12">
            <div class="card" >
              <div id="project-list" class="card-content">
                <div>
                  <div class="input-group">
                    <!-- <span class="input-group-addon"><i class="fa fa-search"></i></span> -->
                    <input type="search" name="search" placeholder="${message(code:"page.home.search.projects.input.placeholder")}" class="form-control input-sm" data-bind="value: search" />
                    <span class="input-group-addon"><g:icon name="search"/></span>
                  </div>
                  <div data-bind="if: filtered.enabledFiltersCount()>0 && loadedProjectNames()">
                    <div class="alert alert-info">
                      <span data-bind="messageTemplate: searchedProjectsCount(), messageTemplatePluralize:true, css: { 'text-info': searchedProjectsCount()>0, 'text-warning': searchedProjectsCount()<1 }">
                        <g:message code="page.home.search.project.title" />|<g:message code="page.home.search.project.title.plural" />
                      </span>
                    </div>
                  </div>
                </div>
                <div  data-bind="if: pagingEnabled()">
                  <span class="text-muted" data-bind="if: paging.hasPages()">
                    <span data-bind="text: paging.pageFirstIndex"></span>-<span data-bind="text: paging.pageLastIndex"></span>
                    of <span class="text-info" data-bind="text: paging.content().length"></span>
                  </span>
                  <div data-ko-pagination="project-list-pagination"></div>
                </div>
                <div class="project_list_header">
                  <div class="row row-border-top p-4">
                    <div class="col-sm-6 col-md-8">
                      <div class="text-lg">Projects </div>
                    </div>
                    <div class="col-sm-6 col-md-2">
                      <div class="text-lg">Activity </div>
                    </div>
                    <div class="col-sm-12 col-md-2 col-last">
                      <div class="text-lg">Actions </div>
                    </div>
                  </div>
                </div>

                <div data-bind="foreach: { data: pagedProjects(), as: 'project' } ">
                  %{--Template for project details--}%
                  <div class="project_list_item" data-bind="attr: { 'data-project': project }, ">
                    <div class="row row-hover row-border-top">
                      <div class="col-sm-6 col-md-8">
                        <a href="${g.createLink(action:'index',controller:'menu',params:[project:'<$>'])}" data-bind="urlPathParam: project"
                          class="link-hover  text-inverse project_list_item_link link-quiet">

                          <span class="h5" data-bind="if: $root.projectForName(project) && $root.projectForName(project).label">
                            <div data-bind="text: $root.projectForName(project).label"></div>
                          </span>
                          <span class="h5" data-bind="ifnot: $root.projectForName(project) && $root.projectForName(project).label">
                            <span data-bind="text: project"></span>
                          </span>

                          <span class="h5" data-bind="if: !$root.projectForName(project).executionEnabled()">
                            <span class="text-base text-warning  has_tooltip" data-placement="right" data-bind="bootstrapTooltip: true" title="${message(code:'project.execution.disabled')}">
                              <i class="glyphicon glyphicon-pause"></i>
                            </span>
                          </span>
                          <span class="h5" data-bind="if: !$root.projectForName(project).scheduleEnabled()">
                            <span class="text-base text-warning has_tooltip"  data-placement="right"  data-bind="bootstrapTooltip: true" title="${message(code:'project.schedule.disabled')}">
                              <i class="glyphicon glyphicon-ban-circle"></i>
                            </span>
                          </span>

                          <span data-bind="if: $root.projectForName(project)">
                            <span class="text-secondary text-base" data-bind="text: $root.projectForName(project).description"></span>
                          </span>
                        </a>
                      </div>
                      <div class="col-sm-6 col-md-2 text-center">
                        <div data-bind="if: $root.projectForName(project)">
                          <a  data-bind="css: { 'text-secondary': $root.projectForName(project).execCount()<1 }, urlPathParam: project,  bootstrapPopover: true, bootstrapPopoverContentRef: '#exec_detail_'+project "
                              href="${g.createLink(controller: "reports", action: "index", params: [project: '<$>'])}"
                              class="as-block link-hover link-block-padded text-inverse "
                              data-toggle="popover"
                              data-placement="bottom"
                              data-trigger="hover"
                              data-container="body"
                              data-delay="{&quot;show&quot;:0,&quot;hide&quot;:200}"
                              data-popover-template-class="popover-wide popover-primary"
                          >
                            <span class="summary-count " data-bind="css: { 'text-info':$root.projectForName(project).execCount()>0 } ">
                              <span data-bind="if: !$root.projectForName(project).loaded()" >...</span>
                              <span data-bind="if: $root.projectForName(project).loaded()">
                                <span data-bind="if: $root.projectForName(project).execCount()>0">
                                  <span data-bind="text: $root.projectForName(project).execCount()" class="text-h3"></span>
                                </span>
                                <span data-bind="if: $root.projectForName(project).execCount()<1">None</span>
                              </span>

                            </span>
                          </a>

                          <div data-bind="if: $root.projectForName(project).userCount()>0,attr: { 'id': 'exec_detail_'+project }," style="display:none;" >
                            <span data-bind="if: $root.projectForName(project).execCount()>0">
                              <span data-bind="text: $root.projectForName(project).execCount()"></span>
                            </span>
                            <span data-bind="messageTemplate: $root.projectForName(project).execCount(), messageTemplatePluralize: true">
                              <g:message code="Execution" />|<g:message code="Execution.plural" />
                            </span>
                            <g:message code="page.home.duration.in.the.last.day" />
                            <g:message code="by" />
                            <span class="text-info" data-bind="text: $root.projectForName(project).userCount()">
                            </span>
                            <span data-bind="messageTemplate: $root.projectForName(project).userCount(),messageTemplatePluralize:true">
                              <g:message code="user" />:|<g:message code="user.plural" />:
                            </span>
                            <span data-bind="text: $root.projectForName(project).userSummary().join(', ')"></span>
                          </div>
                          <span data-bind="if: $root.projectForName(project).failedCount()>0">
                            <a  data-bind="urlPathParam: project "
                              class="text-warning"
                              href="${g.createLink(
                                      controller: "reports",
                                      action: "index",
                                      params: [project: '<$>', statFilter: 'fail']
                            )}">
                              <span data-bind="messageTemplate: $root.projectForName(project).failedCount()">
                                <g:message code="page.home.project.executions.0.failed.parenthetical"/>
                              </span>
                            </a>
                          </span>
                        </div>
                      </div>

                <div class="col-sm-12 col-md-2 col-last" data-bind="if: $root.projectForName(project)">
                  <div class="pull-right">
                    <div class="dropdown-toggle-hover" >
                      <a href="#" class="as-block link-hover link-quiet link-block-padded text-inverse dropdown-toggle" data-toggle="dropdown">
                        <g:message code="button.Action"/>
                        <span class="caret"></span>
                      </a>
                      <ul class="dropdown-menu pull-right" role="menu">
                        <li data-bind="if: !$root.projectForName(project).loaded()">
                          <a href="#" class="text-muted">
                            <b class="fas fa-spinner fa-spin loading-spinner text-muted"></b> Loading &hellip;
                          </a>
                        </li>
                        <!-- ko if: $root.projectForName(project).loaded() -->
                        <li data-bind="if: $root.projectForName(project).auth().admin">
                          <a href="${g.createLink(controller: "framework", action: "editProject", params: [project: '<$>'])}"
                              data-bind="urlPathParam: project">
                            <g:message code="edit.configuration"/>
                          </a>
                        </li>

                        <li class="divider" data-bind="if: $root.projectForName(project).auth().admin"></li>
                        <!-- ko if: $root.projectForName(project).auth().jobCreate -->
                        <li >
                          <a href="${g.createLink(controller: "scheduledExecution", action: "create", params: [project: '<$>'])}" data-bind="urlPathParam: project">
                            <i class="glyphicon glyphicon-plus"></i>
                            <g:message code="new.job.button.label" />
                          </a>
                        </li>
                        <li class="divider"></li>
                        <li>
                          <a href="${g.createLink(controller: "scheduledExecution", action: "upload", params: [project: '<$>'])}" data-bind="urlPathParam: project" class="">
                            <i class="glyphicon glyphicon-upload"></i>
                            <g:message code="upload.definition.button.label" />
                          </a>
                        </li>
                        <!-- /ko -->
                        <!-- /ko -->
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div data-bind="if: $root.projectForName(project)">
              <div class="row" data-bind="if: $root.projectForName(project).showMessage() ">
                <div class="project_list_readme col-sm-10 col-sm-offset-1 col-xs-12">
                  <div data-bind="if: $root.projectForName(project).showMotd() ">
                    <span data-bind="if: $root.projectForName(project).readme().motdHTML()">
                        <span data-bind="html: $root.projectForName(project).readme().motdHTML()"></span>
                    </span>
                  </div>
                  <div data-bind="if:  $root.projectForName(project).showReadme() ">
                  <div>
                    <span data-bind="if: $root.projectForName(project).readme().readmeHTML()">
                        <span data-bind="html: $root.projectForName(project).readme().readmeHTML()"></span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <!-- ko if: $root.projectForName(project).extra() -->
            <!-- ko foreach: $root.projectForName(project).extra() -->
            <div data-bind="component: $data"></div>
            <!-- /ko -->
            <!-- /ko -->
          </div>

        </div>
      </div>
    </div>
  </div>
</div>
</div>
</div>
<!-- VUE JS MODULES -->
<g:loadEntryAssets entry="components/first-run" />
<g:loadEntryAssets entry="components/version-notification" />
<!-- /VUE JS MODULES -->
<script type="application/javascript">
  function onSvgLoaded(image) {
    if (typeof SVGInject !== 'undefined') {
      return SVGInject(image)
    }
    window.addEventListener('load', function() { SVGInject(image) })
  }
</script>
</body>
</html>
