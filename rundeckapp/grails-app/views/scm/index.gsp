<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 4/30/15
  Time: 3:27 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="configure"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - scm ${params.project}</></title>

</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
        <g:if test="${flash.joberrors}">
            <ul class="error note">
                <g:each in="${flash.joberrors}" var="errmsg">
                    <li><g:enc>${errmsg}</g:enc></li>
                </g:each>
            </ul>
        </g:if>
    </div>
</div>

<div class="row">
    <div class="col-sm-3">
        <g:render template="/menu/configNav" model="[selected: 'scm']"/>
    </div>

<div class="col-sm-9">
    <h3><g:message code="gui.menu.Scm" default="Project SCM Integration"/></h3>

    <div class="well well-sm">
        <div class="text-info">
            <g:message code="page.scmIndex.description" default="Enable or configure SCM integration."/>
        </div>
    </div>
    <h4>SCM Export</h4>
    <g:if test="${configuredPlugin}">
        <h5>Configured for Project</h5>
        <g:render template="/framework/renderPluginConfig"
                  model="${[values: config, description: configuredPlugin.description, hideTitle: false]}"/>
        <button class="btn btn-warning">Disable</button>
    </g:if>
    <g:if test="${plugins}">
        <span class="help-block">Choose a plugin to setup</span>

        <div class="list-group">

    %{--<ul class="list-unstyled">--}%
        <g:each in="${plugins.keySet().sort()}" var="pluginName">
            <div class="list-group-item">
                <g:set var="isConfigured" value="${pluginConfig && pluginConfig.type == pluginName}"/>
                <g:set var="isConfiguredButDisabled" value="${isConfigured && !pluginConfig.enabled}"/>
                <g:set var="isConfiguredAndEnabled" value="${isConfigured && pluginConfig.enabled}"/>

                <g:if test="${isConfiguredButDisabled}">
                    <span class="badge">Disabled</span>
                </g:if>

                <g:if test="${isConfiguredAndEnabled}">
                    <span class="badge badge-success">Enabled</span>
                </g:if>

                <h4 class="list-group-item-heading">${plugins[pluginName].description.title}</h4>

                <div class="list-group-item-text">

                    <g:if test="${isConfigured}">
                        <g:render template="/framework/renderPluginConfig"
                                  model="${[values     : pluginConfig.config,
                                            description: plugins[pluginName].description, hideTitle: true]}"/>
                        <g:if test="${isConfiguredButDisabled}">

                            <g:link action="enable" class="btn  btn-success"
                                    params="[integration: 'export', type: plugins[pluginName].name, project: params.project]">
                                Enable
                            </g:link>
                        </g:if>
                        <g:else>
                            <g:link action="disable" class="btn  btn-warning"
                                    params="[integration: 'export', type: plugins[pluginName].name, project: params.project]">
                                Disable
                            </g:link>
                        </g:else>
                    </g:if>
                    <g:else>
                        <g:render template="/framework/renderPluginConfig"
                                  model="${[values     : [:],
                                            description: plugins[pluginName].description, hideTitle: true]}"/>
                    </g:else>
                    <g:link action="setup" class="btn  ${isConfiguredButDisabled ? 'btn-default' : 'btn-success'}"
                            params="[type: plugins[pluginName].name, project: params.project]">
                        <i class="glyphicon glyphicon-cog"></i>
                        <g:if test="${isConfiguredButDisabled || isConfiguredAndEnabled}">
                            Configure
                        </g:if>
                        <g:else>
                            Setup
                        </g:else>
                    </g:link>
                </div>

            </div>
            </></g:each>
    %{--</ul>--}%
        </div>
    </g:if>
</div>
</div>
</body>
</html>