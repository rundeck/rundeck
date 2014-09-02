<g:each in="${keys.sort()}" var="key">
    <div>
        <code><g:enc>${key}</g:enc>:
        <g:if test="${obscurePattern && key=~obscurePattern}">
            *****
        </g:if>
        <g:else>
            <g:enc>${map[key]}</g:enc>
        </g:else>
        </code>
    </div>
</g:each>
