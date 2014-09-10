<g:if test="${beanErrors || flash.errors || flash.error || request.error ||request.errorMessage || request.errors || flash.errorCode || request.errorCode}">
    <div id="error" class="alert alert-dismissable alert-danger">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
        <g:render template="/common/messagesText"/>
    </div>
</g:if>
