
<g:if test="${status.toString()=='MODIFIED'}">
    <span class="text-info">
        <g:if test="${!noicon}">
            <span class="glyphicon glyphicon-pencil"></span>
        </g:if>
        <g:if test="${!notext}">
            ${text?:'modified'}
        </g:if>
    </span>
</g:if>
<g:elseif test="${status.toString()=='CREATED'}">
    <span class="text-success">
        <g:if test="${!noicon}">
            <span class="glyphicon glyphicon-plus"></span>
        </g:if>
        <g:if test="${!notext}">
            ${text?:'created'}
        </g:if>
    </span>
</g:elseif>
<g:elseif test="${status.toString()=='DELETED'}">
    <span class="text-danger">
        <g:if test="${!noicon}">
            <span class="glyphicon glyphicon-minus-sign"></span>
        </g:if>
        <g:if test="${!notext}">
            ${text?:'deleted'}
        </g:if>
    </span>
</g:elseif>
<g:elseif test="${status.toString()=='CLEAN'}">
    <span class="${iscommit?'text-muted':'text-success'}">
        <g:if test="${!noicon}">
            <span class="glyphicon glyphicon-ok-sign"></span>
        </g:if>
        <g:if test="${text}">
            ${text?:'deleted'}
        </g:if>
    </span>
</g:elseif>
<g:else>
    <span class="${iscommit?'text-success':'text-muted'}">
        <g:if test="${!noicon}">
            <span class="glyphicon ${iscommit?'glyphicon-plus':'glyphicon-question-sign'}"></span>
        </g:if>
        <g:if test="${!notext}">
            ${text?:'not-found'}
        </g:if>
    </span>
</g:else>