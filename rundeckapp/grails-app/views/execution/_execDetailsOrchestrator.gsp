<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:set var="orchestrator" value="${scheduledExecution.orchestrator}"/>
<g:set var="desc" value="${orchestratorPlugins?.getDescription(orchestrator.type)}"/>
<g:if test="${desc && desc instanceof Description}">
    <g:expander key="orchestratorplugin${orchestrator.type}">${desc.title.encodeAsHTML()} </g:expander>
    <span class="" id="orchestratorplugin${orchestrator.type}" style="display:none;" title="">
        <g:render template="/framework/renderPluginConfig"
                  model="${[values: orchestrator.configuration, description: desc, hideTitle: true]}"/>
    </span>
</g:if>
<g:elseif test="${!orchestratorPlugins?.getDescription(orchestrator.type)}">
    <span class="warn note"><g:message code="plugin.not.found.0" args="[orchestrator.type?.encodeAsHTML()]" /></span>
</g:elseif>