    by <g:username user="${execution.user}"/>
    <g:if test="${execution.dateCompleted == null}">
        <span class="execstatus">
        <span class="nowrunning">
            <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                 alt="Spinner"/>
            Running&hellip;
        </span>
        </span>
    </g:if>
    <g:else>
        <span class="${execution.status == 'true' ? 'succeed' : 'fail'}">
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
