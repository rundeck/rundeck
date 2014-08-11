<g:set var="scopeinfo" value="${g.rkey()}"/>
<g:unless test="${outofscopeShown}">
    <g:expander key="${scopeinfo}">Admin configuration info</g:expander>
</g:unless>
<div class="" id="${enc(attr:scopeinfo)}" style="${wdgt.styleVisible(if: outofscopeShown)}">
    <g:if test="${propScope?.isProjectLevel()}">
       <div>configure project:
        <code>
            <g:if test="${mapping && mapping[prop.name]}">
                <g:enc>${mapping[prop.name]}=${prop.defaultValue?:'value'}</g:enc>
            </g:if>
            <g:else>
            <g:pluginPropertyProjectScopeKey provider="${pluginName}"
                                             service="${serviceName}"
                                             property="${prop.name}"/>=<g:enc>${prop.defaultValue ?: 'value'}</g:enc>
             </g:else>
        </code></div>
    </g:if>
    <g:if test="${propScope?.isFrameworkLevel() && (frameworkMapping&& frameworkMapping[prop.name] || !hideMissingFrameworkMapping )}">

        <div>configure framework:

        <code>
            <g:if test="${frameworkMapping && frameworkMapping[prop.name]}">
                <g:enc>${frameworkMapping[prop.name]}=${prop.defaultValue ?: 'value'}</g:enc>
            </g:if>
            <g:else>
                <g:pluginPropertyFrameworkScopeKey provider="${pluginName}"
                                                   service="${serviceName}"
                                                   property="${prop.name}"/>=<g:enc>${prop.defaultValue ?: 'value'}</g:enc>
            </g:else>
        </code>
            </div>
    </g:if>
    <div class="text-info">
        <g:if test="${prop.defaultValue}">
            Default value: <code><g:enc>${prop.defaultValue}</g:enc></code>
        </g:if>
        <g:if test="${prop.selectValues}">
            Allowed values:
            <g:each in="${prop.selectValues}">
                <code><g:enc>${it}</g:enc></code>,
            </g:each>
        </g:if>
    </div>
</div>
