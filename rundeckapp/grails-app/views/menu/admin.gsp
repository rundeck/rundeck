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
   admin.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 6/1/11 2:22 PM
--%>

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="gui.menu.Admin" /></title>

</head>

<body>

<div class="row">
<div class="col-sm-12">
    <g:render template="/common/messages"/>
    <g:if test="${flash.joberrors}">
        <div class="alert alert-danger alert-dismissable">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
            <ul>
                <g:each in="${flash.joberrors}" var="errmsg">
                    <li><g:enc>${errmsg}</g:enc></li>
                </g:each>
            </ul>
        </div>
    </g:if>
    <g:if test="${flash.execerrors}">
        <div class="alert alert-warning alert-dismissable">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
            <g:message code="some.files.in.the.archive.could.not.be.imported" />
            <ul>
                <g:each in="${flash.execerrors}" var="errmsg">
                    <li><g:enc>${errmsg}</g:enc></li>
                </g:each>
            </ul>
        </div>
    </g:if>
    <g:if test="${flash.aclerrors}">
        <div class="alert alert-warning alert-dismissable">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
            <g:message code="some.files.in.the.archive.could.not.be.imported" />
            <ul>
                <g:each in="${flash.aclerrors}" var="errmsg">
                    <li><g:enc>${errmsg}</g:enc></li>
                </g:each>
            </ul>
        </div>
    </g:if>
    </div>
</div>
<div class="row">
<div class="col-sm-3">
    <g:render template="configNav" model="[selected:'project']"/>
</div>
<div class="col-sm-9">

    <span class="h3">
        <g:message code="project.named.prompt" args="${[params.project ?: request.project]}"/>
    </span>
<g:set var="authAdmin" value="${auth.resourceAllowedTest(action: AuthConstants.ACTION_ADMIN, type: "project",
        name: (params.project ?: request.project), context: "application")}"/>
<g:set var="authDelete"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_DELETE, type: "project",
               name: (params.project ?: request.project), context: "application")}"/>
<g:set var="authExport"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_EXPORT, type: "project",
               name: (params.project ?: request.project), context: "application")}"/>
<g:set var="authImport"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_IMPORT, type: "project",
               name: (params.project ?: request.project), context: "application")}"/>
<g:set var="authConfigure"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_CONFIGURE, type: "project",
               name: (params.project ?: request.project), context: "application")}"/>
<div class="row row-space">
<div class="col-sm-12">
    <ul class="nav nav-tabs">
        <li class="${authConfigure ? 'active' : 'disabled'}">
            <a href="#configure"
               data-toggle="${authConfigure ? 'tab' : ''}"
               title="${authConfigure ? '' : message(code:"request.error.unauthorized.title")}"
                ><g:message code="configuration" /></a>
        </li>
        <li class="${!authImport ? 'disabled' : ''}">
            <a href="#import"
               data-toggle="${authImport ? 'tab' : ''}"
               title="${authImport ? '' : message(code:"request.error.unauthorized.title")}"
                ><g:message code="import.archive" /></a>
        </li>
        <li class="${!authDelete?'disabled':''}">
            <a href="#delete" data-toggle="${authDelete?'tab':''}"
               title="${authDelete?'':message(code:"request.error.unauthorized.title")}">
                <g:message code="delete.project" />
            </a>
        </li>
    </ul>

<div class="tab-content">
<div class="tab-pane active" id="configure">
<ul class="list-group list-group-tab-content">
         <g:if test="${authConfigure}">
         <li class="list-group-item">
            <g:link controller="framework" action="editProject" params="[project: params.project ?: request.project]"
                    class="btn btn-success   ">
                <i class="glyphicon glyphicon-edit"></i>
                <g:message code="page.admin.EditProjectSimple.button" default="Simple Configuration"/>
            </g:link>
            <g:link controller="framework" action="editProjectConfig" params="[project: params.project ?: request.project]"
                    class="btn btn-info has_tooltip"
                    data-placement="right"
                    title="${message(code:'page.admin.EditProjectConfigFile.title',default:'Advanced: Edit config file directly')}"
                       >
                <i class="glyphicon glyphicon-edit"></i>
                <g:message code="page.admin.EditProjectConfigFile.button" default="Edit Configuration File"/>
            </g:link>
         </li>
         <li class="list-group-item">
            <g:link controller="framework"
                    action="editProjectFile"
                    params="[project: params.project ?: request.project, filename: 'readme.md']"
                    class="btn btn-${hasreadme?'info':'link btn-success'} btn-sm">
                <i class="glyphicon glyphicon-edit"></i>
                ${hasreadme?'Edit':'Add'} Project Readme
            </g:link>
            <g:link
                    controller="framework"
                    action="editProjectFile"
                    params="[project: params.project ?: request.project,filename:'motd.md']"
                    class="btn btn-${hasmotd?'info':'link btn-success'} btn-sm">
                <i class="glyphicon glyphicon-edit"></i>
                ${hasmotd?'Edit':'Add'} Message of the Day
            </g:link>
         </li>

        <li class="list-group-item">
            <h4 class="list-group-item-heading">
                <g:message code="framework.service.ResourceModelSource.label"/>
            </h4>
            <g:if test="${!configs}">
                <div class="text-muted"><g:message code="message.none.set"/></div>
            </g:if>

            <ol id="configs">
                <g:if test="${configs}">
                    <g:each var="config" in="${configs}" status="n">
                        <li>
                            <div class="inpageconfig">
                                <g:set var="desc"
                                       value="${resourceModelConfigDescriptions.find {it.name==config.type}}"/>
                                <g:if test="${desc}">

                                    <g:render template="/framework/viewResourceModelConfig"
                                              model="${[values: config.props, description: desc]}"/>
                                </g:if>
                                <g:else>
                                    <span
                                        class="text-warning"><g:message code="invalid.resource.model.source.configuration.provider.not.found"  args="${[config.type]}"/></span>
                                </g:else>
                                <g:if test="${nodeErrorsMap && nodeErrorsMap[(n+1)+'.source']}">
                                    <div class="row row-space">
                                    <div class="col-sm-12">
                                        <g:set var="arkey" value="${g.rkey()}"/>
                                        <div class=" well well-embed text-danger " id="srcerr_${arkey}">
                                            <g:icon name="warning-sign"/>
                                            Error:
                                            ${nodeErrorsMap[(n+1)+'.source'].message}
                                        </div>
                                    </div>
                                    </div>
                                </g:if>
                            </div>
                        </li>
                    </g:each>
                </g:if>
            </ol>
        </li>



             <g:set var="categories" value="${new HashSet(
                     extraConfig?.values()?.collect { it.configurable.categories?.values() }.flatten()
             )}"/>


             <g:each in="${categories.sort()}" var="category">

                     <g:render template="projectConfigurableView"
                               model="${[extraConfigSet: extraConfig?.values(),
                                         category      : category,
                                         titleCode     : 'project.configuration.extra.category.' + category + '.title',
                                         helpCode      : 'project.configuration.extra.category.' + category + '.description'
                               ]}"/>

             </g:each>
        <li class="list-group-item">
            <h4 class="list-group-item-heading">
                <g:message code="framework.service.NodeExecutor.default.label" />
            </h4>
            <span class="text-muted"><g:message code="domain.Project.edit.NodeExecutor.explanation"/></span>
            <g:if test="${!nodeexecconfig}">
                <div class="text-warning"><g:message code="message.none.set"/></div>
            </g:if>

            <g:if test="${nodeexecconfig}">
                <div class="inpageconfig">
                    <g:set var="desc" value="${nodeExecDescriptions.find {it.name==nodeexecconfig.type}}"/>
                    <g:if test="${desc}">

                        <g:render template="/framework/renderPluginConfig"
                                  model="${[serviceName:'NodeExecutor',values: nodeexecconfig.config, description: desc]}"/>
                    </g:if>
                    <g:else>
                        <span
                            class="warn note"><g:message code="framework.service.error.missing-provider"
                                                         args="[nodeexecconfig.type]"/></span>
                    </g:else>
                </div>
            </g:if>
        </li>

        <li class="list-group-item">
            <h4 class="list-group-item-heading">
                <g:message code="framework.service.FileCopier.default.label" />
            </h4>
            <span
                class="text-muted"><g:message code="domain.Project.edit.FileCopier.explanation"/></span>
            <g:if test="${!fcopyconfig}">
                <div class="text-warning"><g:message code="message.none.set"/></div>
            </g:if>

            <g:if test="${fcopyconfig}">
                <div class="inpageconfig">
                    <g:set var="desc" value="${fileCopyDescriptions.find {it.name==fcopyconfig.type}}"/>
                    <g:if test="${desc}">

                        <g:render template="/framework/renderPluginConfig"
                                  model="${[serviceName:'FileCopier',values: fcopyconfig.config, description: desc]}"/>
                    </g:if>
                    <g:else>
                        <span
                            class="warn note"><g:message code="framework.service.error.missing-provider"
                                                         args="[fcopyconfig.type]"/></span>
                    </g:else>
                </div>
            </g:if>
        </li>

         </g:if>
        <g:else>
            <li class="list-group-item">
                <p class="text-muted">
                    <g:message code="configuration.not.available" />
                </p>
            </li>
        </g:else>
    </ul>

</div>


</div>
</div>
</div>
</div>
</body>
</html>
