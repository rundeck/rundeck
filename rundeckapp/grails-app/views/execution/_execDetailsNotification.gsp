%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

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
                      model="${[serviceName:'Notification',values: notification.configuration, description: desc, hideTitle: true]}"/>
        </span>
    </g:if>
    <g:elseif test="${!notificationPlugins?.get(notification.type)}">
        <span class="warn note"><g:message code="plugin.not.found.0" args="[enc(html:notification.type).toString()]" /></span>
    </g:elseif>
</g:else>
