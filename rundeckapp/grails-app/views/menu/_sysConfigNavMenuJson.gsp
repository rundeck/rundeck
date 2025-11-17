<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<g:set var="authAclRead" value="${auth.resourceAllowedTest(
        type: AuthConstants.TYPE_RESOURCE,
        kind: AuthConstants.TYPE_SYSTEM_ACL,
        action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants
                .ACTION_OPS_ADMIN],
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
<g:set var="logfileStoragePlugin" value="${cfg.getString(config: 'execution.logs.fileStoragePlugin', default: null)}"/>
<g:set var="links" value="${[
        [url: g.createLink(controller: 'menu', action: 'storage'), title: g.message(code: 'gui.menu.KeyStorage')],

        [enabled: authAclRead, url: g.createLink(controller: 'menu', action: 'acls'), title: g
                .message(code: 'gui.menu.AccessControl')],

        [enabled: logfileStoragePlugin, url: g.createLink(controller: 'menu', action: 'logStorage'), title: g
                .message(code: 'gui.menu.LogStorage')],
        [
                enabled: pluginRead && repoEnabled,
                title  : g.message(code: 'gui.menu.Plugins', default: 'Plugins'),
                links  : [
                        [url: g.createLink(uri: '/artifact/index/repositories'), title: g
                                .message(code: 'gui.menu.FindPlugins')],
                        [url: g.createLink(uri: '/artifact/index/configurations'), title: g
                                .message(code: 'gui.menu.InstalledPlugins')],
                        [enabled: !pluginSecurityEnabled, url: g.createLink(uri: '/artifact/index/upload'), title: g
                                .message(code: 'gui.menu.UploadPlugin')],
                ]
        ],
        [
                enabled: pluginRead && !repoEnabled,
                url    : g.createLink(uri: '/artifact/index/configurations'),
                title  : g.message(code: 'gui.menu.InstalledPlugins')
        ],
        [
                enabled: pluginInstall && !repoEnabled && !pluginSecurityEnabled,
                url    : g.createLink(uri: '/artifact/index/upload'),
                title  : g.message(code: 'gui.menu.UploadPlugin')
        ],
        [
                url  : g.createLink(controller: "passwordUtility", action: 'index'),
                title: g.message(code: 'gui.menu.PasswordUtility')
        ],
]}"/>
<g:set var="navMenuComponents" value="${[]}"/>
<g:forMenuItems type="SYSTEM_CONFIG" var="item" groupvar="group">
    %{
        navMenuComponents << [
                url : item.href,
                title: g.message(code: item.titleCode, default: item.title),
                group: (group && group.id) ? [
                        id       : group.id,
                ] : null
        ]

    }%
</g:forMenuItems>
%{
    if (navMenuComponents.size() > 0) {
        links << [separator:true];
        links.addAll(navMenuComponents)
    }
    links.add( [
            url    : g.createLink(controller: 'menu', action: 'executionMode'),
            title  : g.message(code: 'gui.menu.ExecutionMode'),
            icon   : g.executionMode(active: true) ? 'play' : 'pause',
            iconCss: g.executionMode(active: true) ? 'text-success' : 'text-warning',
            order:1000,
            separator:true
    ])
}%
<g:embedJSON id="sysConfigNavJSON" data="${links}"/>