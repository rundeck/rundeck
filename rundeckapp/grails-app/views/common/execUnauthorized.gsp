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

<%
    response.setStatus(403,"Unauthorized") 
%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>Unauthorized Execution</title>

</head>
<body>

<div id="nowRunningContent">
    <div class="pageTop">
    <div class="floatl">
        <span class="welcomeMessage error">Unauthorized Execution</span>
    </div>
    <div class="clear"></div>
    </div>
</div>
<div class="pageBody" id="execUnauthorized">
    <g:if test="${scheduledExecution}">
        <div id="error" class="error note" >
            <g:if test="${scheduledExecution.id}">
                <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}"><g:enc>${scheduledExecution.jobName}</g:enc></g:link> cannot be executed:
            </g:if>
            <g:else>
                <g:enc>${scheduledExecution.jobName}</g:enc> cannot be executed:
            </g:else>
            User <g:enc>${session.user}</g:enc> is not authorized to execute the job

        </div>
    </g:if>
    <g:elseif test="${flash.error}">
        <div id="error" class="error note" >
            <g:enc>${flash.error}</g:enc>
        </div>
    </g:elseif>

</div>
</body>
</html>
