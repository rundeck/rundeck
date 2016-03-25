<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants" %>
<g:set var="scopeinfo" value="${g.rkey()}"/>

<g:if test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in [StringRenderingConstants.DisplayType.PASSWORD, 'PASSWORD']}">
    <g:set var="propValue" value="••••••••"/>
</g:if>
<g:else>
    <g:set var="propValue" value="${prop.defaultValue ?: 'value'}"/>
</g:else>

<g:unless test="${outofscopeHidden}">
<g:unless test="${outofscopeShown}">
    <g:expander key="${scopeinfo}">Admin configuration info</g:expander>
</g:unless>
<div class="" id="${enc(attr:scopeinfo)}" style="${wdgt.styleVisible(if: outofscopeShown)}">
    <g:if test="${!specialConfiguration?.prefix}">
    <g:if test="${propScope?.isProjectLevel()}">
       <div>configure project:
        <code>
            <g:if test="${mapping && mapping[prop.name]}">
                <g:enc>${mapping[prop.name]}=${prop.defaultValue?:'value'}</g:enc>
            </g:if>
            <g:else>
            <g:pluginPropertyProjectScopeKey provider="${pluginName}"
                                             service="${serviceName}"
                                             property="${prop.name}"/>=<g:enc>${propValue}</g:enc>
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
                                                   property="${prop.name}"/>=<g:enc>${propValue}</g:enc>
            </g:else>
        </code>
            </div>
    </g:if>
    </g:if><g:else>
    <div>configuration:
        <code>
            <g:enc>${prefix+prop.name}=${prop.defaultValue ?: 'value'}</g:enc>
        </code>
    </div>
    </g:else>
    <div class="text-info">
        <g:if test="${prop.defaultValue}">
            Default value: <code><g:enc>${propValue}</g:enc></code>
        </g:if>
        <g:if test="${prop.selectValues}">
            Allowed values:
            <g:each in="${prop.selectValues}">
                <code><g:enc>${it}</g:enc></code>,
            </g:each>
        </g:if>
    </div>
</div>
</g:unless>