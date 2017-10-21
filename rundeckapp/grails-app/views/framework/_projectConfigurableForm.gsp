<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" %>
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
<div class="list-group-item">
    <span class="h4 ">
        <g:message code="${titleCode}"/>
    </span>

    <g:if test="${helpCode}"><div class="help-block"><g:message code="${helpCode}"/></div></g:if>

    <div class="form-horizontal">
        <g:each in="${extraConfigSet}" var="configdata">
            <g:set var="configService" value="${configdata.name}"/>
            <g:set var="configurable" value="${configdata.configurable}"/>

            <g:set var="pluginprefix" value="${configdata.get('prefix')}"/>
            <g:set var="categoryProps" value="${configurable.projectConfigProperties.findAll{configdata.configurable.categories[it.name]==category}}"/>
            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                    properties         : categoryProps,
                    report             : configdata.get('report'),
                    prefix             : pluginprefix,
                    values             : configdata.get('values') ?: [:],
                    fieldnamePrefix    : pluginprefix,
                    origfieldnamePrefix: 'orig.' + pluginprefix,
                    allowedScope       : PropertyScope.Project
            ]}"/>

        </g:each>
    </div>
</div>