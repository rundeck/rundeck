<g:link
        controller="execution"
        action="show"
        class="primary"
        id="${execution.id}"
        absolute="${absolute ? 'true' : 'false'}"
        params="${followparams?.findAll { it.value }}">
    <span class="jobIcon ${execution?.status == 'true' ? 'jobok' : execution?.cancelled ? 'jobwarn' : 'joberror'}">
        <g:if test="${execution}">
            <i class="exec-status icon ${!execution.dateCompleted ? 'running' : execution.status == 'true' ? 'succeed' : execution.cancelled ? 'warn' : 'fail'}">
            </i>
        </g:if>
    </span>
    <g:if test="${scheduledExecution}">
        <span class="primary"><g:message code="scheduledExecution.identity"
                                         args="[scheduledExecution.jobName, execution.id]"/></span>
    </g:if>
    <g:else>
        <span class="primary"><g:message code="execution.identity" args="[execution.id]"/></span>
    </g:else>

</g:link>
