<g:set var="wasSaved" value="${(params?.saved=='true') || iscopy  || scheduledExecution?.id || scheduledExecution?.jobName}"/>

<div class="pageBody form">
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
                <g:actionSubmit id="createFormCancelButton" value="Cancel"/>
                <g:if test="${auth.allowedTest(job:[jobName:'create', groupPath:'ui'], action:[UserAuth.WF_CREATE])}">
                    <g:actionSubmit action="save" value="Create"  class="cformAllowSave cformAllowSaveOnly"
                        style="${wdgt.styleVisible(if:scheduledExecution.scheduled || wasSaved)}"/>
                </g:if>

                <g:if test="${auth.allowedTest(job:[jobName:'create', groupPath:'ui'], action:[UserAuth.WF_RUN,UserAuth.WF_CREATE])}">
                    <g:actionSubmit action="saveAndExec" value="Create And Run"  class="cformAllowSave cformAllowRun"
                        style="${ wdgt.styleVisible(unless:scheduledExecution.scheduled || !wasSaved) }"/>
                </g:if>
                <g:if test="${auth.allowedTest(job:[jobName:'adhoc_run', groupPath:'ui'], action:UserAuth.WF_RUN)}">
                    <g:actionSubmit action="execAndForget" value="Run And Forget"  class="cformAllowRunAndForget cformAllowRun"
                        style="${ wdgt.styleVisible(if:!scheduledExecution.scheduled && !wasSaved) }" />
                </g:if>
                <g:if test="${auth.allowedTest(has:false, job:[jobName:'run_and_forget', groupPath:'ui'], action:UserAuth.WF_RUN)}">
                    <span class="error message cformAllowRunAndForget cformAllowRun">Not authorized to Run Jobs</span>
                </g:if>
            </div>
            <div id="schedCreateSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                Creating Scheduled Command...
            </div>
        </div>
    </g:form>
</div>