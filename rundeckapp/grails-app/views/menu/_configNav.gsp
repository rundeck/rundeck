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
    <li class="${selected == 'profiles' ? 'active' : ''}">
        <g:link controller="user" action="list">
            <g:message code="gui.menu.UserProfiles" default="User Profiles"/>
        </g:link>
    </li>
    <li class="${selected == 'plugins' ? 'active' : ''}">
        <g:set var="pluginParams"
               value="${[utm_source: 'rundeckapp', utm_medium: 'app', utm_campaign: 'getpluginlink'].collect { k, v -> k + '=' + v }.join('&')}"/>

        <g:set var="pluginUrl" value="http://rundeck.org/plugins/?${pluginParams}"/>
        <g:set var="pluginLinkUrl"
               value="${org.codehaus.groovy.grails.commons.ConfigurationHolder.config.rundeck?.gui?.pluginLink ?: pluginUrl}"/>
        <a href="${pluginLinkUrl}">
            <g:message code="gui.admin.GetPlugins" default="Get Plugins"/>
        </a>
    </li>
</ul>
