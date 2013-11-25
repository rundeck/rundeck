    by <g:username user="${execution.user}"/>
    <g:if test="${execution.dateCompleted == null}">
        <span class="isnode execstate execstatedisplay overall" data-bind="attr: { 'data-execstate': executionState() } ">
        </span>
    </g:if>
    <g:else>
        <span class="exec-status ${execution.status == 'true' ? 'succeed' : execution.cancelled?'warn': 'fail'}">
            <g:if test="${execution.status == 'true'}">
                Succeeded
            </g:if>
            <g:elseif test="${execution.cancelled}">
                Killed<g:if
                    test="${execution.abortedby}"> by <g:username user="${execution.abortedby}"/></g:if>
            </g:elseif>
            <g:else>
                Failed
            </g:else>
        </span>
    </g:else>
