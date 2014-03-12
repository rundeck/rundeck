<g:if test="${flash.message || request.message}">
    <g:set var="style" value="info"/>
</g:if>
<g:if test="${flash.error||request.error||flash.errorCode||request.errorCode}">
    <g:set var="style" value="danger"/>
</g:if>
<g:if test="${flash.warn || request.warn}">
    <g:set var="style" value="warn"/>
</g:if>
<g:if test="${style}">
<div class="alert alert-${style} alert-dismissable">
    <g:unless test="${notDismissable}">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
    </g:unless>
    <g:render template="/common/messagesText"/>
</div>
</g:if>
