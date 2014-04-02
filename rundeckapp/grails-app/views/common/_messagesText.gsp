<g:if test="${flash.message || request.message}">
        <g:autoLink>${flash.message?.encodeAsHTML()}${request.message?.encodeAsHTML()}</g:autoLink>
</g:if>
<g:if test="${flash.error || request.error || flash.errorCode || request.errorCode}">
        <g:autoLink>${flash.error?.encodeAsHTML()}${request.error && request.error instanceof String?
    request.error.encodeAsHTML():''}</g:autoLink>
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}" args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
</g:if>
<g:if test="${flash.warn || request.warn}">
        <g:autoLink>${flash.warn?.encodeAsHTML()}${request.warn?.encodeAsHTML()}</g:autoLink>
</g:if>
