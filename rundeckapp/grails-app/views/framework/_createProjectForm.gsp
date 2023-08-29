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

<%@ page import="org.rundeck.core.auth.AuthConstants; com.dtolabs.rundeck.plugins.ServiceNameConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="skipPrototypeJs" content="true"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="domain.Project.choose.title" default="Create a Project"/></title>
    <g:set var="nodeExecutorPluginsData" value="${nodeExecDescriptions.collect {
        [
                type       : it.name,
                iconSrc    : stepplugin.pluginIconSrc(service: 'NodeExecutor', name: it.name),
                providerMeta    : stepplugin.pluginProviderMeta(service: 'NodeExecutor', name: it.name),
                title      :
                        stepplugin.messageText(
                                service: 'NodeExecutor',
                                name: it.name,
                                code: 'plugin.title',
                                default: it.title
                        ),
                description:
                        stepplugin.messageText(
                                service: 'NodeExecutor',
                                name: it.name,
                                code: 'plugin.description',
                                default: it.description
                        )
        ]
    }}"/>
    <g:set var="fileCopierPluginsData" value="${fileCopyDescriptions.collect {
        [
                type       : it.name,
                iconSrc    : stepplugin.pluginIconSrc(service: 'FileCopier', name: it.name),
                providerMeta    : stepplugin.pluginProviderMeta(service: 'FileCopier', name: it.name),
                title      :
                        stepplugin.messageText(
                                service: 'FileCopier',
                                name: it.name,
                                code: 'plugin.title',
                                default: it.title
                        ),
                description:
                        stepplugin.messageText(
                                service: 'FileCopier',
                                name: it.name,
                                code: 'plugin.description',
                                default: it.description
                        )
        ]
    }}" />
    <g:embedJSON id="projectDataJSON" data="${[
            create: true,
            name:params.newproject,
            defaultFileCopier:defaultFileCopy,
            defaultNodeExec:defaultNodeExec,
            descriptions: [
                    NodeExecutor: nodeExecutorPluginsData,
                    FileCopier: fileCopierPluginsData,
            ]
    ]}"/>
    <g:embedJSON id="pluginGroupJSON" data="${[
            project: params.newproject,
            config: pluginGroupConfig
    ]}"/>

    <asset:javascript src="framework/editProject.js"/>
    <g:loadEntryAssets entry="pages/project-config" />
    <g:javascript>

    function init(){
        jQuery('input[type=text]').on('keydown', noenter);
    }
    jQuery(init);
    window._rundeck = Object.assign(window._rundeck || {}, {
        data: {
            pluginGroups: loadJsonData("pluginGroupJSON")
        }
    })
    </g:javascript>
    <style type="text/css">
    #configs li {
        margin-top: 5px;
    }

    </style>
</head>

<body>
<g:set var="adminauth"
       value="${auth.resourceAllowedTest(type: AuthConstants.TYPE_RESOURCE, kind: AuthConstants.TYPE_PROJECT, action: [AuthConstants.ACTION_CREATE], context: AuthConstants.CTX_APPLICATION)}"/>
<g:if test="${adminauth}">
<div class="content">
<div id="layoutBody">
  <div class="container-fluid">
    <div class="row">
      <div class="col-xs-12">
        <div class="card" id="createform">
          <g:form action="createProject" useToken="true" method="post" >
            <div class="card-header" data-ko-bind="editProject">
              <h4 class="card-title"><g:message code="domain.Project.create.message" default="Create a new Project"/></h4>
            </div>
            <div class="card-content">

                <g:set var="serviceDefaultsList" value="${[
                        [
                                service     : ServiceNameConstants.NodeExecutor,
                                descriptions: nodeExecDescriptions,
                                prefix      : 'nodeexec',
                                errreport   : nodeexecreport,
                                selectedType: defaultNodeExec,
                                config      : nodeexecconfig

                        ],
                        [
                                service     : ServiceNameConstants.FileCopier,
                                descriptions: fileCopyDescriptions,
                                prefix      : 'fcopy',
                                errreport   : fcopyreport,
                                selectedType: defaultFileCopy,
                                config      : fcopyconfig

                        ]
                ]}"/>
              <tmpl:editProjectFormTabs serviceDefaultsList="${serviceDefaultsList}"/>
            </div>
            <div class="card-footer">
              <g:submitButton name="cancel" value="${g.message(code: 'button.action.Cancel', default: 'Cancel')}" class="btn btn-default"/>
              <g:submitButton name="create" value="${g.message(code: 'button.action.Create', default: 'Create')}" class="btn btn-cta"/>
            </div>
          </div>
        </g:form>
      </div>
    </div>
    <g:render template="storageBrowseModalKO"/>
  </div>
</g:if>
<g:else>
    <div class="pageBody">
        <div class="error note"><g:message code="unauthorized.project.create"/></div>
    </div>
</g:else>
</div>
</div>
</body>
</html>
