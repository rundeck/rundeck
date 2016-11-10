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

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" contentType="text/html;charset=UTF-8" %></page>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="configure"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="scmController.page.commit.title" args="[params.project]"/></title>

</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>

<div class="row">
    <div class="col-sm-12 col-md-10 col-md-offset-1 col-lg-8 col-lg-offset-2">
        <g:form action="performActionSubmit"
                params="${[project: params.project, integration: integration]}"
                useToken="true"
                method="post" class="form form-horizontal">
            <g:hiddenField name="allJobs" value="${params.allJobs}"/>
            <g:hiddenField name="actionId" value="${params.actionId}"/>
            <g:set var="serviceName" value="${integration=='export'?'ScmExport':'ScmImport'}"/>
            <div class="panel panel-primary" id="createform">
                <div class="panel-heading">
                    <span class="h3">

                        <stepplugin:message
                                service="${serviceName}"
                                name="${pluginDescription.name}"
                                code="plugin.title"
                                default="${pluginDescription.description?.title?:pluginDescription.name}"/>:
                        <g:if test="${actionView && actionView.title}">
                            <stepplugin:message
                                    service="${serviceName}"
                                    name="${pluginDescription.name}"
                                    code="action.${actionId}.title"
                                    default="${actionView.title}"/>
                        </g:if>
                        <g:else>
                            <g:message code="scmController.page.commit.description" default="SCM Export"/>
                        </g:else>
                    </span>
                </div>

                <div class="list-group">
                    <g:if test="${actionView && actionView.description}">
                        <div class="list-group-item">
                            <div class="list-group-item-text">
                                <g:markdown><stepplugin:message
                                        service="${serviceName}"
                                        name="${pluginDescription.name}"
                                        code="action.${actionId}.description"
                                        default="${actionView.description}"/></g:markdown>
                            </div>
                        </div>
                    </g:if>
                    <g:if test="${integration == 'export'}">

                        <g:if test="${jobs || deletedPaths}">

                            <div class="list-group-item">
                                <div class="row">
                                    <div class="col-sm-12">
                                        <div class="help-block">
                                            <g:message code="select.jobs.to.export"/>
                                        </div>
                                    </div>
                                </div>
                            </div>

                        </g:if>
                        <g:if test="${jobs}">

                            <div class="list-group-item">
                                <div class="form-group">
                                    <g:each in="${jobs}" var="job">

                                        <div class="checkbox col-sm-12">
                                            <label>
                                                <g:set var="jobstatus" value="${scmStatus?.get(job.extid)}"/>
                                                <g:if test="${jobstatus?.synchState?.toString() != 'CLEAN'}">

                                                    <g:checkBox name="id" value="${job.extid}"
                                                                checked="${selected?.contains(job.extid)}"/>
                                                </g:if>

                                                <g:render template="statusIcon"
                                                          model="[iscommit          : true,
                                                                  exportStatus: jobstatus?.synchState?.toString(),
                                                                  notext: true,
                                                                  integration:integration,
                                                                  text: '',
                                                                  commit: jobstatus?.commit]"/>
                                                <g:render template="statusIcon"
                                                          model="[iscommit          : true,
                                                                  exportStatus: jobstatus?.synchState?.toString(),
                                                                  noicon: true,
                                                                  integration:integration,
                                                                  text: job.jobName,
                                                                  commit: jobstatus?.commit]"/>

                                                <span class="text-muted">
                                                    - ${job.groupPath}
                                                </span>

                                            </label>
                                            <g:link action="diff" class="btn btn-xs btn-info"
                                                    params="${[project: params.project, id: job.extid, integration: 'export']}">
                                                <g:message code="button.View.Diff.title"/>
                                            </g:link>
                                        </div>
                                        <g:if test="${renamedJobPaths?.get(job.extid)}">
                                            <div class="col-sm-11 col-sm-offset-1">
                                                <span class="text-muted">
                                                    <g:icon name="file"/>
                                                    ${renamedJobPaths[job.extid]}

                                                    <g:hiddenField
                                                            name="renamedPaths.${job.extid}"
                                                            value="${renamedJobPaths[job.extid]}"/>
                                                </span>
                                            </div>
                                        </g:if>
                                        <g:if test="${filesMap?.get(job.extid)}">
                                            <div class="col-sm-11 col-sm-offset-1">
                                                <span class="text-muted">
                                                    <g:if test="${renamedJobPaths?.get(job.extid)}">
                                                        <g:icon name="arrow-right"/>
                                                    </g:if>
                                                    <g:icon name="file"/>
                                                    ${filesMap[job.extid]}

                                                </span>
                                            </div>
                                        </g:if>

                                    </g:each>
                                </div>
                                <g:if test="${jobs.size() > 1}">
                                    <div class=" row row-spacing">
                                        <div class=" col-sm-12">
                                            <span class="textbtn textbtn-default"
                                                  onclick="jQuery('input[name=id]').prop('checked', true)">
                                                <g:message code="select.all"/>
                                            </span>
                                        &bull;
                                            <span class="textbtn textbtn-default"
                                                  onclick="jQuery('input[name=id]').prop('checked', false)">
                                                <g:message code="select.none"/>
                                            </span>
                                        </div>
                                    </div>
                                </g:if>
                            </div>

                        </g:if>
                        <g:if test="${deletedPaths}">

                            <div class="list-group-item">
                                <div class="form-group">
                                    <g:each in="${deletedPaths.keySet().sort()}" var="path">

                                        <div class="checkbox col-sm-12">
                                            <label>

                                                <g:checkBox name="deletePaths" value="${path}"
                                                            checked="${selectedPaths?.contains(path)}"/>

                                                <g:set var="deletedJobText" value="${
                                                    deletedPaths[path].jobNameAndGroup ?:
                                                            message(code: "deleted.job.label")
                                                }"/>

                                                <g:render template="statusIcon"
                                                          model="[iscommit: true, exportStatus: 'DELETED', notext: true,
                                                                                                     text: '',]"/>
                                                <g:render template="statusIcon"
                                                          model="[iscommit: true, exportStatus: 'DELETED', noicon: true,
                                                                                                     text: deletedJobText]"/>

                                            </label>
                                        </div>

                                        <div class="col-sm-11 col-sm-offset-1">
                                            <span class="text-muted">
                                                <span class="glyphicon glyphicon-file"></span>
                                                ${path}
                                            </span>
                                        </div>

                                    </g:each>
                                </div>
                                <g:if test="${deletedPaths.size() > 1}">
                                    <div class=" row row-spacing">
                                        <div class=" col-sm-12">
                                            <span class="textbtn textbtn-default"
                                                  onclick="jQuery('input[name=deletePaths]').prop('checked', true)">
                                                <g:message code="select.all"/>
                                            </span>
                                        &bull;
                                            <span class="textbtn textbtn-default"
                                                  onclick="jQuery('input[name=deletePaths]').prop('checked', false)">
                                                <g:message code="select.none"/>
                                            </span>
                                        </div>
                                    </div>
                                </g:if>
                            </div>
                        </g:if>
                        <g:if test="${!jobs && !deletedPaths && scmProjectStatus.state.toString() == 'CLEAN'}">
                            <div class="list-group-item">
                                <g:message code="no.changes" />
                            </div>
                        </g:if>
                    </g:if>
                    <g:elseif test="${integration == 'import'}">
                        <g:if test="${trackingItems}">

                            <div class="list-group-item overflowy">
                                <div class="form-group">
                                    <g:each in="${trackingItems}" var="trackedItem">
                                        <g:set var="job" value="${trackedItem.jobId?jobMap[trackedItem.jobId]:null}"/>
                                        <div class="checkbox col-sm-12">
                                            <label title="${trackedItem.id}">
                                                <g:checkBox name="chosenTrackedItem"
                                                            value="${trackedItem.id}"
                                                            checked="${selectedItems?.contains(trackedItem.id)||trackedItem.selected||(trackedItem.jobId && selected?.contains(trackedItem.jobId))}"/>

                                                <g:if test="${job}">

                                                    <g:set var="jobstatus" value="${scmStatus?.get(job.extid)}"/>

                                                    <g:render template="statusIcon"
                                                              model="[iscommit          : true,
                                                                      importStatus: jobstatus?.synchState?.toString(),
                                                                      notext: true,
                                                                      integration:integration,
                                                                      text: '',
                                                                      commit: jobstatus?.commit]"/>
                                                    <g:render template="statusIcon"
                                                              model="[iscommit          : true,
                                                                      importStatus: jobstatus?.synchState?.toString(),
                                                                      noicon: true,
                                                                      integration:integration,
                                                                      text: job.jobName,
                                                                      commit: jobstatus?.commit]"/>

                                                    <span class="text-muted">
                                                        - ${job.groupPath}
                                                    </span>
                                                </g:if>
                                                <g:else>

                                                    <span class="">
                                                        <g:if test="${trackedItem.iconName}">
                                                            <g:icon name="${trackedItem.iconName}"/>
                                                        </g:if>
                                                        ${trackedItem.title ?: trackedItem.id}
                                                    </span>
                                                </g:else>

                                            </label>
                                            <g:if test="${job}">

                                                <g:link action="diff" class="btn btn-xs btn-info"
                                                        params="${[project: params.project, id: job.extid, integration: 'import']}">
                                                    <g:message code="button.View.Diff.title"/>
                                                </g:link>
                                            </g:if>
                                        </div>
                                        <g:if test="${job}">
                                            <div class="col-sm-11 col-sm-offset-1">
                                                <span class="text-muted">
                                                    <span class="">
                                                        <g:if test="${trackedItem.iconName}">
                                                            <g:icon name="${trackedItem.iconName}"/>
                                                        </g:if>
                                                        ${trackedItem.title ?: trackedItem.id}
                                                    </span>
                                                </span>
                                            </div>
                                        </g:if>

                                    </g:each>
                                </div>
                                <g:if test="${trackingItems.size() > 1}">
                                    <div class=" row row-spacing">
                                        <div class=" col-sm-12">
                                            <span class="textbtn textbtn-default"
                                                  onclick="jQuery('input[name=chosenTrackedItem]').prop('checked', true)">
                                                <g:message code="select.all"/>
                                            </span>
                                        &bull;
                                            <span class="textbtn textbtn-default"
                                                  onclick="jQuery('input[name=chosenTrackedItem]').prop('checked', false)">
                                                <g:message code="select.none"/>
                                            </span>
                                        </div>
                                    </div>
                                </g:if>
                            </div>
                        </g:if>
                    </g:elseif>
                    <div class="list-group-item">
                        <g:if test="${actionView?.properties}">
                            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                    service:serviceName,
                                    provider:pluginDescription.name,
                                    messagePrefix:"action.${actionId}.",
                                    properties:actionView?.properties,
                                    report:report,
                                    values:config,
                                    fieldnamePrefix:'pluginProperties.',
                                    origfieldnamePrefix:'orig.' ,
                                    allowedScope: PropertyScope.Project
                            ]}"/>
                        </g:if>
                    </div>
                </div>

                <div class="panel-footer">
                    <button class="btn btn-default" name="cancel" value="Cancel"><g:message
                            code="button.action.Cancel"/></button>
                    <g:submitButton
                            name="submit"
                            value="${stepplugin.messageText(service:serviceName,name:pluginDescription.name,code:'action.'+actionId+'.buttonTitle',default:actionView.buttonTitle ?:
                                    g.message(code: 'button.Export.title'))}"
                            class="btn btn-primary"/>
                </div>
            </div>
        </g:form>
    </div>
</div>
</body>
</html>