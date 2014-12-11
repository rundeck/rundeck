<g:set var="projectSet" value="${projects.sort()}"/>
<g:set var="selectParams" value="${selectParams?:[:]}"/>
    <a data-toggle="dropdown" href="#">
        <i class="glyphicon glyphicon-tasks"></i>
        <g:enc>${ selectItemTitle}${project?:params.project?:request.project?: 'Choose ...'}</g:enc>
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
                <g:link controller="menu" action="index" params="${selectParams + [project: project]}" >
                    <i class="glyphicon glyphicon-tasks"></i>
                    <g:enc>${project}</g:enc>
                </g:link>
            </li>
        </g:each>
    </ul>
