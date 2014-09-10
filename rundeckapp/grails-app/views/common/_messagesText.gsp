<g:if test="${flash.message || request.message}">
        <g:autoLink><g:enc>${flash.message}</g:enc><g:enc>${request.message}</g:enc></g:autoLink>
</g:if>
<g:if test="${beanErrors ||flash.errors ||flash.error || request.error|| request.errorMessage || request.errors || flash.errorCode || request.errorCode}">
        <g:autoLink><g:enc>${flash.error}</g:enc>${request.error && request.error instanceof String? enc(html:request.error): request.errorMessage && request.errorMessage instanceof  String? enc(html:request.errorMessage):''}</g:autoLink>
        <g:if test="${flash.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${flash.errors}" as="list"/>
        </g:if>
        <g:if test="${request.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${request.errors}" as="list"/>
        </g:if>
        <g:if test="${request.errors instanceof java.util.Collection}">
            <ul>
            <g:each in="${request.errors}" var="err">
                <li><g:enc>${err}</g:enc></li>
            </g:each>
            </ul>
        </g:if>
        <g:if test="${beanErrors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${beanErrors}" as="list"/>
        </g:if>
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}" args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
</g:if>
<g:if test="${flash.warn || request.warn}">
        <g:autoLink><g:enc>${flash.warn}</g:enc><g:enc>${request.warn}</g:enc></g:autoLink>
</g:if>
