%{--
  - Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope " %>
<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 5/19/17
  Time: 2:27 PM
--%>

<g:set var="serviceName" value="LogFilter"/>
<div class="container">
    <g:hiddenField name="type" value="${type}"/>

    <g:if test="${newfiltertype}">
        <g:hiddenField name="newfiltertype" value="${newfiltertype}"/>
    </g:if>
    <div>
        <span class="h4">
            <stepplugin:pluginIcon service="${serviceName}"
                                   name="${description.name}"
                                   width="16px"
                                   height="16px">
                <i class="rdicon icon-small plugin"></i>
            </stepplugin:pluginIcon>
            <stepplugin:message
                service="${serviceName}"
                name="${description.name}"
                code="plugin.title"
                default="${description.title}"/></span>
        <span class="help-block">
            <g:render template="/scheduledExecution/description"
                      model="[description: stepplugin.messageText(
                              service: serviceName,
                              name: description.name,
                              code: 'plugin.description',
                              default: description.description
                      ),
                              textCss    : '',
                              mode       : 'collapsed',
                              moreText   : message(code: 'more.information', default: 'More Information'),
                              rkey       : g.rkey()]"/>
        </span>
    </div>



    <g:set var="pluginprefix" value="pluginConfig."/>
    <div>
        <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                extraInputCss      : '',
                service            : serviceName,
                provider           : description.name,
                properties         : description.properties,
                report             : report,
                prefix             : pluginprefix,
                values             : config,
                fieldnamePrefix    : pluginprefix,
                origfieldnamePrefix: 'orig.' + pluginprefix,
                allowedScope       : PropertyScope.Instance
        ]}"/>

    </div>
</div>