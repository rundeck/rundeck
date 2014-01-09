<%@ page import="rundeck.ScheduledExecution" %>

<span class="jobInfo" id="jobInfo_${execution.id}">
        <span class="h3">
            <g:render template="/scheduledExecution/showExecutionLink" />

        </span>
        <h4 >
            <small>
                <g:render template="/scheduledExecution/showExecutionDate"/>
            </small>
        </h4>
</span>
