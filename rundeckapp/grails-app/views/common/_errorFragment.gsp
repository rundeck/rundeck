<g:if test="${flash.error || flash.message || error || message}">
    <div id="error" class="error note">
        ${flash.error?: flash.message?: error ?: message}
    </div>
</g:if>