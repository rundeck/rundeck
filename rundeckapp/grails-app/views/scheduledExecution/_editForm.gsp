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

%{--Edit job form--}%
<g:form controller="scheduledExecution" method="post"
        action="update"
        params="[project:params.project]"
        useToken="true"
        class="form-horizontal"
        onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">

<div class="panel panel-primary obs_delete_hide" id="editForm">
    <div class="panel-heading">
        <div class="row">
            <div class="col-sm-10">
        <span class="h4">
            <g:message code="ScheduledExecution.page.edit.title" />
        </span>
            </div>

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

        <g:render template="edit" model="[scheduledExecution:scheduledExecution, crontab:crontab, command:command,authorized:authorized]"/>

        <div class="panel-footer">
            <div class="row">
            <div class="buttons col-sm-10">

                <g:actionSubmit id="editFormCancelButton" value="${g.message(code: 'cancel')}"
                                onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"
                                class="btn btn-default reset_page_confirm"/>
                <g:actionSubmit value="${g.message(code: 'button.action.Save')}" action="Update" class="btn btn-primary reset_page_confirm "/>

            </div>
            </div>
        </div>

</div>

</g:form>
<g:javascript>
fireWhenReady('editForm',function(){
    $$('.behavior_delete_show').each(function(e){
        Event.observe(e,'click',function(evt){
            evt.stop();
            $$('.obs_delete_hide').each(Element.hide);
            $$('.obs_delete_show').each(Element.show);
        })
    });
    $$('.behavior_delete_hide').each(function(e){
        Event.observe(e,'click',function(evt){
            evt.stop();
            $$('.obs_delete_hide').each(Element.show);
            $$('.obs_delete_show').each(Element.hide);
        })
    });
});
</g:javascript>
