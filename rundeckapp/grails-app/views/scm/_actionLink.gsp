<g:link controller="scm" action="performAction" params="${[
        project    : project,
        actionId: action.id,
        integration: integration
]+(linkparams?:[:])}"
        class="${classes?:''}"
        title="${action.description ?: ''}">
    <g:if test="${action.iconName}">
        <g:icon name="${action.iconName}"/>
    </g:if>
    ${action.title ?: 'Action'}</g:link>