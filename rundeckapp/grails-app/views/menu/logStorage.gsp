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
        StorageStats.init("${g.createLink(action: 'logStorageAjax')}");
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

        <h3><g:message code="menu.logStorage.page.title" />
            <span data-bind="if: !loaded()">
                <asset:image src="spinner-blue.gif" width="24px" height="24px"/>
            </span>
        </h3>

        <div data-bind="if: enabled() && loaded()">

            <table class="table table-bordered table-condensed">

                <tr>
                    <th colspan="6" class="text-muted table-footer text-small">
                        <g:message code="menu.logStorage.table.title" />
                    </th>
                </tr>
                <tr>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.progress.title" />
                    </th>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.queueCount.title" />
                    </th>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.storageResults.title" />
                    </th>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.incomplete.title" />
                    </th>
                    <th style="width: 20%" class="text-muted text-center h5 text-header">
                        <g:message code="menu.logStorage.stats.missing.title" />
                    </th>
                </tr>
                <tr>
                    <td class="text-center" data-bind="click: toggleProgressView">
                        <span data-bind="visible: progressView()==0">
                            <g:render template="/common/progressBar"
                                      model="[completePercent: 0,
                                              progressClass: 'progress-embed',
                                              progressBarClass: 'progress-bar-success',
                                              containerId: 'progressContainer2',
                                              innerContent: '',
                                              showpercent: true,
                                              progressId: 'progressBar',
                                              bind: 'percent()',
                                              bindText: '',
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
                        <span  data-bind="text: failedCount, css: {'text-warning': failedCount()>0, 'text-muted': failedCount()<1 }"></span>

                    </td>
                    <g:each in="['incompleteCount']" var="propname">
                        <td class="h3 text-center"
                            data-bind="text: ${propname}, css: { 'text-warning': ${propname}()>0 , 'text-muted': ${propname}()<1 } "></td>
                    </g:each>
                    <td class="h3 text-center text-muted"
                        data-bind="text: missingCount"></td>
                </tr>
                <tr>
                    %{--descriptions--}%
                    <g:each in="['progress','queueCount','storageResults','incomplete','missing']" var="name">

                        <td class="text-muted text-small">
                            <g:message code="menu.logStorage.stats.${name}.description"/>
                        </td>
                    </g:each>
                </tr>
            </table>

            <div data-bind="if: incompleteCount()>0">

                <g:form useToken="true" action="resumeIncompleteLogStorage" controller="menu"
                        params="[project: params.project]">
                    <div class="btn-group">
                        <button class="btn btn-info">
                            <g:message code="menu.logStorage.button.resume.incomplete.log.storage.uploads" />
                            <g:icon name="upload"/>
                        </button>
                    </div>
                </g:form>

            </div>


        </div>

        <div data-bind="if: !enabled() && loaded()">
            <g:message code="menu.logStorage.not.enabled.message" />
        </div>
    </div>
</div>
</body>
</html>
