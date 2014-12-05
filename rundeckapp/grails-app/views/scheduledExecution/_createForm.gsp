<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

    <g:render template="/common/errorFragment"/>

    <g:form method="POST"
            onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}"
            useToken="true"
            controller="scheduledExecution" action="save" params="[project: params.project]"
            class="form-horizontal">
        <div class="panel panel-primary">
        <div class="panel-heading">
            <div class="row">
                <div class="col-sm-10">
                    <span class="h4">
                        <g:message code="ScheduledExecution.page.create.title" />
                    </span>
                </div>

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

        <g:render template="edit" model="['scheduledExecution':scheduledExecution, 'crontab':crontab,authorized:authorized]"/>

        <div class="panel-footer">
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
