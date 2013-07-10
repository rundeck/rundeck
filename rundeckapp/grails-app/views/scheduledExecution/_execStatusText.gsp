<span class="execstatus">
    <g:if test="${execution.dateCompleted == null}">
        <span class="nowrunning">
            <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                 alt="Spinner"/>
            Running&hellip;
        </span>
    </g:if>
    <g:else>
        <span class="${execution.status == 'true' ? 'succeed' : 'fail'}">
            <g:if test="${execution.status == 'true'}">
                Succeeded
            </g:if>
            <g:elseif test="${execution.cancelled}">
                Killed<g:if
                    test="${execution.abortedby}">by: ${execution.abortedby.encodeAsHTML()}</g:if>
            </g:elseif>
            <g:else>
                Failed
            </g:else>
        </span>
    </g:else>
</span>
