<g:if test="${flash.message||request.message}">
    <div class="alert alert-info alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        ${flash.message?.encodeAsHTML()}${request.message?.encodeAsHTML()}
    </div>
</g:if>
<g:if test="${flash.error||request.error}">
    <div class="alert alert-danger alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        ${flash.error?.encodeAsHTML()}${request.error?.encodeAsHTML()}
    </div>
</g:if>
<g:if test="${flash.warn||request.warn}">
    <div class="alert alert-warning alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        ${flash.warn?.encodeAsHTML()}${request.warn?.encodeAsHTML()}
    </div>
</g:if>
