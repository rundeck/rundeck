<g:if test="${flash.message || request.message}">
    <div class="alert alert-info alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:if test="${flash.message || request.message}">
            <g:autoLink>${flash.message?.encodeAsHTML()}${request.message?.encodeAsHTML()}</g:autoLink>
        </g:if>
    </div>
</g:if>
<g:if test="${flash.error||request.error||flash.errorCode||request.errorCode}">
    <div class="alert alert-danger alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:if test="${flash.error || request.error || flash.errorCode || request.errorCode}">
            <g:autoLink>${flash.error?.encodeAsHTML()}${request.error && request.error instanceof String ?
                request.error.encodeAsHTML() : ''}</g:autoLink>
            <g:if test="${flash.errorCode ?: request.errorCode}">
                <g:message code="${flash.errorCode ?: request.errorCode}"
                           args="${flash.errorArgs ?: request.errorArgs}"/>
            </g:if>
        </g:if>
    </div>
</g:if>
<g:if test="${flash.warn || request.warn}">
    <div class="alert alert-warn alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:if test="${flash.warn || request.warn}">
            <g:autoLink>${flash.warn?.encodeAsHTML()}${request.warn?.encodeAsHTML()}</g:autoLink>
        </g:if>
    </div>
</g:if>
