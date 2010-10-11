<g:if test="${execution}">
    <p>
        <g:if test="${execution.status=='true'}">
            <img src="${resource(dir: 'images', file: 'icon-tiny-ok.png')}" alt="" width="12px" height="12px"/>
        </g:if>
        <g:else>
            <img src="${resource(dir: 'images', file: 'icon-tiny-warn.png')}" alt="" width="12px" height="12px"/>
        </g:else>
        Execution ${execution.status=='true'?'Succeeded':execution.cancelled?'Killed':'Failed'}</p>
    <p>Duration: ${execution.durationAsString()}</p>
    <p>User: ${execution.user}</p>
</g:if>
