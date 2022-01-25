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

<%@ page import="org.rundeck.core.auth.AuthConstants" %>

<g:render template="/common/errorFragment"/>
<g:render template="editLogFilterModal"/>


%{--Edit job form--}%
<g:form controller="scheduledExecution" method="post"
        action="update"
        params="[project:params.project]"
        useToken="true"
        class="form-horizontal"
        onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">

<div class="card " id="editForm">
    <div class="card-header" data-ko-bind="jobeditor">
        <div class="row">
            <h4 class="col-sm-10 card-title">
                <span class="text-secondary colon-after"><g:message code="ScheduledExecution.page.edit.title" /></span>
                <a class="link-quiet text-strong" href="#" data-bind="text: jobName, attr: {href: href, title: groupPath}, bootstrapTooltip: groupPath">

                </a>
                <span class=" text-muted" data-bind="text: uuid"></span>
            </h4>

            <auth:resourceAllowed action="${AuthConstants.ACTION_CREATE}"
                                  project="${params.project}" kind="${AuthConstants.TYPE_JOB}">

                <div class="col-sm-2 ">
                    <g:link controller="scheduledExecution" action="upload"
                            params="[project: params.project ?: request.project]"
                            class="btn btn-default btn-sm pull-right">
                        <i class="glyphicon glyphicon-upload"></i>
                        <g:message code="upload.definition.button.label" />
                    </g:link>
                </div>
            </auth:resourceAllowed>
        </div>
    </div>

    <div class="card-content">
        <tmpl:tabsEdit scheduledExecution="${scheduledExecution}" crontab="${crontab}" authorized="${authorized}"
                       command="${command}" sessionOpts="${sessionOpts}"/>
    </div>

    <div class="card-footer" data-ko-bind="jobeditor">
      <g:actionSubmit id="editFormCancelButton" value="${g.message(code: 'cancel')}"
                      onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"
                      class="btn btn-default reset_page_confirm"
                      action="Cancel"/>
      <g:actionSubmit value="${g.message(code: 'button.action.Save')}" action="Update" class="btn btn-primary reset_page_confirm " id="jobUpdateSaveButton"/>
        <span data-bind="if: inPageError()" class="text-warning">
            <g:message code="job.editor.workflow.unsavedchanges.warning" />
        </span>
    </div>

</div>

</g:form>
