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
   chooseProject.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Dec 29, 2010 6:28:51 PM
--%>

<%@ page import="com.dtolabs.rundeck.plugins.ServiceNameConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="skipPrototypeJs" content="true"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code:'configuration')}"/>
    <meta name="projconfigselected" content="edit-project"/>
    <title><g:message code="edit.configuration" /></title>
    <g:set var="projectName" value="${params.project ?: request.project}"/>
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
            project          : projectName,
            defaultFileCopier: defaultFileCopy,
            defaultNodeExec  : defaultNodeExec,
            descriptions: [
                    NodeExecutor: nodeExecutorPluginsData,
                    FileCopier: fileCopierPluginsData,
            ]
    ]}"/>
    <g:embedJSON id="pluginGroupJSON" data="${[
            project          : projectName,
            config: pluginGroupConfig
    ]}"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="framework/editProject.js"/>
    <asset:javascript src="static/pages/project-config.js" defer="defer" />
    <asset:stylesheet href="static/css/pages/project-config.css" />
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

    var confirm = new PageConfirm(message('page.unsaved.changes'),{
        skipbehavior:true,
        setNeedsConfirm(){
            window._rundeck.eventBus.emit('page-modified','Edit Page')
        },
        clearNeedConfirm(){
            window._rundeck.eventBus.emit('page-reset','*')
        }
    });
    function init(){
        jQuery('input[type=text]').on('keydown', noenter);
    }
    var _storageBrowseSelected=confirm.setNeedsConfirm;
    jQuery(init);
    window._rundeck = Object.assign(window._rundeck || {}, {
        data: {
            pluginGroups: loadJsonData("pluginGroupJSON")
        }
    })
    </g:javascript>
</head>

<body>
<div class="content">
<div id="layoutBody">
  <div class="title">
    <span class="text-h3"><i class="fas fa-cog"></i> ${g.message(code:"edit.configuration")}</span>
  </div>
<div class="container-fluid">
  <div class="row">
      <div class="col-sm-12">
          <g:render template="/common/messages"/>
      </div>
  </div>
  <div class="row">
    <g:form action="saveProject" method="post" useToken="true" class="form">
    <div class="col-xs-12">
      <div class="card"  id="createform">
          <div class="card-header" data-ko-bind="editProject">
          <h3 class="card-title">
              <g:message code="domain.Project.edit.message" default="Configure Project"/>: <g:enc>${projectName}</g:enc>
            <g:link controller="framework" action="editProjectConfig"
                    params="[project: projectName]"
                    class="has_tooltip pull-right btn btn-default btn-sm"
                    data-placement="bottom"
                    title="${message(
                      code: 'page.admin.EditProjectConfigFile.title',
                      default: 'Advanced: Edit config file directly'
              )}">

                <g:message code="page.admin.EditProjectConfigFile.button" default="Edit Configuration File"/>
            </g:link>
          </h3>
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
            <g:render template="editProjectFormTabs"
                      model="${[editOnly: true, project: projectName, serviceDefaultsList: serviceDefaultsList]}"/>
        </div>
        <div class="card-footer">
          <g:submitButton name="cancel" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default reset_page_confirm"/>
          <g:submitButton name="save" value="${g.message(code:'button.action.Save',default:'Save')}" class="btn btn-cta reset_page_confirm"/>

            <div class="project-config-plugins-vue">
                <page-confirm :event-bus="EventBus"
                              class="text-warning"
                              message="${enc(attr: message(code: 'page.unsaved.changes'))}"
                              :display="true">
                </page-confirm>
            </div>
        </div>
      </div>
    </div>
    </g:form>
  </div>
  <g:render template="storageBrowseModalKO"/>
</div>
</div>
</div>
</body>
</html>
