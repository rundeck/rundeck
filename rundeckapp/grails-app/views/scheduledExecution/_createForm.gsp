<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %><g:set var="wasSaved" value="${(params?.saved=='true') || iscopy  || scheduledExecution?.id || scheduledExecution?.jobName}"/>

<div class="pageBody form">
    <g:render template="/common/errorFragment"/>
    <g:form method="post" onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">
        <g:render template="edit" model="['scheduledExecution':scheduledExecution, 'crontab':crontab,authorized:authorized]"/>
        <div class="buttons">
            <g:javascript>
                <wdgt:eventHandlerJS for="savedFalse" state="unempty" >
                    <wdgt:action visible="false" targetSelector=".cformAllowSave"/>
                    <wdgt:action visible="true" targetSelector=".cformAllowRunAndForget"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="savedTrue" state="unempty" >
                    <wdgt:action visible="true" targetSelector=".cformAllowSave"/>
                    <wdgt:action visible="false" targetSelector=".cformAllowRunAndForget"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="scheduledTrue" state="unempty" >
                    <wdgt:action visible="true" targetSelector=".cformAllowSaveOnly"/>
                    <wdgt:action visible="false" targetSelector=".cformAllowRun"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="scheduledFalse" state="unempty" visible="true" targetSelector=".cformAllowSave">
                    <wdgt:condition for="savedTrue" state="unempty"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="scheduledFalse" state="unempty" visible="true" targetSelector=".cformAllowRunAndForget">
                    <wdgt:condition for="savedFalse" state="unempty"/>
                </wdgt:eventHandlerJS>
            </g:javascript>
            <div id="schedCreateButtons">
                <g:actionSubmit id="createFormCancelButton" value="Cancel" onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"/>
                <g:if test="${auth.resourceAllowedTest( kind:'job',action:[AuthConstants.ACTION_CREATE])}">
                    <g:actionSubmit action="save" value="Create"  class="cformAllowSave cformAllowSaveOnly"
                        style="${wdgt.styleVisible(if:scheduledExecution.scheduled || wasSaved)}"/>
                </g:if>

                <g:if test="${auth.resourceAllowedTest( kind:'job', action:[AuthConstants.ACTION_CREATE])}">
                    <g:actionSubmit action="saveAndExec" value="Create And Run"  class="cformAllowSave cformAllowRun"
                        style="${ wdgt.styleVisible(unless:scheduledExecution.scheduled || !wasSaved) }"/>
                </g:if>
                <g:if test="${auth.adhocAllowedTest(action:AuthConstants.ACTION_RUN)}">
                    <g:actionSubmit action="execAndForget" value="Run And Forget"  class="cformAllowRunAndForget cformAllowRun"
                        style="${ wdgt.styleVisible(if:!scheduledExecution.scheduled && !wasSaved) }" />
                </g:if>
                %{--<g:if test="${auth.resourceAllowedTest( has:false, kind:'job', action:[AuthConstants.RUN])}">--}%
                    %{--<span class="error message cformAllowRunAndForget cformAllowRun">Not authorized to Run Jobs</span>--}%
                %{--</g:if>--}%
                <g:if test="${auth.resourceAllowedTest( has:false, kind:'job', action:[AuthConstants.ACTION_CREATE])}">
                    <span class="error message cformAllowRunAndForget cformAllowRun">Not authorized to Save Jobs</span>
                </g:if>
            </div>
            <div id="schedCreateSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                Creating Scheduled Command...
            </div>
        </div>
    </g:form>
</div>