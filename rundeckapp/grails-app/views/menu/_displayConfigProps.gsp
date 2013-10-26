<g:each in="${keys.sort()}" var="key">
    <div>
        <code>${key.encodeAsHTML()}:
        <g:if test="${obscurePattern && key=~obscurePattern}">
            *****
        </g:if>
        <g:else>
            ${map[key]?.encodeAsHTML()}
        </g:else>
        </code>
    </div>
</g:each>
