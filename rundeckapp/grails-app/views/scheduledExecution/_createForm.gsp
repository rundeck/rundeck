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

    <g:render template="/common/errorFragment"/>
    <g:render template="editLogFilterModal"/>
    <g:form method="POST"
            onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}"
            useToken="true"
            controller="scheduledExecution" action="save" params="[project: params.project]"
            class="form-horizontal">
        <div class="card">
        <div class="card-header">
            <div class="row">
                <h4 class="col-sm-10 card-title">
                  <g:message code="ScheduledExecution.page.create.title" />
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
        <div class="card-content">
            <tmpl:tabsEdit scheduledExecution="${scheduledExecution}" crontab="${crontab}" authorized="${authorized}"
                           command="${command}"/>
        </div>
        <div class="card-footer">
            <g:javascript>
                <wdgt:eventHandlerJS for="scheduledTrue" state="unempty" >
                    <wdgt:action visible="true" targetSelector=".cformAllowSaveOnly"/>
                    <wdgt:action visible="false" targetSelector=".cformAllowRun"/>
                </wdgt:eventHandlerJS>
            </g:javascript>
            <div id="schedCreateButtons">
                <g:actionSubmit id="createFormCancelButton" value="${g.message(code:'cancel')}"
                                onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"
                                class="btn btn-default reset_page_confirm"/>
                <g:submitButton name="Create" value="${g.message(code: 'button.action.Create')}"
                                    class="cformAllowSave cformAllowSaveOnly btn btn-primary reset_page_confirm" />

            </div>
            <div id="schedCreateSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                <g:message code="creating.job.loading.text" />
            </div>
        </div>

      </div>
    </g:form>
