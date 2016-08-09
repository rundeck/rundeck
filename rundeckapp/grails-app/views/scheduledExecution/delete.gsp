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
  Date: 3/25/15
  Time: 4:16 PM
--%>

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message
            code="ScheduledExecution.page.delete.title"/></title>

</head>
<body>
<g:form controller="scheduledExecution" useToken="true" action="delete" method="post" class="form form-horizontal">
    <div class="panel panel-primary">
    <div class="panel-heading">
        <span class="h3"><g:message code="really.delete.this.job" /></span>
    </div>
    <div class="panel-body">
        <div class="form-group">
            <label class="col-sm-2 control-label">
                <g:message code="scheduledExecution.jobName.label"/>
            </label>

            <div class="col-sm-10">
                <p class="form-control-static text-info"><g:enc>${scheduledExecution.jobName}</g:enc></p>
            </div>
        </div>

        <auth:resourceAllowed type="project"
                              name="${scheduledExecution.project}"
                              context="application"
                              action="${[AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]}"
                              any="true">
            <div class="form-group">
                <div class="col-sm-10 col-sm-offset-2">
                    <label class="checkbox">
                        <input type="checkbox" name="deleteExecutions"
                               value="true"/>
                        <g:message code="delete.all.executions.of.this.job"/>
                    </label>
                </div>
            </div>
        </auth:resourceAllowed>

    </div>

    <div class="panel-footer">
        <g:hiddenField name="id" value="${scheduledExecution.extid}"/>
        <g:actionSubmit value="Cancel" action="cancel" class="btn btn-default btn-sm "/>
        <input type="submit" value="${g.message(code: 'Delete')}"
               class="btn btn-danger btn-sm"/>
    </div>
    </div>
</g:form>
</body>
</html>