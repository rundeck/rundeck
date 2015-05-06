    by <g:username user="${execution.user}"/>
    <g:if test="${execution.dateCompleted == null}">
        <span class="isnode execstate execstatedisplay overall" data-bind="attr: { 'data-execstate': executionState() } ">
        </span>
    </g:if>
    <g:else>
        <span class="exec-status ${execution.statusSucceeded() ? 'succeed' : execution.cancelled?'warn': execution.customStatusString?'other':'fail'}">

            <g:if test="${execution.customStatusString}">
                "${execution.customStatusString}"
            </g:if>
            <g:else>
                <g:message code="status.label.${execution.executionState}"/>
                <g:if test="${execution.cancelled}">
                    <g:if test="${execution.abortedby}"> by <g:username user="${execution.abortedby}"/></g:if>
                </g:if>
            </g:else>

        </span>
    </g:else>
