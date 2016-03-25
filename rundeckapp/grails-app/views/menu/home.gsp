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
    <asset:stylesheet href="dtable.css"/>
    <asset:javascript src="jquery.js"/>
    <script>jQuery.noConflict(true);</script>
    <asset:javascript src="jquery.dataTables.js"/>
    <asset:javascript src="DT_bootstrap.js"/>
    <asset:javascript src="dtable.js"/>
    <asset:javascript src="menu/home.js"/>

</head>
<body>

<div class="row">
<div class="col-sm-12">
    <g:render template="/common/messages"/>
</div>
</div>

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
                    <b class="glyphicon glyphicon-plus"></b>
                </g:link>
            </p>
        </div>
    </auth:resourceAllowed>
</div>
</div>
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
    </div>
</div>
</div>

</body>
</html>
