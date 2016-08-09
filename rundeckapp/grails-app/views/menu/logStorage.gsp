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
  Date: 3/3/16
  Time: 2:34 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="menu.logStorage.page.title"/></title>
    <asset:javascript src="menu/logStorage.js"/>
    <g:javascript>

        var storagestats=StorageStats.init({
            baseUrl:"${g.createLink(action: 'logStorageAjax')}",
            requestsUrl:"${g.createLink(action: 'logStorageIncompleteAjax')}",
            missingUrl:"${g.createLink(action: 'logStorageMissingAjax')}",
            resumeUrl:"${g.createLink(action: 'resumeIncompleteLogStorageAjax', params: [project: params.project])}",
            cleanupUrl:"${g.createLink(action: 'cleanupIncompleteLogStorageAjax', params: [project: params.project])}",
            tokensName:'page_tokens'
        });
    </g:javascript>
</head>

<body>
<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>

<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'logstorage']"/>
    </div>

    <div class="col-sm-9">

        <h3><g:message code="menu.logStorage.page.title"/>
            <span data-bind="if: !loaded() || loading()">
                <asset:image src="spinner-blue.gif" width="20px" height="20px"/>
            </span>
        </h3>

        <div data-bind="if: enabled() && loaded()">

            <table class="table table-bordered table-condensed">

                <tr>
                    <th colspan="5" class="text-muted table-footer text-small">
                        <g:message code="menu.logStorage.table.title"/>
                    </th>
                </tr>
                <tr>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.progress.title"/>
                    </th>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.queueCount.title"/>
                    </th>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.storageResults.title"/>
                    </th>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.incomplete.title"/>
                    </th>
                </tr>
                <tr>
                    <td class="text-center" data-bind="click: toggleProgressView">
                        <span data-bind="visible: progressView()==0">
                            <g:render template="/common/progressBar"
                                      model="[completePercent : 0,
                                              progressClass   : 'progress-embed',
                                              progressBarClass: 'progress-bar-success',
                                              containerId     : 'progressContainer2',
                                              innerContent    : '',
                                              showpercent     : true,
                                              progressId      : 'progressBar',
                                              bind            : 'percent()',
                                              bindText        : '',
                                      ]"/>
                        </span>
                        <span data-bind="visible: progressView()==1">
                            <span class="h3" data-bind="text: percentText"></span>
                        </span>
                        <span data-bind="visible: progressView()==2">
                            <span class="h3" data-bind="text: succeededCount()+'/'+totalCount()"></span>
                        </span>

                    </td>
                    <g:each in="['queuedCount']" var="propname">
                        <td class="h3 text-center"
                            data-bind="text: ${propname}, css: { 'text-info': ${propname}()>0 , 'text-muted': ${propname}()<1 } "></td>
                    </g:each>
                    <td class="text-center h3">
                        <span class="text-success" data-bind="text: succeededCount"></span>
                        <span class="text-muted">/</span>
                        <span data-bind="text: failedCount, css: {'text-warning': failedCount()>0, 'text-muted': failedCount()<1 }"></span>

                    </td>
                    <g:each in="['incompleteCount']" var="propname">
                        <td class="h3 text-center"
                            data-bind="text: ${propname}, css: { 'text-warning': ${propname}()>0 , 'text-muted': ${propname}()<1 } "></td>
                    </g:each>
                </tr>
                <tr>
                %{--descriptions--}%
                    <g:each in="['progress', 'queueCount', 'storageResults', 'incomplete']" var="name">

                        <td class="text-muted text-small">
                            <g:message code="menu.logStorage.stats.${name}.description"/>
                            <g:if test="${name == 'queueCount'}">
                                <span data-bind="messageTemplate: retryDelay">
                                    <g:message code="menu.logStorage.stats.queueCount.description.extended"/>
                                </span>
                            </g:if>
                        </td>
                    </g:each>
                </tr>
                <tr>
                    <td></td>
                    <td>

                        <div data-bind="if: queuedCount()>0">

                            <g:form useToken="true" action="haltIncompleteLogStorage" controller="menu"
                                    params="[project: params.project]">
                                <div class="btn-group">
                                    <button class="btn btn-warning btn-sm" title="${message(code:"menu.logStorage.button.clear.queue.title")}">
                                        <g:message code="menu.logStorage.button.clear.queue" />
                                        <g:icon name="remove-sign"/>
                                    </button>
                                </div>
                            </g:form>
                        </div>
                    </td>
                    <td></td>
                    <td>
                        <div data-bind="if: incompleteCount()>0">

                            <div class="btn-group">
                                <button class="btn btn-default btn-sm"
                                        data-bind="click: resumeAllIncomplete">
                                    <g:message code="menu.logStorage.button.resume.incomplete.log.storage.uploads"/>
                                    <g:icon name="play"/>
                                </button>
                                <button class="btn btn-warning btn-sm"
                                        data-bind="click: cleanupAllIncomplete"
                                        title="${message(code:"menu.logStorage.button.remove.all.title")}">
                                    <g:message code="menu.logStorage.button.remove.all" />
                                    <g:icon name="remove"/>
                                </button>
                            </div>

                        </div>
                    </td>
                </tr>
            </table>


            <div class="btn-group">
                <button class="btn btn-info  btn-sm"
                        data-bind="click: loadIncomplete, attr: { disabled: incompleteCount()<1 && queuedCount()<1 }">
                    <g:message code="menu.logStorage.button.list.incomplete.log.data.storage" />
                </button>

            </div>


            <div data-bind="if: incompleteRequests().total()>0">
                <table class="table table-bordered table-condensed">
                    <tr>
                        <th colspan="4" class="text-muted table-footer text-small">
                            <g:message code="menu.logStorage.table.title.incomplete.log.data" />
                        </th>
                    </tr>
                    <tr>
                        <th colspan="4" class="text-muted table-footer text-small">

                            <div class="btn-group">
                                <button class="btn btn-default btn-xs"
                                        data-bind="click: incompletePageBackward, attr: {disabled: !hasIncompletePageBackward()}">
                                    <g:message code="prev.page" />
                                </button>
                                <button class="btn btn-default btn-xs"
                                        data-bind="click: incompletePageForward, attr: {disabled: !hasIncompletePageForward()}">
                                    <g:message code="next.page" />
                                </button>
                            </div>
                            <span class="text-muted">
                                <span data-bind="messageTemplate: [incompleteRequests().offsetInt()+1, incompleteRequests().offsetInt()+incompleteRequests().maxInt(),incompleteRequests().total] ">
                                    <g:message code="menu.logStorage.table.paging.info" />
                                </span>
                            </span>
                        </th>
                    </tr>
                    <tr>
                        <th class="text-muted text-small" colspan="2">
                            <g:message code="menu.logStorage.table.incomplete.header.execution.id" />
                        </th>
                        <th class="text-muted text-small">
                            <g:message code="menu.logStorage.table.incomplete.header.date.created" />
                        </th>
                        <th class="text-muted text-small">
                            <g:message code="menu.logStorage.table.incomplete.header.Action" />
                        </th>
                    </tr>
                    <tbody data-bind="foreach: incompleteRequests().contents()">
                    <tr>
                        <td width="24px">
                            <span data-bind="if: queued" class="text-info" title="${message(code:"menu.logStorage.incomplete.status.queued.title")}">
                                %{--<asset:image src="spinner-gray.gif" width="16px" height="16px" title="Queued"/>--}%
                                <g:icon name="hourglass"/>
                            </span>
                            <span data-bind="if: failed" title="${message(code:"menu.logStorage.incomplete.status.failed.title")}" class="text-warning">
                                <g:icon name="exclamation-sign"/>
                            </span>
                            <span data-bind="if: !failed() && !queued()" title="${message(code:"menu.logStorage.incomplete.status.unqueued.title")}" class="text-muted">
                                <g:icon name="hourglass"/>
                            </span>
                        </td>
                        <td>

                            <a href="#" data-bind="attr: { href: permalink }  ">
                                #<span data-bind="text: executionId"></span>
                            </a>
                            <span data-bind="if: localFilesPresent, bootstrapTooltip:true"
                                  class="text-success"
                                  data-placement="right"
                                  title="${message(code:"menu.logStorage.localFilesPresent.true.title")}">
                                <g:icon name="file"/>
                            </span>
                            <span data-bind="if: !localFilesPresent(), bootstrapTooltip:true"
                                  class="text-warning"
                                  data-placement="right"
                                  title="${message(code:"menu.logStorage.localFilesPresent.false.title")}">
                                <g:icon name="alert"/>
                            </span>
                        </td>
                        <td data-bind="text: dateCreated"></td>
                        <td width="25%">
                            <span data-bind="if: !queued()">
                                <div class="btn-group">

                                    <button class="btn btn-default btn-xs"
                                            data-bind="click: $root.resumeSingleIncomplete">
                                        <g:message code="menu.logStorage.button.requeue" />
                                        <g:icon name="play"/>
                                    </button>

                                    <button class="btn btn-warning btn-xs"
                                            data-bind="click: $root.cleanupSingleIncomplete">
                                        <g:message code="menu.logStorage.button.remove" />
                                        <g:icon name="remove"/>
                                    </button>
                                </div>
                            </span>
                        </td>

                    </tr>
                    <tr data-bind="if: messages()">
                        <td colspan="4">
                            <div data-bind="foreach: messages()" class="text-warning text-small">
                                <span data-bind="text: $data"></span>
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>

        </div>

        <div data-bind="if: !enabled() && loaded()">
            <g:message code="menu.logStorage.not.enabled.message"/>
        </div>
    </div>
</div>
<g:jsonToken id="page_tokens" url="${request.forwardURI}"/>
</body>
</html>
