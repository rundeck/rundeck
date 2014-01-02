<ul class="nav nav-pills nav-stacked">
    <li class="${selected=='project'?'active':''}">
        <g:link controller="menu" action="admin" params="[project: params.project ?: request.project]">
            <g:message code="gui.menu.ProjectConfiguration" default="Project Configuration"/>
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
    <li class="${selected == 'plugins' ? 'active' : ''}">
        <g:link controller="menu" action="plugins" params="[project: params.project ?: request.project]">
            <g:message code="gui.menu.ListPlugins" />
        </g:link>
    </li>
    <li class="${selected == 'licenses' ? 'active' : ''}">
        <g:link controller="menu" action="licenses">
            <g:message code="licenses"/>
        </g:link>
    </li>
</ul>
