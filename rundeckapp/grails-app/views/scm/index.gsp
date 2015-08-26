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
    <meta name="layout" content="base" />
    <title><g:appTitle/> - scm ${params.project}</title>

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
        <g:render template="/menu/configNav" model="[selected:'scm']"/>
    </div>
    <div class="col-sm-9">
        <g:if test="${plugin}">
            <g:render template="/framework/renderPluginConfig"
                      model="${[values: config, description: plugin.description, hideTitle: false]}"
                />
        </g:if>
        <g:if test="${plugins && !plugin}">
            <ul class="list-unstyled">
                <g:each in="${plugins.keySet().sort()}" var="pluginName" >
                    <li>

                        <g:enc>${plugins[pluginName].description?.title?:plugins[pluginName].name}</g:enc>
                        <g:link action="setup" class="btn btn-sm btn-default" params="[type:plugins[pluginName].name,project:params.project]">
                                <i class="glyphicon glyphicon-plus"></i>
                            Setup
                        </g:link>
                        <span class="help-block"><g:enc>${plugins[pluginName].description?.description}</g:enc></span>
                    </li>
                </g:each>
            </ul>
        </g:if>
        </div>
    </div>
</body>
</html>