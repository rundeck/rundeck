<g:set var="projectSet" value="${projects*.name.sort()}"/>
<g:set var="selectParams" value="${selectParams?:[:]}"/>
    <a data-toggle="dropdown" href="#">
        <i class="glyphicon glyphicon-tasks"></i>
        ${project?:params.project?:request.project}
        <i class="caret"></i>
    </a>
    <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
<auth:resourceAllowed action="create" kind="project" context="application">
        <g:if test="${!params.nocreate}">
            <li>
                <g:link controller="framework" action="createProject">
                    New Project
                    <b class="glyphicon glyphicon-plus"></b>
                </g:link>
            </li>
            <li class="divider">
            </li>

        </g:if>
</auth:resourceAllowed>
        <g:each var="project" in="${projectSet}">
            <li>
                <g:link controller="menu" action="index" params="${[project: project] + selectParams}" >
                    <i class="glyphicon glyphicon-tasks"></i>
                    ${project.encodeAsHTML()}
                </g:link>
            </li>
        </g:each>
    </ul>
