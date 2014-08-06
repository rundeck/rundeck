<g:if test="${flash.message || request.message}">
        <g:autoLink>${g.enc(html:flash.message)}${g.enc(html:request.message)}</g:autoLink>
</g:if>
<g:if test="${flash.errors ||flash.error || request.error || request.errors || flash.errorCode || request.errorCode}">
        <g:autoLink>${g.enc(html:flash.error)}${request.error && request.error instanceof String? g.enc(html:request.error):''}</g:autoLink>
        <g:if test="${flash.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${flash.errors}" as="list"/>
        </g:if>
        <g:if test="${request.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${request.errors}" as="list"/>
        </g:if>
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}" args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
</g:if>
<g:if test="${flash.warn || request.warn}">
        <g:autoLink>${g.enc(html:flash.warn)}${g.enc(html:request.warn)}</g:autoLink>
</g:if>
