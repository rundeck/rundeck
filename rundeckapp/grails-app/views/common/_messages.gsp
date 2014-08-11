<g:if test="${flash.message || request.message}">
    <div class="alert alert-info alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:autoLink><g:enc>${flash.message}${request.message}</g:enc></g:autoLink>
    </div>
</g:if>
<g:if test="${flash.error||flash.errors!=null||request.error||request.errors||flash.errorCode||request.errorCode}">
    <div class="alert alert-danger alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:autoLink><g:enc>${flash.error}${request.error && request.error instanceof String ?
            request.error : ''}</g:enc></g:autoLink>
        <g:if test="${flash.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${flash.errors}" as="list"/>
        </g:if>
        <g:if test="${request.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${request.errors}" as="list"/>
        </g:if>
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}"
                       args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
    </div>
</g:if>
<g:if test="${flash.warn || request.warn}">
    <div class="alert alert-warn alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:autoLink><g:enc>${flash.warn}${request.warn}</g:enc></g:autoLink>
    </div>
</g:if>
