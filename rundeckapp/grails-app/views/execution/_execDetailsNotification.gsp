<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:set var="ukey" value="${g.rkey()}"/>
<g:if test="${notification.type == 'url'}">
    <g:expander key="webhook${ukey}"><g:message code="notification.webhook.label" /> </g:expander>
    <span class="webhooklink note" id="webhook${ukey}" style="display:none;"
          title="URLs: ${enc(attr:notification.content)}"><g:enc>${notification.content}</g:enc></span>
</g:if>
<g:elseif test="${notification.type == 'email'}">
    <g:message code="notification.email.display" args="[enc(html:notification.mailConfiguration().recipients).toString()]" />
</g:elseif>
<g:else>
%{--plugin display--}%
    <g:set var="desc" value="${notificationPlugins?.get(notification.type)?.description}"/>
    <g:if test="${desc && desc instanceof Description}">

        <g:expander key="notificationplugin${ukey}"><g:enc>${desc.title}</g:enc> </g:expander>
        <span class="" id="notificationplugin${ukey}" style="display:none;" title="">
            <g:render template="/framework/renderPluginConfig"
                      model="${[values: notification.configuration, description: desc, hideTitle: true]}"/>
        </span>
    </g:if>
    <g:elseif test="${!notificationPlugins?.get(notification.type)}">
        <span class="warn note"><g:message code="plugin.not.found.0" args="[enc(html:notification.type).toString()]" /></span>
    </g:elseif>
</g:else>
