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
    <g:if test="${projectNames.size()<50}">
        <g:embedJSON data="${[projectNames:projectNames,projectNamesTotal:projectNames.size()]}" id="projectNamesData"/>
    </g:if>
    <g:else>
        <g:embedJSON data="${[projectNames:projectNames[0..49],projectNamesTotal:projectNames.size()]}" id="projectNamesData"/>
    </g:else>
    <g:embedJSON data="${[loaded:true,execCount:execCount,totalFailedCount:totalFailedCount,recentUsers:recentUsers,recentProjects:recentProjects]}" id="statsData"/>
    <g:embedJSON data="${[
            pagingInitialMax:grailsApplication.config.rundeck?.gui?.home?.projectList?.pagingInitialMax?:15,
            pagingRepeatMax:grailsApplication.config.rundeck?.gui?.home?.projectList?.pagingRepeatMax?:50,
            summaryRefresh:!(grailsApplication.config.rundeck?.gui?.home?.projectList?.summaryRefresh in ['false',false]),
            refreshDelay:grailsApplication.config.rundeck?.gui?.home?.projectList?.summaryRefreshDelay?:30000,
            doPaging:!(grailsApplication.config.rundeck?.gui?.home?.projectList?.doPaging in ['false',false]),
            pagingDelay:grailsApplication.config.rundeck?.gui?.home?.projectList?.pagingDelay?:2000
    ]}" id="homeDataPagingParams"/>
    <asset:javascript src="menu/home.js"/>

    <!-- VUE JS REQUIREMENTS -->
    <asset:javascript src="static/manifest.js"/>
    <asset:javascript src="static/vendor.js"/>
    <!-- /VUE JS REQUIREMENTS -->

    <!-- VUE CSS MODULES -->
    <asset:stylesheet href="static/css/components/version-notification.css"/>
    <!-- /VUE CSS MODULES -->

    <style type="text/css">
    .project_list_item_link{
        display:inline-block;
        width: calc(100% - 20px);
    }
    </style>
</head>
<body>
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
                    <g:markdown><g:autoLink>${message(code: "app.firstRun.md")}</g:autoLink></g:markdown>
                    <p class="h6 text-primary" style="margin-top:1em;">
                        <g:message code="you.can.see.this.message.again.by.clicking.the" />
                        <g:link action="welcome" controller="menu"><g:message code="version.number" /></g:link>
                        <g:message code="in.the.page.footer" />
                    </p>
                  </div>
              </div>
            </div>
        </g:if>
        <div class="col-sm-12 col-md-5">
          <div class="card">
            <div class="card-content" style="padding-bottom: 20px;">
              <span class="h3 text-primary">
                  <span data-bind="messageTemplate: projectNamesTotal, messageTemplatePluralize:true">
                      <g:message code="page.home.section.project.title" />|<g:message code="page.home.section.project.title.plural" />
                  </span>
              </span>
              <auth:resourceAllowed action="create" kind="project" context="application">
                <g:link controller="framework" action="createProject" class="btn  btn-success pull-right">
                    <g:message code="page.home.new.project.button.label" />
                    <b class="glyphicon glyphicon-plus"></b>
                </g:link>
              </auth:resourceAllowed>
            </div>
            <!--
            <div class="card-footer">
              <hr>
              <div class="row">
                <auth:resourceAllowed action="create" kind="project" context="application">
                    <div class="col-sm-4">
                        <g:link controller="framework" action="createProject" class="btn  btn-success pull-right">
                            <g:message code="page.home.new.project.button.label" />
                            <b class="glyphicon glyphicon-plus"></b>
                        </g:link>
                    </div>
                </auth:resourceAllowed>
              </div>
            </div>
          -->
          </div>
        </div>
        <div class="col-sm-12 col-md-7">
          <div class="card">
            <div class="card-content">
              <span data-bind="if: !loaded()">
                <b class="fas fa-spinner fa-spin loading-spinner text-muted fa-lg"></b>
              </span>
              <div data-bind="if: projectCount() > 1 && loaded()">
                %{--app summary info--}%
                  <span class="h4">
                    <span class="summary-count" data-bind="css: { 'text-info': execCount()>0, 'text-primary': execCount()<1 }">
                          <span data-bind="text: execCount"></span>
                    </span>
                    <span data-bind="messageTemplate: execCount, messageTemplatePluralize:true">
                      <g:message code="Execution" />|<g:message code="Execution.plural" />
                    </span>
                    <g:message code="page.home.duration.in.the.last.day" />
                    <span class="summary-count" data-bind="css: { 'text-warning': totalFailedCount()>0, 'text-primary': totalFailedCount()<1 }">
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
<g:if test="${projectNames.size()<1}">
  <div class="container-fluid">
    <div class="row">
        <div class="col-sm-12">
          <div class="card">
            <div class="card-content">
              <auth:resourceAllowed action="create" kind="project" context="application" has="false">
                  <div class="well">
                      <g:set var="roles" value="${request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class)?.collect { it.name }?.join(", ")}"/>
                      <h2 class="text-warning">
                          <g:message code="no.authorized.access.to.projects" />
                      </h2>
                      <p>
                          <g:message code="no.authorized.access.to.projects.contact.your.administrator.user.roles.0" args="[roles]" />
                      </p>
                  </div>
              </auth:resourceAllowed>
              <auth:resourceAllowed action="create" kind="project" context="application" has="true">
                  <div class="jumbotron">
                      <h2><g:message code="app.firstRun.title" args="${[g.appTitle(),grailsApplication.metadata['build.ident']]}"/></h2>
                      <g:markdown><g:autoLink>${message(code: "app.firstRun.md")}</g:autoLink></g:markdown>
                      <p>
                          <g:message code="page.home.get.started.message" />
                      </p>
                      <p>
                          <g:link controller="framework" action="createProject" class="btn  btn-success btn-lg ">
                              <g:message code="page.home.new.project.button.label" />
                              <b class="glyphicon glyphicon-plus"></b>
                          </g:link>
                      </p>
                      <span class="text-small text-primary">
                          <g:message code="you.can.see.this.message.again.by.clicking.the" />
                          <g:link action="welcome" controller="menu"><g:message code="version.number" /></g:link>
                          <g:message code="in.the.page.footer" />
                      </span>
                  </div>
              </auth:resourceAllowed>
            </div>
          </div>
        </div>
    </div>
  </div>
</g:if>
<div class="container-fluid">
  <div class="row">
    <div class="col-xs-12">
      <div class="card" data-bind="if:  projectCount()>0 ">
        <div class="card-content">
          <div data-bind="if: !loadedProjectNames() && projectCount()<1">
            <div class="">
                <g:message code="page.home.loading.projects" />
                <b class="fas fa-spinner fa-spin loading-spinner text-muted fa-2x"></b>
            </div>
          </div>
          <div data-bind="if: projectCount()>0">
            <div class="input-group">
              <!-- <span class="input-group-addon"><i class="fa fa-search"></i></span> -->
              <input type="search" name="search" placeholder="${message(code:"page.home.search.projects.input.placeholder")}" class="form-control input-sm" data-bind="value: search" />
              <span class="input-group-addon"><g:icon name="search"/></span>
            </div>
            <div data-bind="if: search()">
              <div class="alert alert-info">
                <span data-bind="messageTemplate: searchedProjectsCount(), messageTemplatePluralize:true, css: { 'text-info': searchedProjectsCount()>0, 'text-warning': searchedProjectsCount()<1 }">
                    <g:message code="page.home.search.project.title" />|<g:message code="page.home.search.project.title.plural" />
                </span>
              </div>
            </div>
          </div>
          <div data-bind="foreach: { data: searchedProjects(), as: 'project' } ">
          %{--Template for project details--}%
            <div class="project_list_item" data-bind="attr: { 'data-project': project }, ">
              <div class="row row-hover row-border-top">
                  <div class="col-sm-6 col-md-8">
                      <a href="${g.createLink(action:'index',controller:'menu',params:[project:'<$>'])}" data-bind="urlPathParam: project"
                         class="text-h3  link-hover  text-inverse project_list_item_link">

                          <span data-bind="if: $root.projectForName(project) && $root.projectForName(project).label">
                              <span data-bind="text: $root.projectForName(project).label"></span>
                          </span>
                          <span data-bind="ifnot: $root.projectForName(project) && $root.projectForName(project).label">
                              <span data-bind="text: project"></span>
                          </span>

                          <span data-bind="if: !$root.projectForName(project).executionEnabled()">
                              <span class="text-base text-warning  has_tooltip" data-placement="right" data-bind="bootstrapTooltip: true" title="${message(code:'project.execution.disabled')}">
                                  <i class="glyphicon glyphicon-pause"></i>
                              </span>
                          </span>
                          <span data-bind="if: !$root.projectForName(project).scheduleEnabled()">
                              <span class="text-base text-warning has_tooltip"  data-placement="right"  data-bind="bootstrapTooltip: true" title="${message(code:'project.schedule.disabled')}">
                                  <i class="glyphicon glyphicon-ban-circle"></i>
                              </span>
                          </span>

                          <span data-bind="if: $root.projectForName(project)">
                              <span class="text-secondary text-base" data-bind="text: $root.projectForName(project).description"></span>
                          </span>
                      </a>

                      <div data-bind="if: $root.projectForName(project)">
                          <div class="row " data-bind="if: $root.projectForName(project).showMessage() ">
                              <div class="col-sm-11 col-sm-offset-1 col-xs-12">
                                  <div data-bind="if: $root.projectForName(project).showMotd() ">
                                      <span data-bind="if: $root.projectForName(project).readme().motdHTML()">
                                          <span data-bind="html: $root.projectForName(project).readme().motdHTML()"></span>
                                      </span>
                                  </div>
                                  <div data-bind="if:  $root.projectForName(project).showReadme() ">
                                      <span data-bind="if: $root.projectForName(project).readme().readmeHTML()">
                                          <span data-bind="html: $root.projectForName(project).readme().readmeHTML()"></span>
                                      </span>
                                  </div>
                              </div>
                          </div>
                      </div>
                  </div>
                  <div class="col-sm-6 col-md-2 text-center">
                      <div data-bind="if: $root.projectForName(project)">
                          <a data-bind="css: { 'text-secondary': $root.projectForName(project).execCount()<1 }, urlPathParam: project,  bootstrapPopover: true, bootstrapPopoverContentRef: '#exec_detail_'+project "
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
                              <a data-bind="urlPathParam: project "
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
                  <div data-bind="if: $root.projectForName(project)">
                      <div class="col-sm-12 col-md-2" >
                          <div class="pull-right">
                              <span data-bind="if: !$root.projectForName(project).loaded()">
                                  <b class="fas fa-spinner fa-spin loading-spinner text-muted fa-lg"></b>
                              </span>
                              <div class="btn-group dropdown-toggle-hover" data-bind="if: $root.projectForName(project).auth().jobCreate">
                                  <a href="#" class="as-block link-hover link-block-padded text-inverse dropdown-toggle" data-toggle="dropdown">
                                      <g:message code="button.Action"/>
                                      <span class="caret"></span>
                                  </a>
                                  <ul class="dropdown-menu pull-right" role="menu">
                                      <li data-bind="if: $root.projectForName(project).auth().admin">
                                          <a href="${g.createLink(controller: "framework", action: "editProject", params: [project: '<$>'])}"
                                             data-bind="urlPathParam: project">
                                              <g:message code="edit.configuration"/>
                                          </a>
                                      </li>

                                      <li class="divider" data-bind="if: $root.projectForName(project).auth().admin"></li>

                                      <li>
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
                                  </ul>
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
</div>
<!-- VUE JS MODULES -->
<asset:javascript src="static/components/version-notification.js"/>
<!-- /VUE JS MODULES -->
</body>
</html>
