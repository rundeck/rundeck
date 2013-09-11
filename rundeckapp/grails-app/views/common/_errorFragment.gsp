<g:if test="${flash.error || flash.message || error || message}">
    <div id="error" class="alert alert-dismissable alert-danger">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
        <p>${flash.error?: flash.message?: error ?: message}</p>
    </div>
</g:if>
