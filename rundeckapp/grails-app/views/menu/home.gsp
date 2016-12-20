<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/4/13
  Time: 10:23 AM
  To change this template use File | Settings | File Templates.
--%>

<%@ page import="grails.converters.JSON; com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <style>
        .dtCol1{
            width:auto;
            overflow:hidden;
        }
        .dtCol2{
            width:170px;
            overflow:hidden;
        }
        .dtCol3{
            width: 140px;
            overflow: hidden;
        }
    </style>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>

    <meta name="layout" content="base"/>
    <meta name="tabpage" content="home"/>
    <title><g:appTitle/></title>
<<<<<<< HEAD
    <asset:stylesheet href="dtable.css"/>
    <asset:javascript src="jquery.js"/>
    <script>jQuery.noConflict(true);</script>
    <asset:javascript src="jquery.dataTables.js"/>
    <asset:javascript src="DT_bootstrap.js"/>
    <asset:javascript src="dtable.js"/>
=======
    <g:if test="${projectNames.size()<50}">
        <g:embedJSON data="${[projectNames:projectNames,projectNamesTotal:projectNames.size()]}" id="projectNamesData"/>
    </g:if>
    <g:else>
        <g:embedJSON data="${[projectNames:projectNames[0..49],projectNamesTotal:projectNames.size()]}" id="projectNamesData"/>
    </g:else>
    <g:embedJSON data="${[loaded:true,execCount:execCount,recentUsers:recentUsers,recentProjects:recentProjects]}" id="statsData"/>
    <g:embedJSON data="${[
            pagingInitialMax:grailsApplication.config.rundeck?.gui?.home?.projectList?.pagingInitialMax?:15,
            pagingRepeatMax:grailsApplication.config.rundeck?.gui?.home?.projectList?.pagingRepeatMax?:50,
            summaryRefresh:!(grailsApplication.config.rundeck?.gui?.home?.projectList?.summaryRefresh in ['false',false]),
            refreshDelay:grailsApplication.config.rundeck?.gui?.home?.projectList?.summaryRefreshDelay?:30000,
            doPaging:!(grailsApplication.config.rundeck?.gui?.home?.projectList?.doPaging in ['false',false]),
            pagingDelay:grailsApplication.config.rundeck?.gui?.home?.projectList?.pagingDelay?:2000
    ]}" id="homeDataPagingParams"/>
>>>>>>> rundeck/development
    <asset:javascript src="menu/home.js"/>

</head>
<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>
<<<<<<< HEAD

<div class="container-fluid">
<div class="well well-sm">

<g:if test="${projCount>0}">
<div class="row row-space">
    <auth:resourceAllowed action="create" kind="project" context="application">
        <div class="col-sm-1">
            <g:link controller="framework" action="createProject" class="btn btn-sm btn-success pull-left">
                New Project
                <b class="glyphicon glyphicon-plus"></b>
            </g:link>
        </div>
    </auth:resourceAllowed>

     <g:set var="execCountTotal" value="${0l}" />
     <g:set var="projCountTotal" value="${0l}" />
     <g:set var="userCountTotal" value="${0l}" />

     <g:each in="${projectSummaries.sort{a,b->a.key<=>b.key}}" var="projectData">
         <g:set var="project" value="${projectData.key}"/>
         <g:set var="data" value="${projectData.value}"/>
         <g:set var="execCountTotal" value="${execCountTotal+data.execCount}"/>
         <g:if test="${data.execCount > 0}">
             <g:set var="projCountTotal" value="${++projCountTotal}"/>
             <g:set var="userCountTotal" value="${++userCountTotal}"/>
         </g:if>
     </g:each>

    <auth:resourceAllowed action="read" kind="project" context="application">
        <div class="col-sm-11">
            <span class="h5 text-muted">
            <g:enc>${projCount}</g:enc>
            <g:plural code="Project" count="${projCount}" textOnly="${true}"/>
        </span>

            <g:if test="${projCount > 0}">
    %{--app summary info--}%
                        <span class="h5">
                            <span class='badge'>
                                <span class="summary-count"><g:enc>${execCountTotal}</g:enc></span>
                            </span>
                    <strong>
                        <g:plural code="Execution" count="${execCountTotal}" textOnly="${true}"/>
                    </strong>
                </span>
                <g:if test="${projectSummaries.size() > 0}">
                    <div>
                        in
                        <span class="text-info">
                            <g:enc>${projCountTotal}</g:enc>
                        </span>
                        <g:plural code="Project" count="${projectSummary.size()}" textOnly="${true}"/>:
                                <g:each var="projectData" in="${projectSummaries.sort{a,b->a.key<=>b.key}}" status="i">
                                    <g:set var="project" value="${projectData.key}"/>
                                    <g:set var="data" value="${projectData.value}"/>
                                    <g:if test="${data.execCount > 0}">
                                        <g:link action="index" controller="menu" params="[project: project]" class="btn btn-xs btn-default">
                                        <g:enc>${project}</g:enc></g:link>
                                    </g:if>
                        </g:each>
                    </div>
                </g:if>
                <div>
                    <g:if test="${userCount > 0}">
                        by
                        <span class="text-info">
                            <g:enc>${userCountTotal}</g:enc>
                        </span>
                                <g:plural code="User" count="${userSummary.size()}" textOnly="${true}"/>
                                <g:each var="projectData" in="${projectSummaries.sort{a,b->a.key<=>b.key}}" status="i">
                                    <g:set var="project" value="${projectData.key}"/>
                                    <g:set var="data" value="${projectData.value}"/>
                                    <g:if test="${data.userCount > 0}">
                                    <g:each in="${data.userSummary}" var="user" status="j">
                                        <g:enc>[${user}]</g:enc><g:if test="${j < data.userSummary.size() - 1}">,</g:if>
                                    </g:each>
                                    </g:if>
                        </g:each>
                    </g:if>
                </div>
    </g:if>
        </div>
    </auth:resourceAllowed>
</div>
</g:if>
</div> <%--end of well--%>

<g:if test="${!projCount}">
<div class="row row-space">
<div class="col-sm-12">
    <auth:resourceAllowed action="create" kind="project" context="application" has="false">
        <div class="well well-sm">
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
            <h1>Welcome to <g:appTitle/></h1>

            <p>
                To get started, create a new project.
            </p>
            <p>
                <g:link controller="framework" action="createProject" class="btn  btn-success btn-sm ">
                    New Project
=======
<div data-bind="if: projectCount()>0 || !loadedProjectNames()">
    <div class="row row-space">

        <div class="col-sm-4">
            <span class="h3 text-muted">
                <span data-bind="messageTemplate: projectNamesTotal, messageTemplatePluralize:true">
                    <g:message code="page.home.section.project.title" />|<g:message code="page.home.section.project.title.plural" />
                </span>
            </span>
        </div>

        <div class="col-sm-4">

            <span data-bind="if: !loaded()">
                <asset:image src="spinner-gray.gif" width="32px" height="32px"/>
            </span>
            <div data-bind="if: projectCount() > 1 && loaded()">
            %{--app summary info--}%
                <span class="h4">

                    <span class="summary-count"
                        data-bind="css: { 'text-info': execCount()>0, 'text-muted': execCount()<1 }">
                        <span data-bind="text: execCount"></span>
                    </span>

                    <span data-bind="messageTemplate: execCount, messageTemplatePluralize:true">
                        <g:message code="Execution" />|<g:message code="Execution.plural" />
                    </span>

                    <g:message code="page.home.duration.in.the.last.day" />

                </span>
                <div data-bind="if: recentProjectsCount()>1">

                    <g:message code="in" />
                    <span class="text-info" data-bind="text: recentProjectsCount()"></span>
                    <g:message code="Project.plural" />:

                    <span data-bind="foreach: recentProjects">
                        <a href="${g.createLink(action:'index',controller:'menu',params:[project:'<$>'])}"
                           data-bind="urlPathParam: $data, text: $data"></a>
                    </span>
                </div>
                <div data-bind="if: recentUsersCount()>0">
                        <g:message code="by" />
                        <span class="text-info" data-bind="text: recentUsersCount">

                        </span>

                        <span data-bind="messageTemplate: recentUsersCount(),messageTemplatePluralize:true">
                            <g:message code="user" />:|<g:message code="user.plural" />:
                        </span>
                        <span data-bind="text: recentUsers().join(', ')">

                        </span>
                </div>
            </div>
        </div>
        <auth:resourceAllowed action="create" kind="project" context="application">
            <div class="col-sm-4">
                <g:link controller="framework" action="createProject" class="btn  btn-success pull-right">
                    <g:message code="page.home.new.project.button.label" />
>>>>>>> rundeck/development
                    <b class="glyphicon glyphicon-plus"></b>
                </g:link>
            </div>
        </auth:resourceAllowed>
    </div>
</div>
<<<<<<< HEAD
</g:if>
<div class="row row-space">
    <div class="col-sm-12">
    <div class="list-group">
<table id="CTable" class="table table-condensed table-hover table-striped style='width:100%'">
<thead bgcolor='#DDD'>
<td class="dtCol1"><b>Project(s)</b></td>
<td class="dtCol2"><b>Execution(s) past 24H</b></td>
<td class="dtCol3"><b>Action(s)</b></td>
</thead>
        <g:each in="${projectSummaries.sort{a,b->a.key<=>b.key}}" var="projectData">
<tr><td>
            <g:set var="project" value="${projectData.key}"/>
            <g:set var="data" value="${projectData.value}"/>
%{--Template for project details--}%

                        <i class="glyphicon glyphicon-tasks"></i>

                    <g:link action="index" controller="menu" params="[project: project]" class="h5">
                        <g:enc>${project}</g:enc></g:link>
                    <g:if test="${data.description}">
                        <span class="text-muted"><g:enc>${data.description}</g:enc></span>
                    </g:if>
        <g:if test="${data.readme?.readme || data.readme?.motd}">
            <div class="row row-space">
                <div class="col-sm-12">
                    <g:if test="${data.readme?.motd}">
                        %{--Test if user has dismissed the motd for this project--}%
                        <div class="">
                            <g:if test="${data.readme.motd && data.readme.readme}">
                            </g:if>
                            <g:if test="${data.readme.motdHTML}">
                                <g:enc raw="true">${data.readme.motdHTML}</g:enc>
                            </g:if>
                            <g:elseif test="${data.readme.motd}">
                                <g:enc>${data.readme.motd}</g:enc>
                            </g:elseif>
                        </div>
                    </g:if>
                    <g:if test="${data.readme?.readme}">
                        <g:if test="${data.readme.readmeHTML}">
                            <g:enc raw="true">${data.readme.readmeHTML}</g:enc>
                    </g:if>
                        <g:elseif test="${data.readme.readme}">
                            <g:enc>${data.readme.readme}</g:enc>
                        </g:elseif>

                    </g:if>
                </div>
                </div>
        </g:if>
        </td><td class="dtCol2">
                    <g:set var="execCount" value="${data.execCount}"/>
                    <g:if test="${data.execCount<=0}">
                        <g:set var="execCount" value="None"/>
                    </g:if>

                    <a class="${data.execCount > 0 ? 'btn btn-xs btn-link' : 'text-muted'}"
                       href="${g.createLink(controller: "reports", action: "index", params: [project: project])}"
                        <strong>
                        </strong>
                    <span class="${data.execCount > 0 ? 'badge' : 'text-muted'}">
                        <span class="summary-count"><g:enc>${execCount}</g:enc></span>
                    </span>
                        <g:if test="${data.userCount>0}">
                        by
                        <span class="text-info">
                        <g:enc>${data.userCount}</g:enc>
                        </span>
                            <g:plural code="User" count="${data.userCount}" textOnly="${true}"/>:
                            <g:each in="${data.userSummary}" var="user" status="i">
                                <g:enc>${user}</g:enc><g:if test="${i < data.userSummary.size() - 1}">,</g:if>
                            </g:each>
                        </g:if>
                    </a>

</td><td>
<%-- Add a quick link to Activity page --%>
                <a href="${g.createLink(controller: "reports", action: "index", params: [project: project])}"
                   class="btn btn-warning btn-xs has_Dtooltip" data-placement="bottom" data-container="body"  title data-original-title="View Project Activity">
                   <i class="glyphicon glyphicon-time"></i>
                </a>

                <g:if test="${data.auth?.jobCreate || data.auth?.admin}">
                        <g:if test="${data.auth?.admin}">
                            <a href="${g.createLink(controller: "menu", action: "admin", params: [project: project])}"
                               class="btn btn-info btn-xs has_Dtooltip" data-placement="bottom" title="Configure Project">
                               <i class="glyphicon glyphicon-cog"></i>
                            </a>
                        </g:if>
                        <div class="btn-group ">
                            <g:if test="${data.auth.jobCreate}">
                            <button type="button" class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
                                Create <g:message code="domain.ScheduledExecution.title"/>
                                <span class="caret"></span>
                            </button>
                            <ul class="dropdown-menu pull-right" role="menu">
                                <li><a href="${g.createLink(controller: "scheduledExecution", action: "create", params: [project: project])}">
                                    <i class="glyphicon glyphicon-plus"></i>
                                    New <g:message
                                        code="domain.ScheduledExecution.title"/>&hellip;

                                </a>
                                </li>
                                <li class="divider">
                                </li>
                                <li>
                                    <a href="${g.createLink(controller: "scheduledExecution", action: "upload", params: [project: project])}"
                                       class="">
                                        <i class="glyphicon glyphicon-upload"></i>
                                        Upload Definition&hellip;
                                    </a>
                                </li>
                            </ul>
                            </g:if>
                        </div>
        </g:if>
</div>
</td>
</g:each>
</tr></table>
    </div>
=======
<div data-bind="if: projectCount()<1 && loadedProjectNames()">
    <div class="row row-space">
        <div class="col-sm-12">
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
                    <h1><g:message code="page.home.welcome.to.app" args="${[g.appTitle()]}"/></h1>

                    <p>
                        <g:message code="page.home.get.started.message" />
                    </p>
                    <p>
                        <g:link controller="framework" action="createProject" class="btn  btn-success btn-lg ">
                            <g:message code="page.home.new.project.button.label" />
                            <b class="glyphicon glyphicon-plus"></b>
                        </g:link>
                    </p>
                </div>
            </auth:resourceAllowed>
        </div>
    </div>
</div>

<div class="row row-space">
    <div class="col-sm-12">
        <div class="list-group">
            <div data-bind="if: !loadedProjectNames() && projectCount()<1">
            <div class="list-group-item">
                <g:message code="page.home.loading.projects" />
                <asset:image src="spinner-gray.gif" width="32px" height="32px"/>
            </div>
            </div>
            <div data-bind="if: projectCount()>0">
            <div class="list-group-item">
                <div class="row">
                    <div class="col-sm-12  form-inline">
                        <div class="form-group  ">

                            <label>
                                <g:icon name="search"/>
                                <input
                                        type="search"
                                        name="search"
                                        placeholder="${message(code:"page.home.search.projects.input.placeholder")}"
                                    class="form-control input-sm"
                                    data-bind="value: search"
                                />
                            </label>
                            <span data-bind="if: search()">
                            <span data-bind="messageTemplate: searchedProjectsCount(), messageTemplatePluralize:true, css: { 'text-info': searchedProjectsCount()>0, 'text-warning': searchedProjectsCount()<1 }">
                                <g:message code="page.home.search.project.title" />|<g:message code="page.home.search.project.title.plural" />
                            </span>
                            </span>
                        </div>
                    </div>
                </div>
            </div>
            </div>
            <div data-bind="foreach: { data: searchedProjects(), as: 'project' } ">
            %{--Template for project details--}%
                <div class="list-group-item project_list_item" data-bind="attr: { 'data-project': project }, ">
                <div class="row">
                        <div class="col-sm-6 col-md-4">
                            <a href="${g.createLink(action:'index',controller:'menu',params:[project:'<$>'])}"
                                data-bind="urlPathParam: project"
                               class="h3">
                                <i class="glyphicon glyphicon-tasks"></i>
                                <span data-bind="text: project"></span>
                            </a>

                            <span data-bind="if: $root.projectForName(project)">
                                <span class="text-muted" data-bind="text: $root.projectForName(project).description"></span>
                            </span>
                        </div>

                        <div class="clearfix visible-sm"></div>
                        <div class="col-sm-6 col-md-4">
                            <span data-bind="if: $root.projectForName(project)">
                            <a
                                    class="h4"
                                    data-bind="css: { 'text-muted': $root.projectForName(project).execCount()<1 }, urlPathParam: project "
                                    href="${g.createLink(controller: "reports", action: "index", params: [project: '<$>'])}"
                            >
                                <span class="summary-count "
                                      data-bind="css: { 'text-muted': $root.projectForName(project).execCount()<1, 'text-info':$root.projectForName(project).execCount()>0 } "
                                >
                                        <span data-bind="text: $root.projectForName(project).loaded()?$root.projectForName(project).execCount():''"></span>
                                        <span data-bind="if: !$root.projectForName(project).loaded()" >...</span>
                                </span>
                                <span data-bind="messageTemplate: $root.projectForName(project).execCount(), messageTemplatePluralize: true">
                                    <g:message code="Execution" />|<g:message code="Execution.plural" />
                                </span>
                                <g:message code="page.home.duration.in.the.last.day" />
                            </a>
                            <div>
                                <div data-bind="if: $root.projectForName(project).userCount()>0">
                                    <g:message code="by" />
                                    <span class="text-info" data-bind="text: $root.projectForName(project).userCount()">
                                    </span>

                                    <span data-bind="messageTemplate: $root.projectForName(project).userCount(),messageTemplatePluralize:true">
                                        <g:message code="user" />:|<g:message code="user.plural" />:
                                    </span>

                                    <span data-bind="text: $root.projectForName(project).userSummary().join(', ')">

                                    </span>
                                </div>
                            </div>
                            </span>
                        </div>



                        <div class="clearfix visible-xs visible-sm"></div>
                        <div data-bind="if: $root.projectForName(project)">


                            <div class="col-sm-12 col-md-4" >
                                <div class="pull-right">
                                    <span data-bind="if: !$root.projectForName(project).loaded()">
                                        <g:img file="spinner-gray.gif" width="24px" height="24px"/>
                                    </span>
                                    <span data-bind="if: $root.projectForName(project).auth().admin">
                                        <a href="${g.createLink(controller: "menu", action: "admin", params: [project: '<$>'])}"
                                            data-bind="urlPathParam: project"
                                           class="btn btn-default btn-sm">
                                            <g:message code="gui.menu.Admin"/>
                                        </a>
                                    </span>
                                    <div class="btn-group " data-bind="if: $root.projectForName(project).auth().jobCreate">

                                            <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                                                <g:message code="create.job.button.label" />
                                                <span class="caret"></span>
                                            </button>
                                            <ul class="dropdown-menu pull-right" role="menu">
                                                <li><a href="${g.createLink(controller: "scheduledExecution", action: "create", params: [project: '<$>'])}"
                                                       data-bind="urlPathParam: project"
                                                >
                                                    <i class="glyphicon glyphicon-plus"></i>
                                                    <g:message code="new.job.button.label" />

                                                </a>
                                                </li>
                                                <li class="divider">
                                                </li>
                                                <li>
                                                    <a href="${g.createLink(controller: "scheduledExecution", action: "upload", params: [project: '<$>'])}"
                                                       data-bind="urlPathParam: project"
                                                       class="">
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

                    <div data-bind="if: $root.projectForName(project)">
                        <div class="row row-space" data-bind="if: $root.projectForName(project).readme && ($root.projectForName(project).readme().readmeHTML || $root.projectForName(project).readme().motdHTML)">
                            <div class="col-sm-12">
                                <div data-bind="if: $root.projectForName(project).readme().motdHTML()">
                                    <span data-bind="if: $root.projectForName(project).readme().motdHTML()">
                                        <span data-bind="html: $root.projectForName(project).readme().motdHTML()"></span>
                                    </span>
                                </div>
                                <div data-bind="if: $root.projectForName(project).readme().readmeHTML()">
                                    <span data-bind="if: $root.projectForName(project).readme().readmeHTML()">
                                        <span data-bind="html: $root.projectForName(project).readme().readmeHTML()"></span>
                                    </span>
                                </div>

                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
>>>>>>> rundeck/development
    </div>
</div>
</div>

</body>
</html>
