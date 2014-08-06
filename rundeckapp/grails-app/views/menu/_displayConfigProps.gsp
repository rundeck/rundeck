<g:each in="${keys.sort()}" var="key">
    <div>
        <code>${enc(html:key)}:
        <g:if test="${obscurePattern && key=~obscurePattern}">
            *****
        </g:if>
        <g:else>
            ${enc(html:map[key])}
        </g:else>
        </code>
    </div>
</g:each>
