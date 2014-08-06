<g:if test="${flash.message || request.message}">
        <g:autoLink>${g.html(value:flash.message)}${g.html(value:request.message)}</g:autoLink>
</g:if>
<g:if test="${flash.errors ||flash.error || request.error || flash.errorCode || request.errorCode}">
        <g:autoLink>${g.html(value:flash.error)}${request.error && request.error instanceof String? g.html(value:request.error):''}</g:autoLink>
        <g:if test="${flash.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${flash.errors}" as="list"/>
        </g:if>
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}" args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
</g:if>
<g:if test="${flash.warn || request.warn}">
        <g:autoLink>${g.html(value:flash.warn)}${g.html(value:request.warn)}</g:autoLink>
</g:if>
