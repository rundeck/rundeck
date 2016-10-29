%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 4/30/15
  Time: 3:29 PM
--%>

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="configure"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="scmController.page.setup.title" args="[params.project]"/></title>
    <asset:javascript src="storageBrowseKO.js"/>
    <g:javascript>

        var configControl;
        function init() {
            $$('input').each(function (elem) {
                if (elem.type == 'text') {
                    elem.observe('keypress', noenter);
                }
            });
        }
        jQuery(init);
    </g:javascript>
</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>

<div class="row">
    <div class="col-sm-12 col-md-10 col-md-offset-1 col-lg-8 col-lg-offset-2">
        <g:form action="saveSetup"
                params="${[project: params.project, type: type, integration: integration]}"
                useToken="true"
                method="post" class="form form-horizontal">
            <g:set var="serviceName" value="${integration=='export'?'ScmExport':'ScmImport'}"/>
            <div class="panel panel-primary" id="createform">
                <div class="panel-heading">
                    <span class="h3">
                        <g:message code="scmController.page.setup.description" />:

                        <stepplugin:message
                                service="${serviceName}"
                                name="${type}"
                                code="plugin.title"
                                default="${plugin.description?.title?:plugin.name}"/>
                    </span>
                </div>

                <div class="list-group">
                    <div class="list-group-item">
                        <div class="form-group">
                            <label class="control-label col-sm-2 input-sm"><g:message code="Project"/></label>

                            <div class="col-sm-10">
                                <span class="form-control-static">${params.project}</span>
                            </div>
                        </div>
                    </div>

                    <div class="list-group-item">
                        <g:if test="${properties}">
                            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                    service:serviceName,
                                    provider:type,
                                    messagePrefix:"setup.",
                                    properties:properties,
                                    report:report,
                                    values:config,
                                    fieldnamePrefix:'config.',
                                    origfieldnamePrefix:'orig.' ,
                                    allowedScope: PropertyScope.Project
                            ]}"/>
                        </g:if>

                    </div>
                </div>

                <div class="panel-footer">
                    <g:submitButton name="create" value="${g.message(code: 'button.Setup.title', default: 'Setup')}"
                                    class="btn btn-default"/>
                </div>
            </div>
        </g:form>
    </div>
</div>

<g:render template="/framework/storageBrowseModalKO"/>
</body>
</html>