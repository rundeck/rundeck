<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

    <g:render template="/common/errorFragment"/>

    <g:form method="post" onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}"
            class="form-horizontal"
    >
        <div class="panel panel-primary">
        <div class="panel-heading">
            <div class="row">
                <div class="col-sm-10">
                    <span class="h4">
                        Create New <g:message code="domain.ScheduledExecution.title"/>
                    </span>
                </div>

                <div class="col-sm-2 ">
                    <g:link controller="scheduledExecution" action="upload"
                            params="[project: params.project?:request.project]"
                            class="btn btn-default btn-sm pull-right">
                        <i class="glyphicon glyphicon-upload"></i>
                        Upload Definition&hellip;
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
                <g:actionSubmit id="createFormCancelButton" value="Cancel"
                                onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"
                                class="btn btn-default"/>
                <g:if test="${auth.resourceAllowedTest( kind:'job',action:[AuthConstants.ACTION_CREATE],project: params.project ?: request.project)}">
                    <g:actionSubmit action="save" value="Create"
                                    class="cformAllowSave cformAllowSaveOnly btn btn-primary" />
                </g:if>

                <g:if test="${auth.resourceAllowedTest( has:false, kind:'job', action:[AuthConstants.ACTION_CREATE],project: params.project ?: request.project)}">
                    <span class="error message cformAllowRunAndForget cformAllowRun">Not authorized to Save Jobs</span>
                </g:if>
            </div>
            <div id="schedCreateSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                Creating Job...
            </div>
        </div>

        </div>
    </g:form>
