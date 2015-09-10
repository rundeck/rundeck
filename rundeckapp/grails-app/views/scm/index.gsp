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
    <title><g:appTitle/> - <g:message code="scmController.page.index.title" args="[params.project]"/></></title>

</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
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
                <g:message code="scmController.page.index.description" default="Enable or configure SCM integration."/>
            </div>
        </div>
        <g:each in="['export','import']" var="integration">
            <g:render template="pluginConfigList" model="[
                    integration:integration,
                    pluginConfig:pluginConfig[integration],
                    enabled:enabled[integration],
                    configuredPlugin:configuredPlugin[integration],
                    plugins:plugins[integration]
            ]"/>
        </g:each>
    </div>
</div>
</body>
</html>