<g:if test="${flash.message||request.message}">
    <div class="alert alert-info alert-dismissable">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        ${flash.message?.encodeAsHTML()}${request.message?.encodeAsHTML()}
    </div>
</g:if>
<g:if test="${flash.error||request.error}">
    <div class="alert alert-danger alert-dismissable">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        ${flash.error?.encodeAsHTML()}${request.error?.encodeAsHTML()}
    </div>
</g:if>
<g:if test="${flash.warn||request.warn}">
    <div class="alert alert-warning alert-dismissable">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        ${flash.warn?.encodeAsHTML()}${request.warn?.encodeAsHTML()}
    </div>
</g:if>
