<g:set var="projectSet" value="${[]}"/>
<g:each in="${projects*.name.sort()}" var="proj">
    %{
        projectSet << ['key': proj, 'value': proj]
    }%
</g:each>
<auth:resourceAllowed action="create" kind="project" context="application">
    <g:if test="${!params.nocreate}">
        %{
            projectSet << [value: "Create new Project...", key: '-new-']
        }%
    </g:if>
</auth:resourceAllowed>
<g:select from="${projectSet}" optionKey='key' optionValue='value' name="${params.key ? params.key : 'projectSelect'}"
          onchange="${params.callback ? params.callback : 'selectProject'}(this.value);"
          value="${params.selected ? params.selected : project}"/>
<g:if test="${error}">
    <span class="error message">${error}</span>
</g:if>
