<g:set var="projectSet" value="${[]}"/>
<g:each in="${projects*.name.sort()}" var="proj">
    %{
        projectSet << ['key': proj, 'value': proj]
    }%
</g:each>

%{--<g:select from="${projectSet}" optionKey='key' optionValue='value' name="${params.key ? params.key : 'projectSelect'}"--}%
            %{--id="projectSelectInput"--}%
          %{--onchange="${params.callback ? params.callback : 'selectProject'}(this.value);"--}%
          %{--value="${params.selected ? params.selected : project}"/>--}%
    <a data-toggle="dropdown" href="#">
        Project: ${session.project}
        <i class="caret"></i>
    </a>
    <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
<auth:resourceAllowed action="create" kind="project" context="application">
        <g:if test="${!params.nocreate}">
            <li>
                <a onclick="${params.callback?:'selectProject'}('-new-')">
                    New Project
                    <b class="glyphicon glyphicon-plus"></b>
                </a>
            </li>
            <li class="divider">
            </li>

        </g:if>
</auth:resourceAllowed>
        <g:each var="project" in="${projectSet}">
            <li>
                <a onclick="${params.callback?:'selectProject'}(this.value)">
                    ${project.key}
                </a>
            </li>
        </g:each>
    </ul>
