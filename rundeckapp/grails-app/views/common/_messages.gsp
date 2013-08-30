<g:if test="${flash.message||request.message}">
    <div class="alert alert-success">${flash.message?.encodeAsHTML()}${request.message?.encodeAsHTML()}</div>
</g:if>
<g:if test="${flash.error||request.error}">
    <div class="alert alert-danger">${flash.error?.encodeAsHTML()}${request.error?.encodeAsHTML()}</div>
</g:if>
<g:if test="${flash.warn||request.warn}">
    <div class="alert alert-warning">${flash.warn?.encodeAsHTML()}${request.warn?.encodeAsHTML()}</div>
</g:if>
