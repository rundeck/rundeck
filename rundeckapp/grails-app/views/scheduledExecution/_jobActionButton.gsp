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
    <a href="#" class=" ${enc(attr: btnClass ?: 'btn btn-default btn-sm')} dropdown-toggle"
            data-toggle="dropdown"
            aria-expanded="false">
        <g:if test="${!hideIcon}">
        <i class="glyphicon glyphicon-list"></i>
        </g:if>
        <g:if test="${!hideTitle}">
            <g:message code="button.Action"/>
        </g:if>
        <span class="caret"></span>
    </a>
    <ul class="dropdown-menu dropdown-menu-right" role="menu">
        <g:render template="/scheduledExecution/jobActionButtonMenuContent" model="[scheduledExecution:scheduledExecution]"/>
    </ul>
</div>
%{--<g:set var="authEnableDisableExecution" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_TOGGLE_EXECUTION])}"/>--}%
<g:if test="${scheduledExecution.scheduled}">
    <auth:jobAllowed action="${AuthConstants.ACTION_TOGGLE_SCHEDULE}" job="${scheduledExecution}" project="${scheduledExecution.project}">
        <g:javascript>
            jQuery(function(){
                //register modal as the handler for 'job_delete_single'
                PageActionHandlers.registerModalHandler('enable_job_schedule_single','#jobdschedtoggle',{});
                PageActionHandlers.registerModalHandler('disable_job_schedule_single','#jobdschedtoggle',{});
            });
        </g:javascript>
        <div class="modal" id="jobdschedtoggle" tabindex="-1" role="dialog"
             aria-labelledby="schedenablejobtitle" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                                aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="schedenablejobtitle">
                            <g:if test="${scheduledExecution.hasScheduleEnabled()}">
                                <g:message code="job.single.disable.schedule.modal.title"/>
                            </g:if>
                            <g:else>
                                <g:message code="job.single.enable.schedule.modal.title"/>
                            </g:else>
                        </h4>
                    </div>

                    <g:form controller="scheduledExecution"
                            useToken="true"
                            action="flipScheduleEnabled"
                            params="[project: scheduledExecution.project, id: scheduledExecution.extid,returnToJob:true]"
                            method="post"
                            class="form form-horizontal">

                        <div class="modal-body">
                            <div class="form-group">
                                <label class="col-sm-2 control-label">
                                    <g:message code="scheduledExecution.jobName.label"/>
                                </label>

                                <div class="col-sm-10">
                                    <p class="form-control-static text-info"><g:enc>${scheduledExecution.jobName}</g:enc></p>
                                </div>
                            </div>

                            <g:if test="${scheduledExecution.hasScheduleEnabled()}">
                                <g:message code="job.single.disable.schedule.confirm.message"/>
                            </g:if>
                            <g:else>
                                <g:message code="job.single.enable.schedule.confirm"/>
                            </g:else>

                        </div>

                        <div class="modal-footer">
                            <g:hiddenField name="id" value="${scheduledExecution.extid}"/>
                            <g:hiddenField name="scheduleEnabled" value="${!scheduledExecution.hasScheduleEnabled()}"/>
                            <button type="submit" class="btn btn-default btn-sm "
                                    data-dismiss="modal">
                                <g:message code="cancel"/>
                            </button>
                            <g:if test="${scheduledExecution.hasScheduleEnabled()}">
                                <input type="submit" value="${g.message(code: 'job.single.disable.schedule.button')}" class="btn btn-danger btn-sm"/>
                            </g:if>
                            <g:else>
                                <input type="submit" value="${g.message(code: 'job.single.enable.schedule.button')}" class="btn btn-danger btn-sm"/>
                            </g:else>
                        </div>
                    </g:form>
                </div>
            </div>
        </div>
    </auth:jobAllowed>
</g:if>
<auth:jobAllowed action="${AuthConstants.ACTION_TOGGLE_EXECUTION}" job="${scheduledExecution}" project="${scheduledExecution.project}">
    <g:javascript>
        jQuery(function(){
            //register modal as the handler for 'job_delete_single'
            PageActionHandlers.registerModalHandler('enable_job_execution_single','#jobexectoggle',{});
            PageActionHandlers.registerModalHandler('disable_job_execution_single','#jobexectoggle',{});
        });
    </g:javascript>
    <div class="modal" id="jobexectoggle" tabindex="-1" role="dialog"
         aria-labelledby="toggleexecjobtitle" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal"
                            aria-hidden="true">&times;</button>
                    <h4 class="modal-title" id="toggleexecjobtitle">
                        <g:if test="${scheduledExecution.hasExecutionEnabled()}">
                            <g:message code="job.single.disable.execution.modal.title"/>
                        </g:if>
                        <g:else>
                            <g:message code="job.single.enable.execution.modal.title"/>
                        </g:else>
                    </h4>
                </div>

                <g:form controller="scheduledExecution"
                        useToken="true"
                        action="flipExecutionEnabled"
                        params="[project: scheduledExecution.project, id: scheduledExecution.extid,returnToJob:true]"
                        method="post"
                        class="form form-horizontal">

                    <div class="modal-body">
                        <div class="form-group">
                            <label class="col-sm-2 control-label">
                                <g:message code="scheduledExecution.jobName.label"/>
                            </label>

                            <div class="col-sm-10">
                                <p class="form-control-static text-info"><g:enc>${scheduledExecution.jobName}</g:enc></p>
                            </div>
                        </div>

                        <g:if test="${scheduledExecution.hasExecutionEnabled()}">
                            <g:message code="job.single.disable.execution.confirm.message"/>
                        </g:if>
                        <g:else>
                            <g:message code="job.single.enable.execution.confirm.message"/>
                        </g:else>

                    </div>

                    <div class="modal-footer">
                        <g:hiddenField name="id" value="${scheduledExecution.extid}"/>
                        <g:hiddenField name="executionEnabled" value="${!scheduledExecution.hasExecutionEnabled()}"/>
                        <button type="submit" class="btn btn-default btn-sm "
                                data-dismiss="modal">
                            <g:message code="cancel"/>
                        </button>
                        <g:if test="${scheduledExecution.hasExecutionEnabled()}">
                            <input type="submit" value="${g.message(code: 'job.single.disable.execution.button')}" class="btn btn-danger btn-sm"/>
                        </g:if>
                        <g:else>
                            <input type="submit" value="${g.message(code: 'job.single.enable.execution.button')}" class="btn btn-danger btn-sm"/>
                        </g:else>
                    </div>
                </g:form>
            </div>
        </div>
    </div>
</auth:jobAllowed>
<auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}"
                      project="${scheduledExecution.project ?: params.project ?: request.project}">
    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE, project: scheduledExecution.project)}">
<g:javascript>
jQuery(function(){
   //register modal as the handler for 'job_delete_single'
    PageActionHandlers.registerModalHandler('job_delete_single','#jobdelete',{});
});
</g:javascript>
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
                            <br/>
                            <g:if test="${parentList}">
                                <div class="form-group">
                                    <div class="col-sm-10 warn note">
                                        <g:message code="delete.referenced.job"/>
                                    </div>
                                </div>
                                <div class="form-group">
                                   <div class="col-sm-10">
                                       <ul>

                                    <g:each var="job" in="${parentList}">
                                        <li>
                                        <span class=" wfitem jobtype" title="">
                                        <g:link controller="scheduledExecution" action="show" id="${job.extid}">
                                            <i class="glyphicon glyphicon-book"></i>
                                            ${(job.groupPath?job.groupPath+'/':'')+job.jobName+(scheduledExecution.project!=job.project?' ('+job.project+')':'')}
                                        </g:link>
                                        </span>
                                        </li>
                                    </g:each>
                                       </ul>
                                   </div>
                                </div>
                            </g:if>
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
