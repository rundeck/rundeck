
<g:set var="textColor" value="text-info"/>
<g:set var="iconName" value="${iscommit?'glyphicon-plus':'glyphicon-question-sign'}"/>
<g:set var="defaultText" value="${message(code: "scm.export.status.${status}.display.text", default: status.toString())}"/>

<g:if test="${status.toString()=='EXPORT_NEEDED'}">
    <g:set var="textColor" value="text-info"/>
    <g:set var="iconName" value="glyphicon-exclamation-sign"/>
</g:if>
<g:elseif test="${status.toString()=='CREATE_NEEDED'}">
    <g:set var="textColor" value="text-success"/>
    <g:set var="iconName" value="glyphicon-exclamation-sign"/>
</g:elseif>
<g:elseif test="${status.toString()=='DELETED'}">
    <g:set var="textColor" value="text-danger"/>
    <g:set var="iconName" value="glyphicon-minus-sign"/>

</g:elseif>
<g:elseif test="${status.toString()=='CLEAN'}">
    <g:set var="textColor" value="text-muted"/>
    <g:set var="iconName" value="glyphicon-ok"/>
</g:elseif>

<span class="${textColor}">
    <g:if test="${!noicon}">
        <g:icon name="${icon?:iconName}"/>
    </g:if>
    <g:if test="${!notext}">
        ${text?:defaultText}
    </g:if>
</span>