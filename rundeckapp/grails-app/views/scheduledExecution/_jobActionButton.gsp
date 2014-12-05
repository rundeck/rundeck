<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
%{--
- Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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
<div class="btn-group">
    <g:if test="${includeRun}">
        <g:link controller="scheduledExecution"
                action="execute"
                id="${scheduledExecution.extid}"
                class=" btn btn-default btn-xs act_execute_job"
                params="[project: scheduledExecution.project]"
                title="${g.message(code:'action.prepareAndRun.tooltip')}"
                data-job-id="${scheduledExecution.extid}"
                >
            <b class="glyphicon glyphicon-play"></b>
        </g:link>
    </g:if>
    <button type="button" class="btn ${enc(attr: btnClass ?: ' btn-default btn-sm btn-link')} dropdown-toggle"
            data-toggle="dropdown"
            aria-expanded="false">
        <g:if test="${!hideIcon}">
        <i class="glyphicon glyphicon-list"></i>
        </g:if>
        <g:if test="${!hideTitle}">
            <g:message code="button.Action"/>
        </g:if>
        <span class="caret"></span>
    </button>
    <ul class="dropdown-menu" role="menu">
        <g:render template="/scheduledExecution/jobActionButtonMenuContent" model="[scheduledExecution:scheduledExecution]"/>
    </ul>
</div>
<auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}"
                      project="${scheduledExecution.project ?: params.project ?: request.project}">
    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE, project: scheduledExecution.project)}">
        <div class="modal" id="jobdelete" tabindex="-1" role="dialog"
             aria-labelledby="deletejobtitle" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                                aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="deletejobtitle">
                            <g:message code="really.delete.this.job"/>
                        </h4>
                    </div>

                    <g:form controller="scheduledExecution" useToken="true" action="delete"
                            method="post" class="form form-horizontal">
                        <div class="modal-body">
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

                        <div class="modal-footer">
                            <g:hiddenField name="id" value="${scheduledExecution.extid}"/>
                            <button type="submit" class="btn btn-default btn-sm "
                                    data-dismiss="modal">
                                <g:message code="cancel"/>
                            </button>
                            <input type="submit" value="${g.message(code: 'Delete')}"
                                   class="btn btn-danger btn-sm"/>
                        </div>
                    </g:form>
                </div>
            </div>
        </div>
    </g:if>
</auth:resourceAllowed>
