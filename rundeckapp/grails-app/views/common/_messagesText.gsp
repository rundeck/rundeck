<g:if test="${flash.message || request.message}">
        ${flash.message?.encodeAsHTML()}${request.message?.encodeAsHTML()}
</g:if>
<g:if test="${flash.error || request.error || flash.errorCode || request.errorCode}">
        ${flash.error?.encodeAsHTML()}${request.error?.encodeAsHTML()}
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}" args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
</g:if>
<g:if test="${flash.warn || request.warn}">
        ${flash.warn?.encodeAsHTML()}${request.warn?.encodeAsHTML()}
</g:if>
