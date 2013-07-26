<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<div class="pageBody form">
    <g:render template="/common/errorFragment"/>
    <g:form method="post" onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">
        <g:render template="edit" model="['scheduledExecution':scheduledExecution, 'crontab':crontab,authorized:authorized]"/>
        <div class="buttons">
            <g:javascript>
                <wdgt:eventHandlerJS for="scheduledTrue" state="unempty" >
                    <wdgt:action visible="true" targetSelector=".cformAllowSaveOnly"/>
                    <wdgt:action visible="false" targetSelector=".cformAllowRun"/>
                </wdgt:eventHandlerJS>
            </g:javascript>
            <div id="schedCreateButtons">
                <g:actionSubmit id="createFormCancelButton" value="Cancel" onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"/>
                <g:if test="${auth.resourceAllowedTest( kind:'job',action:[AuthConstants.ACTION_CREATE])}">
                    <g:actionSubmit action="save" value="Create"  class="cformAllowSave cformAllowSaveOnly" />
                </g:if>

                <g:if test="${auth.resourceAllowedTest( kind:'job', action:[AuthConstants.ACTION_CREATE])}">
                    <g:actionSubmit action="saveAndExec" value="Create And Run"  class="cformAllowSave cformAllowRun"/>
                </g:if>

                <g:if test="${auth.resourceAllowedTest( has:false, kind:'job', action:[AuthConstants.ACTION_CREATE])}">
                    <span class="error message cformAllowRunAndForget cformAllowRun">Not authorized to Save Jobs</span>
                </g:if>
            </div>
            <div id="schedCreateSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                Creating Job...
            </div>
        </div>
    </g:form>
</div>
