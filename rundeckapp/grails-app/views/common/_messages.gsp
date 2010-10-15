<g:if test="${flash.message||request.message}">
    <span class="message note">${flash.message?.encodeAsHTML()}${request.message?.encodeAsHTML()}</span>
</g:if>
<g:if test="${flash.error||request.error}">
    <span class="error note">${flash.error?.encodeAsHTML()}${request.error?.encodeAsHTML()}</span>
</g:if>
<g:if test="${flash.warn||request.warn}">
    <span class="warn note">${flash.warn?.encodeAsHTML()}${request.warn?.encodeAsHTML()}</span>
</g:if>