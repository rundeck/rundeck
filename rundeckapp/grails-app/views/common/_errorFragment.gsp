<g:if test="${flash.error || flash.message || beanErrors || error || message}">
    <div id="error" class="alert alert-dismissable alert-danger">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
        ${enc(html:flash.error?: flash.message?: error ?: message)}

        <g:if test="${beanErrors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${beanErrors}" as="list"/>
        </g:if>

    </div>
</g:if>
