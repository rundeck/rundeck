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

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution; grails.util.Environment" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:appTitle/> - <g:enc>${scheduledExecution?.jobName}</g:enc></title>
    <g:javascript library="prototype/effects"/>
    <asset:javascript src="util/markdeep.js"/>
    <g:embedJSON id="jobParams"
                 data="${[filter: scheduledExecution?.filter, doNodeDispatch: scheduledExecution?.doNodedispatch, project: params.project
                         ?:
                         request.project]}"/>
    <g:embedJSON id="pageParams" data="${[project: params.project ?: request.project]}"/>

</head>

<body>
<div class="row">
    <div class="col-sm-6">
        <g:render template="/scheduledExecution/showHead"
                  model="[scheduledExecution : scheduledExecution,
                          jobAction          : 'runbook',
                          groupBreadcrumbMode: 'static',
                          followparams       : [mode: followmode, lastlines: params.lastlines],
                          jobDescriptionMode : 'open',
                          jobActionButtons   : false,
                  ]"/>
    </div>

    <div class="col-sm-6">
        %{--<tmpl:execOptionsForm--}%
        %{--model="${[scheduledExecution: scheduledExecution, crontab: crontab, authorized: authorized]}"--}%
        %{--hideHead="${true}"--}%
        %{--hideCancel="${true}"--}%
        %{--defaultFollow="${true}"/>--}%
    </div>

</div>
<g:set var="runAccess" value="${auth.jobAllowedTest(
        job: scheduledExecution,
        action: com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_RUN
)}"/>
<g:set var="runEnabled" value="${g.executionMode(is: 'active')}"/>
<g:set var="canRunJob" value="${runAccess && runEnabled}"/>
<g:set var="extendeddesc" value="${g.textRemainingLines(text: scheduledExecution.description)}"/>
<g:set var="rundoctext"
       value="${extendeddesc ? g.textAfterLine(text: extendeddesc, marker: rundeck.ScheduledExecution.RUNBOOK_MARKER) :
               null}"/>
<g:if test="${rundoctext}">
    <div class="row">
        <div class="col-sm-12">
            <div id="rundoc" class="panel panel-default">
                <div class="panel-body">
                    <div class="markdeep">${rundoctext.replaceAll('\\Q[[run]]\\E', '<div id="inlinerun"></div>')}</div>
                    <g:javascript>
                        jQuery(function () {
                            "use strict";
                            jQuery('#inlinerun').append(
                                    '<button class="btn btn-success">Run Job Now <i class="glyphicon glyphicon-play"></i></button>'
                            );
                        })
                    </g:javascript>
                </div>
            </div>
        </div>
    </div>
</g:if>

</body>
</html>


