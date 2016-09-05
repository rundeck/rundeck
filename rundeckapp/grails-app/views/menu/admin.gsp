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
        <li class="${!authExport ? 'disabled' : ''}">
            <a href="#export"
               data-toggle="${authExport ? 'tab' : ''}"
               title="${authExport ? '' : message(code:"request.error.unauthorized.title")}"
                ><g:message code="export.archive" /></a>
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

             <g:if test="${extraConfig}">
                 <div class="list-group-item">
                     <h4 class="list-group-item-heading ">
                         <g:message code="resource.model" />
                     </h4>

                     <span class="text-muted">
                         <g:message code="additional.configuration.for.the.resource.model.for.this.project" />
                     </span>

                     <div class="inpageconfig">
                         <g:each in="${extraConfig.keySet()}" var="configService">
                             <g:set var="configurable" value="${extraConfig[configService].configurable}"/>
                             <g:if test="${configurable.category == 'resourceModelSource'}">

                                 <g:set var="pluginprefix" value="${extraConfig[configService].get('prefix')}"/>

                                 <g:each in="${configurable.projectConfigProperties}" var="prop">
                                     <g:render template="/framework/pluginConfigPropertySummaryValue"
                                               model="${[
                                                       prop:prop,
                                                       prefix:pluginprefix,
                                                         values:extraConfig[configService].get('values')?:[:],
                                               ]}"/>
                                 </g:each>
                                 
                             </g:if>
                         </g:each>
                     </div>
                 </div>
             </g:if>

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

<g:if test="${authExport}">
<div class="tab-pane" id="export">
    <div class="panel panel-default panel-tab-content">
        <div class="panel-heading">
            <g:message code="download.an.archive.of.project.named.prompt" args="${[params.project ?: request.project]}"/>
        </div>
        <div class="panel-body">
                <g:link controller="project" action="exportPrepare" params="[project: params.project ?: request.project]"
                    class="btn btn-success"
                >
                    <g:message code="export.project.jar.button" args="${[params.project ?: request.project]}" />
                </g:link>

        </div>

    </div>


</div>
</g:if>
<g:if test="${authDelete}">
<div class="tab-pane" id="delete">
    <div class="panel panel-default panel-tab-content">
        <div class="panel-heading">
            <g:message code="delete.project" />
        </div>
        <div class="panel-body">


                <a class="btn btn-danger btn-lg" data-toggle="modal" href="#deleteProjectModal">
                    <g:message code="delete.this.project.button" />
                    <i class="glyphicon glyphicon-remove"></i>
                </a>
                <g:form style="display: inline;" controller="project" action="delete" params="[project: (params.project ?: request.project)]"
                        useToken="true"
                >
                <div class="modal fade" id="deleteProjectModal" role="dialog" aria-labelledby="deleteProjectModalLabel"
                     aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal"
                                        aria-hidden="true">&times;</button>
                                <h4 class="modal-title" id="deleteProjectModalLabel"><g:message code="delete.project" /></h4>
                            </div>

                            <div class="modal-body">
                                <span class="text-danger"><g:message code="really.delete.this.project" /></span>
                            </div>
                            <div class="modal-body container">
                                <div class="form-group">
                                    <label class="control-label col-sm-2"><g:message code="project.prompt" /></label>

                                    <div class="col-sm-10">
                                        <span class="form-control-static"
                                              data-bind="text: filterName"><g:enc>${params.project ?: request.project}</g:enc></span>
                                    </div>
                                </div>
                            </div>


                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal"><g:message code="no" /></button>
                                <button type="submit" class="btn btn-danger"><g:message code="delete.project.now.button" /></button>
                            </div>
                        </div><!-- /.modal-content -->
                    </div><!-- /.modal-dialog -->
                </div>


                </g:form>

        </div>

    </div>


</div>
</g:if>

<g:if test="${authImport}">
<div class="tab-pane" id="import">
    <g:form controller="project" action="importArchive" params="[project:params.project ?: request.project]"
            useToken="true"
            enctype="multipart/form-data" class="form">
    <div class="list-group list-group-tab-content">
        <div class="list-group-item">
            <div class="form-group">
                <label>
                    <g:message code="choose.a.rundeck.archive" />
                    <input type="file" name="zipFile" class="form-control"/>
                </label>

                <p class="help-block">
                    <g:message code="archive.import.help" />
                </p>
            </div>
        </div>

        <div class="list-group-item">
            <h4 class="list-group-item-heading"><g:message code="imported.jobs" /></h4>

            <div class="radio">
                    <label title="Original UUIDs will be preserved, conflicting UUIDs will be replaced">
                        <input type="radio" name="jobUuidOption" value="preserve" checked />
                        <g:message code="project.archive.import.jobUuidOption.preserve.label"/>
                    </label>

                <p class="help-block"><g:message
                        code="project.archive.import.jobUuidOption.preserve.description"/></p>
            </div>
            <div class="radio">
                <label title="New UUIDs will be generated for every imported Job">
                    <input type="radio" name="jobUuidOption" value="remove"/>
                    <g:message code="project.archive.import.jobUuidOption.remove.label"/>
                </label>

                <p class="help-block"><g:message
                        code="project.archive.import.jobUuidOption.remove.description"/></p>
            </div>
        </div>

        <div class="list-group-item">
            <h4 class="list-group-item-heading"><g:message code="Execution.plural" /></h4>

            <div class="radio">
                <label title="All executions and reports will be imported">
                    <input type="radio" name="importExecutions" value="true" checked/>
                    <g:message code="archive.import.importExecutions.true.title" />
                </label>
                <span class="help-block"><g:message code="archive.import.importExecutions.true.help" /></span>
            </div>

            <div class="radio">
                <label title="No executions or reports will be imported">
                    <input type="radio" name="importExecutions" value="false"/>
                    <g:message code="archive.import.importExecutions.false.title" />
                </label>
                <span class="help-block"><g:message code="archive.import.importExecutions.false.help" /></span>
            </div>
        </div>
        <div class="list-group-item">
            <h4 class="list-group-item-heading">Configuration</h4>

            <div class="radio">
                <label title="">
                    <input type="radio" name="importConfig" value="true" checked/>
                    <g:message code="archive.import.importConfig.true.title" />
                </label>
                <span class="help-block">
                    <g:message code="archive.import.importConfig.true.help" />
                </span>
            </div>

            <div class="radio">
                <label title="">
                    <input type="radio" name="importConfig" value="false"/>

                    <g:message code="archive.import.importExecutions.false.title" />

                </label>
                <span class="help-block">
                    <g:message code="archive.import.importConfig.false.help" />
                </span>
            </div>
        </div>
        <auth:resourceAllowed action="${[AuthConstants.ACTION_CREATE,AuthConstants.ACTION_ADMIN]}"
                              any="true"
                              context='application'
                              type="project_acl"
                              name="${params.project}">
            <div class="list-group-item">
                <h4 class="list-group-item-heading">ACL Policies</h4>

                <div class="radio">
                    <label title="">
                        <input type="radio" name="importACL" value="true" checked/>
                        <g:message code="archive.import.importACL.true.title" />
                    </label>
                    <span class="help-block">
                        <g:message code="archive.import.importACL.true.help" />
                    </span>
                </div>

                <div class="radio">
                    <label title="">
                        <input type="radio" name="importACL" value="false"/>

                        <g:message code="archive.import.importExecutions.false.title" />

                    </label>
                    <span class="help-block">
                        <g:message code="archive.import.importACL.false.help" />
                    </span>
                </div>
            </div>
        </auth:resourceAllowed>
        <auth:resourceAllowed action="${[AuthConstants.ACTION_CREATE,AuthConstants.ACTION_ADMIN]}"
                              any="true"
                              context='application'
                              type="project_acl"
                                has="false"
                              name="${params.project}">
            <div class="list-group-item">
                <h4 class="list-group-item-heading">ACL Policies</h4>


                <span class="help-block">
                    <i class="glyphicon glyphicon-ban-circle"></i>
                    <g:message code="archive.import.importACL.unauthorized.help" />
                </span>
            </div>
        </auth:resourceAllowed>
        <div class="list-group-item">
            <div class="buttons">
                <div id="uploadFormButtons">
                    <g:actionSubmit id="createFormCancelButton" value="Cancel" class="btn btn-default"/>
                    <g:actionSubmit action="importArchive" value="Import" id="uploadFormUpload"
                                    onclick="['uploadFormButtons','importUploadSpinner'].each(Element.toggle)"
                                    class="btn btn-primary"/>
                </div>

                <div id="importUploadSpinner" class="spinner block" style="display:none;">
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                         alt="Spinner"/>
                    <g:message code="uploading.file" />
                </div>
            </div>
        </div>
        </div>
    </div>
    </g:form>
</div>
</g:if>
</div>
</div>
</div>
</div>
</body>
</html>
