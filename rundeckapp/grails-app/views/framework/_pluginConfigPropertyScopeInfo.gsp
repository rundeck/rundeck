<g:set var="scopeinfo" value="${g.rkey()}"/>
<g:unless test="${outofscopeShown}">
    <g:expander key="${scopeinfo}">Admin configuration info</g:expander>
</g:unless>
<div class="" id="${scopeinfo}" style="${wdgt.styleVisible(if: outofscopeShown)}">
    <g:if test="${propScope?.isProjectLevel()}">
       <div>configure project:
        <code>
            <g:if test="${mapping && mapping[prop.name]}">
                ${mapping[prop.name].encodeAsHTML()}=${(prop.defaultValue?:'value').encodeAsHTML()}
            </g:if>
            <g:else>
            <g:pluginPropertyProjectScopeKey provider="${pluginName}"
                                             service="${serviceName}"
                                             property="${prop.name}"/>=${(prop.defaultValue ?: 'value').encodeAsHTML()}
             </g:else>
        </code></div>
    </g:if>
    <g:if test="${propScope?.isFrameworkLevel()}">
        <div>configure framework:
        <code>
            <g:pluginPropertyFrameworkScopeKey provider="${pluginName}"
                                               service="${serviceName}"
                                               property="${prop.name}"/>=${(prop.defaultValue ?: 'value').encodeAsHTML()}
        </code></div>
    </g:if>
    <div class="text-info">
        <g:if test="${prop.defaultValue}">
            Default value: <code>${prop.defaultValue.encodeAsHTML()}</code>
        </g:if>
        <g:if test="${prop.selectValues}">
            Allowed values:
            <g:each in="${prop.selectValues}">
                <code>${it.encodeAsHTML()}</code>,
            </g:each>
        </g:if>
    </div>
</div>
