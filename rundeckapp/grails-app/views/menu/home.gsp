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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <g:set var="appTitle"
       value="${grailsApplication.config.rundeck.gui.title ? grailsApplication.config.rundeck.gui.title : g.message(code: 'main.app.name')}"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="home"/>
    <title>${appTitle}</title>

</head>
<body>


<div class="row">
<div class="col-sm-12">
                <span class="h3">
                    Rundeck
                    server:

                <span class="text-info ">${frameworkNodeName.encodeAsHTML()}</span>

                </span>
</div>
</div>
        <div class="row row-space">
            <div class="col-sm-2">
                <span class="h4">
                    <span class="summary-count ${ jobCount > 0 ? 'text-info' : 'text-muted' }">${projCount}</span>
                    <g:message code="Project${projCount == 1 ? '' : '.plural'}"/>
                </span>
            </div>


            <div class="col-sm-2">
                <span class="h4">
                    <span class="summary-count ${jobCount > 0 ? 'text-info' : 'text-muted'}">${jobCount}</span>
                    <g:message code="Job${jobCount == 1 ? '' : '.plural'}"/>
                    <i class="glyphicon glyphicon-book"></i>
                </span>
            </div>


            <div class="col-sm-3">
                <span class="h4">
                    <span class="summary-count ${ execCount > 0 ? 'text-info' : 'text-muted' }">${execCount}</span>
                    <strong>
                        <g:message code="Execution${execCount == 1 ? '' : '.plural'}"/>
                    </strong>
                    In the last day
                </span>
                <g:if test="${projectSummaries.size()>0}">
                <div>
                in
                <span class="text-info">
                    ${projectSummary.size()}
                </span>


                <g:message code="Project${projectSummary.size() == 1 ? '' : '.plural'}"/>
                <g:each var="project" in="${projectSummary}" status="i">
                ${project}<g:if test="${i< projectSummary.size()-1}">,</g:if>
                </g:each>
                </div>
                </g:if>
                <div>
                    <g:if test="${userCount>0}">
                    by
                    <span class="text-info">
                        ${userCount}
                    </span>
                    <g:message code="user${userCount == 1 ? '' : '.plural'}"/>
                    <g:each in="${userSummary}" var="user" status="i">
                    ${user.encodeAsHTML()}<g:if test="${i<userSummary.size()-1}">,</g:if>
                    </g:each>
                    </g:if>
                </div>
            </div>
    </div>


<div class="row row-space">
    <div class="col-sm-9">
        <span class="h3 text-muted">
            <g:message code="Project.plural" />
        </span>
    </div>
    <auth:resourceAllowed action="create" kind="project" context="application">
        <div class="col-sm-3">
            <g:link controller="framework" action="createProject" class="btn  btn-success pull-right">
                New Project
                <b class="glyphicon glyphicon-plus"></b>
            </g:link>
        </div>
    </auth:resourceAllowed>
</div>

<div class="row row-space">
    <div class="col-sm-12">

        <g:each in="${projectSummaries}" var="projectData">
            <g:set var="project" value="${projectData.key}"/>
            <g:set var="data" value="${projectData.value}"/>
%{--Template for project details--}%
<div class="panel panel-default">
    <div class="panel-body">
            <div class="row">
                <div class="col-sm-6 col-md-2">
                    <a class="h3"
                       href="${g.createLink(controller: "framework", action: "selectProject", params: [project: project])}">
                        <i class="glyphicon glyphicon-tasks"></i>
                    ${project}
                    </a>
                </div>
                <div class="col-sm-6 col-md-2">
                    <a class="h4 ${data.jobCount > 0 ? '' : 'text-muted'}" href="${g.createLink(controller:"framework",action:"selectProject",params:[page: 'jobs',project:project])}">
                        <span class="summary-count ${data.jobCount > 0 ? 'text-info' : '' }">${data.jobCount}</span>

                        <g:plural code="Job" count="${data.jobCount}" textOnly="${true}"/>
                        <i class="glyphicon glyphicon-book"></i>
                    </a>
                </div>
                <div class="clearfix visible-sm"></div>
                <div class="col-sm-6 col-md-3">
                    <a class="h4 ${data.execCount > 0 ? '' : 'text-muted'}"
                       href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'activity', project: project])}"

                    >
                        <span class="summary-count ${data.execCount > 0 ? 'text-info' : '' }">${data.execCount}</span>
                        <strong>
                            <g:plural code="Execution" count="${data.execCount}" textOnly="${true}"/>
                        </strong>
                        In the last day
                    </a>
                    <div>
                        <g:if test="${data.userCount>0}">
                        by
                        <span class="text-info">
                        ${data.userCount}
                        </span>

                            <g:plural code="user" count="${data.userCount}" textOnly="${true}"/>

                            <g:each in="${data.userSummary}" var="user" status="i">
                                ${user.encodeAsHTML()}<g:if test="${i < data.userSummary.size() - 1}">,</g:if>
                            </g:each>
                        </g:if>
                    </div>
                </div>


                <div class="col-sm-6 col-md-2">
                    <a class="h4" href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'nodes',project:project])}">
                        <span class="summary-count ${data.nodeCount>0?'text-info':''}">
                            ${data.nodeCount}
                        </span>
                        <g:plural code="Node" count="${data.nodeCount}" textOnly="${true}"/>
                    </a>
                </div>

                <div class="clearfix visible-xs visible-sm"></div>
                <g:if test="${data.auth?.jobCreate || data.auth?.admin}">
                    <div class="col-sm-12 col-md-3">
                    <div class="pull-right">
                        <g:if test="${data.auth?.admin}">
                            <a href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'configure', project: project])}"
                               class="btn btn-default btn-sm">
                                <g:message code="gui.menu.Admin"/>
                            </a>
                        </g:if>
                        <div class="btn-group ">

                            <g:if test="${data.auth.jobCreate}">
                            <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                                Create <g:message code="domain.ScheduledExecution.title"/>
                                <span class="caret"></span>
                            </button>
                            <ul class="dropdown-menu pull-right" role="menu">
                                <li><a href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'createJob', project: project])}">
                                    <i class="glyphicon glyphicon-plus"></i>
                                    New <g:message
                                        code="domain.ScheduledExecution.title"/>&hellip;

                                </a>
                                </li>
                                <li class="divider">
                                </li>
                                <li>
                                    <a href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'uploadJob', project: project])}"
                                       class="">
                                        <i class="glyphicon glyphicon-upload"></i>
                                        Upload Definition&hellip;
                                    </a>
                                </li>
                            </ul>
                            </g:if>
                        </div>
                        </div>
                    </div>
                </g:if>
            </div>

    </div>
        <g:if test="${data.readme?.readme || data.readme?.motd}">
            <div class="panel-body">
                    <g:if test="${data.readme.readmeHTML}">
                        ${data.readme.readmeHTML}
                    </g:if>
                    <g:elseif test="${data.readme.readme}">
                        ${data.readme.readme.encodeAsHTML()}
                    </g:elseif>

                    <g:if test="${data.readme.motd && data.readme.readme}">
                        <hr/>
                    </g:if>
                    <g:if test="${data.readme.motdHTML}">
                        ${data.readme.motdHTML}
                    </g:if>
                    <g:elseif test="${data.readme.readme}">
                        ${data.readme.readme.motdAsHTML()}
                    </g:elseif>
            </div>
        </g:if>
</div>

</g:each>
    </div>
</div>

</body>
</html>
