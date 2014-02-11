<g:link
        controller="execution"
        action="show"
        class="primary"
        id="${execution.id}"
        absolute="${absolute ? 'true' : 'false'}"
        params="${(followparams?.findAll { it.value }?:[:]) + [project: execution.project]}">
    <g:if test="${execution}">
        <i class="exec-status icon " data-bind="attr: { 'data-execstate': executionState }">
        </i>
    </g:if>
    <g:if test="${scheduledExecution}">
        <span class="primary"><g:message code="scheduledExecution.identity"
                                         args="[scheduledExecution.jobName, execution.id]"/></span>
    </g:if>
    <g:else>
        <span class="primary"><g:message code="execution.identity" args="[execution.id]"/></span>
    </g:else>

</g:link>
