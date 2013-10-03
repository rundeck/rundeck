<ul class="nav nav-pills nav-stacked">
    <li class="${selected=='project'?'active':''}">
        <g:link controller="menu" action="admin">
            <g:message code="gui.menu.SystemInfo" default="Project Configuration"/>
        </g:link>
    </li>
    <li class="${selected == 'sysinfo' ? 'active' : ''}">
        <g:link controller="menu" action="systemInfo">
            <g:message code="gui.menu.SystemInfo" default="System Information"/>
        </g:link>
    </li>
    <li class="${selected == 'metrics' ? 'active' : ''}">
        <g:link controller="menu" action="metrics">
            <g:message code="gui.menu.metrics" default="Metrics"/>
        </g:link>
    </li>
    <li class="${selected == 'profiles' ? 'active' : ''}">
        <g:link controller="user" action="list">
            <g:message code="gui.menu.UserProfiles" default="User Profiles"/>
        </g:link>
    </li>
    <li class="${selected == 'plugins' ? 'active' : ''}">
        <g:link controller="menu" action="plugins">
            List Plugins
        </g:link>
    </li>
    <li class="${selected == 'licenses' ? 'active' : ''}">
        <g:link controller="menu" action="licenses">
            <g:message code="licenses"/>
        </g:link>
    </li>
</ul>
