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
        <h4><g:message code="scm.export.title"/></h4>
        <span class="help-block">
            <g:message code="scm.export.plugins.help"/>
        </span>

        <g:if test="${pluginConfig && pluginConfig.type && pluginConfig.enabled && configuredPlugin}">
        %{--Disable plugin modal--}%
            <g:form useToken="true">
                <div class="modal fade" id="disablePlugin" role="dialog" aria-labelledby="disablePluginModalLabel"
                     aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal"
                                        aria-hidden="true">&times;</button>
                                <h4 class="modal-title" id="disablePluginModalLabel">
                                    <g:message code="scmController.action.disable.confirm.title"/>
                                </h4>
                            </div>

                            <div class="modal-body container">
                                <div class="form-group">
                                    <label class="control-label col-sm-2">
                                        <g:message code="plugin"/>:
                                    </label>

                                    <div class="col-sm-10">
                                        <span class="form-control-static">
                                            ${configuredPlugin.description.title}
                                        </span>
                                        <g:hiddenField name="type" value="${pluginConfig.type}"/>
                                        <g:hiddenField name="project" value="${params.project}"/>
                                        <g:hiddenField name="integration" value="export"/>
                                    </div>
                                </div>

                            </div>

                            <div class="modal-body">
                                <span class="text-danger"><g:message code="plugin.disable.confirm.text" /></span>
                            </div>

                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal">
                                    <g:message code="no"/>
                                </button>
                                <g:actionSubmit action="disable" value="${message(code: 'yes')}" formmethod="POST"
                                                class="btn btn-danger"/>
                            </div>
                        </div><!-- /.modal-content -->
                    </div><!-- /.modal-dialog -->
                </div><!-- /.modal -->
            </g:form>
        </g:if>
        <g:if test="${pluginConfig && pluginConfig.type && !pluginConfig.enabled && configuredPlugin}">
        %{--Enable plugin modal--}%
            <g:form useToken="true">
                <div class="modal fade" id="enablePlugin" role="dialog" aria-labelledby="enablePluginModalLabel"
                     aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal"
                                        aria-hidden="true">&times;</button>
                                <h4 class="modal-title" id="enablePluginModalLabel">
                                    <g:message code="scmController.action.enable.confirm.title"/>
                                </h4>
                            </div>

                            <div class="modal-body container">
                                <div class="form-group">
                                    <label class="control-label col-sm-2">
                                        <g:message code="plugin"/>:
                                    </label>

                                    <div class="col-sm-10">
                                        <span class="form-control-static">
                                            ${configuredPlugin?.description.title}
                                        </span>
                                        <g:hiddenField name="type" value="${pluginConfig.type}"/>
                                        <g:hiddenField name="project" value="${params.project}"/>
                                        <g:hiddenField name="integration" value="export"/>
                                    </div>
                                </div>

                            </div>

                            <div class="modal-body">
                                <span class="text-danger"><g:message code="plugin.enable.confirm.text" /></span>
                            </div>

                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal">
                                    <g:message code="no"/>
                                </button>
                                <g:actionSubmit action="enable" value="${message(code: 'yes')}" formmethod="POST"
                                                class="btn btn-success"/>
                            </div>
                        </div><!-- /.modal-content -->
                    </div><!-- /.modal-dialog -->
                </div><!-- /.modal -->
            </g:form>
        </g:if>

        <g:if test="${plugins}">
            <div class="list-group">

                <g:each in="${plugins.keySet().sort()}" var="pluginName">
                    <g:set var="isConfigured" value="${pluginConfig && pluginConfig.type == pluginName}"/>
                    <g:set var="isConfiguredButDisabled" value="${isConfigured && !pluginConfig.enabled}"/>
                    <g:set var="isConfiguredAndEnabled" value="${isConfigured && pluginConfig.enabled}"/>

                    <div class="list-group-item">

                        <h4 class="list-group-item-heading">
                            ${plugins[pluginName].description.title}

                            <g:if test="${isConfiguredButDisabled}">
                                <span class="badge"><g:message code="badge.Disabled.title"/></span>
                            </g:if>

                            <g:if test="${isConfiguredAndEnabled}">
                                <span class="badge badge-success">
                                    <g:icon name="check"/>
                                    <g:message code="badge.Enabled.title"/>
                                </span>
                            </g:if>
                        </h4>

                        <div class="list-group-item-text">

                            <g:render template="/framework/renderPluginConfig"
                                      model="${[
                                              values     : isConfigured ? pluginConfig.config : [:],
                                              description: plugins[pluginName].description,
                                              hideTitle  : true
                                      ]}"/>

                            <g:if test="${isConfiguredButDisabled}">

                                <span
                                        class="btn  btn-success"
                                        data-toggle="modal"
                                        data-target="#enablePlugin">
                                    <g:message code="button.Enable.title"/>
                                </span>
                            </g:if>
                            <g:elseif test="${isConfiguredAndEnabled}">
                                <span
                                        class="btn  btn-warning"
                                        data-toggle="modal"
                                        data-target="#disablePlugin">
                                    <g:message code="button.Disable.title"/>
                                </span>
                            </g:elseif>

                            <g:link action="setup"
                                    class="btn  ${isConfiguredButDisabled ? 'btn-default' : 'btn-success'}"
                                    params="[type: plugins[pluginName].name, project: params.project]">

                                <g:icon name="cog"/>
                                <g:if test="${isConfiguredButDisabled || isConfiguredAndEnabled}">
                                    <g:message code="button.Configure.title"/>
                                </g:if>
                                <g:else>
                                    <g:message code="button.Setup.title"/>
                                </g:else>

                            </g:link>
                        </div>

                    </div>
                </g:each>
            </div>
        </g:if>
    </div>
</div>
</body>
</html>