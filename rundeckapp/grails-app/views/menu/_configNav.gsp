<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<ul class="nav nav-pills nav-stacked">
    <g:if test="${params.project}">
        <li class="${selected == 'project' ? 'active' : ''} " >
           <g:link controller="menu" action="admin" params="[project: params.project ]">
            <g:message code="gui.menu.ProjectConfiguration" default="Project Configuration" args="${[params.project]}"/>
        </g:link>
       </li>
        <li class="${selected == 'scm' ? 'active' : ''}">
            <g:link controller="scm" action="index"
                    params="[project: params.project ?: request.project]">
                <g:message code="gui.menu.SCM" default="SCM"/>
            </g:link>
        </li>

    </g:if>
    <g:else>
    <g:if test="${session.frameworkProjects}">
        <li class="${selected == 'project' ? 'active' : ''} dropdown" id="projectSelect">
            <g:render template="/framework/projectSelect"
                      model="${[projects: session.frameworkProjects, project: params.project ?:
                          request.project,
                              selectParams: [page:'configure'],selectItemTitle:'Project Configuration: ']}"/>
        </li>
    </g:if>
    <g:else>
        <li id="projectSelect" class="${selected == 'project' ? 'active' : ''} dropdown">
            <span class="action textbtn" onclick="loadProjectSelect({page:'configure'});"
                  title="Select project...">
                    <g:message code="project.configuration" />
                <g:enc>${params.project ?: request.project ?: 'Select project...'}</g:enc>
            </span>
        </li>
    </g:else>
</g:else>
    <g:if test="${auth.resourceAllowedTest(type: 'resource',kind:'system',
                    action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN], any: true, context: 'application')}">
        <li class="${selected == 'storage' ? 'active' : ''}">
            <g:link controller="menu" action="storage"
                    params="[project: params.project ?: request.project]">
                <g:message code="gui.menu.KeyStorage" default="Key Storage"/>
            </g:link>
        </li>
        <li class="${selected == 'syscfg' ? 'active' : ''}">
            <g:link controller="menu" action="systemConfig" params="[project: params.project ?: request.project]">
                <g:message code="gui.menu.SystemConfig" default="System Configuration"/>
            </g:link>
        </li>
        <li class="${selected == 'securityConfig' ? 'active' : ''}">
            <g:link controller="menu" action="securityConfig" params="[project: params.project ?: request.project]">
                <g:message code="gui.menu.Security" default="Security"/>
            </g:link>
        </li>
        <li class="${selected == 'sysinfo' ? 'active' : ''}">
            <g:link controller="menu" action="systemInfo" params="[project: params.project ?: request.project]">
                <g:message code="gui.menu.SystemInfo" default="System Report"/>
            </g:link>
        </li>

        <g:if test="${g.logStorageEnabled() || selected=='logstorage'}">

        <li class="${selected == 'logstorage' ? 'active' : ''}">
            <g:link controller="menu" action="logStorage" params="[project: params.project ?: request.project]">
                <g:message code="gui.menu.LogStorage" default="Log Storage"/>
            </g:link>
        </li>

        </g:if>

    </g:if>
    <li class="${selected == 'plugins' ? 'active' : ''}">
        <g:link controller="menu" action="plugins" params="[project: params.project ?: request.project]">
            <g:message code="gui.menu.ListPlugins"/>
        </g:link>
    </li>
    <li class="${selected == 'licenses' ? 'active' : ''}">
        <g:link controller="menu" action="licenses">
            <g:message code="licenses"/>
        </g:link>
    </li>
</ul>

<g:render template="/menu/configExecutionMode"/>