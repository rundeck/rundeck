<g:set var="parsed" value="${g.parseOptsFromString(args: argString)}"/>
<g:if test="${parsed}">
    <g:each in="${parsed}" var="entry">
        ${entry.key.encodeAsHTML()}:
        <g:if test="${entry.value}"><code class="optvalue">${entry.value?.encodeAsHTML()}</code></g:if>
    </g:each>
</g:if>
<g:else>
    <code class="optvalue">${argString?.encodeAsHTML()}</code>
</g:else>
