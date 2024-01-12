<%@ page import="org.rundeck.core.auth.AuthConstants" %>

<g:set var="authAclRead" value="${auth.resourceAllowedTest(
        type: AuthConstants.TYPE_RESOURCE,
        kind: AuthConstants.TYPE_SYSTEM_ACL,
        action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_OPS_ADMIN],
        any: true,
        context: AuthConstants.CTX_APPLICATION
)}"/>

<g:set var="opsAdminRead" value="${auth.resourceAllowedTest(
        type: AuthConstants.TYPE_RESOURCE,
        kind: AuthConstants.TYPE_SYSTEM,
        action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN],
        any: true,
        context: AuthConstants.CTX_APPLICATION
)}"/>

<g:set var="pluginRead" value="${auth.resourceAllowedTest(
        type: AuthConstants.TYPE_RESOURCE,
        kind: AuthConstants.TYPE_PLUGIN,
        action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
        any: true,
        context: AuthConstants.CTX_APPLICATION
)}"/>
<g:set var="pluginInstall" value="${auth.resourceAllowedTest(
        type: AuthConstants.TYPE_RESOURCE,
        kind: AuthConstants.TYPE_PLUGIN,
        action: [AuthConstants.ACTION_INSTALL, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
        any: true,
        context: AuthConstants.CTX_APPLICATION
)}"/>

<g:set var="repoEnabled" value="${cfg.getBoolean(config: "feature.repository.enabled", default: false)}"/>
<g:set var="pluginSecurityEnabled" value="${cfg.getBoolean(config: "feature.pluginSecurity.enabled", default: false)}"/>

<div>
    <ul class="dropdown-menu dropdown-menu-right scroll-area" style="width: 65vw; height: 50vh;">
        <div class="vue-ui-socket" style="width: 100%; height: 100%;">
            <ui-socket section="project-notification-center" location="main" :socket-data="">
            </ui-socket>
        </div>
    </ul>
</div>