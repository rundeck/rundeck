<g:if test="${flash.message||request.message}">
    <span class="message note">${flash.message}${request.message}</span>
</g:if>
<g:if test="${flash.error||request.error}">
    <span class="error note">${flash.error}${request.error}</span>
</g:if>
<g:if test="${flash.warn||request.warn}">
    <span class="warn note">${flash.warn}${request.warn}</span>
</g:if>