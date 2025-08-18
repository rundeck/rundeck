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

<g:set var="uiType" value="${params.nextUi?'next':params.legacyUi?'legacy':'current'}"/>
<%@ page import="org.rundeck.core.auth.AuthConstants" %>

    <g:render template="/common/errorFragment"/>
    <g:render template="editLogFilterModal"/>
    <g:form method="POST"
            onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}"
            useToken="true"
            controller="scheduledExecution" action="save" params="[project: params.project]"
            class="form-horizontal">
        <div class="card">
        <g:if test="${uiType=='next'}">
            <div class="card-header job-editor-header-vue" id="job-editor-header-vue">
                <header-section />
            </div>
        </g:if>
        <g:else>
            <div class="card-header" data-ko-bind="jobeditor">
                <div class="row">
                    <h4 class="col-sm-10 card-title">
                        <span data-bind="css: {'text-secondary colon-after': jobName}"><g:message code="ScheduledExecution.page.create.title" /></span>
                        <span data-bind="text: jobName, attr: {title: groupPath}, bootstrapTooltip: groupPath"></span>
                    </h4>
                    <div class="col-sm-2 ">
                        <g:link controller="scheduledExecution" action="upload"
                                params="[project: params.project?:request.project]"
                                class="btn btn-default btn-sm pull-right">
                            <i class="glyphicon glyphicon-upload"></i>
                            <g:message code="upload.definition.button.label"/>
                        </g:link>
                    </div>
                </div>
            </div>
        </g:else>
        <div class="card-content">
            <tmpl:tabsEdit scheduledExecution="${scheduledExecution}" crontab="${crontab}" authorized="${authorized}"
                           command="${command}" jobComponents="${jobComponents}"/>
        </div>
        <div class="card-footer" data-ko-bind="jobeditor">
            <div id="schedCreateButtons">
                <g:actionSubmit id="createFormCancelButton"
                                value="${g.message(code:'cancel')}"
                                action="cancel"
                                onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"
                                class="btn btn-default reset_page_confirm"/>
                <g:submitButton name="Create" value="${g.message(code: 'button.action.Create')}"
                                    class="btn btn-cta reset_page_confirm" />

                <span data-bind="if: errorTabs().length" class="text-warning">
                    <g:message code="job.editor.workflow.unsavedchanges.warning" />
                </span>
            </div>
            <div id="schedCreateSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                <g:message code="creating.job.loading.text" />
            </div>
        </div>

      </div>
    </g:form>
