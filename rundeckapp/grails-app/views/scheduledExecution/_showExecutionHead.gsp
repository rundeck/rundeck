<%@ page import="rundeck.ScheduledExecution" %>

<span class="jobInfo" id="jobInfo_${execution.id}">
        <span class="h3">
            <g:render template="showExecutionLink" />

        </span>
        <h4 >
            <small>
                <g:render template="showExecutionDate"/>
            </small>
        </h4>
</span>
