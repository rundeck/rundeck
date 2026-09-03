%{--
  - Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

%{--
  - NB: jobComponent.get('properties'), not jobComponent.properties. jobComponent is the Map the
  - jobComponentSectionProperties tag builds, [name:.., properties:..], and Groovy 5 resolves the
  - `properties` meta-property (Object.getProperties()) ahead of the map entry:
  -     groovy 4: [name:'x', properties:[1,2]].properties -> [1, 2]   (truthy)
  -     groovy 5: [name:'x', properties:[1,2]].properties -> [:]      (falsy)
  - so the g:if below was false and every custom job component section rendered an empty panel --
  - the tab appeared, because jobComponentSections reads inputLocation instead, but the body did not.
  - Same collision the UtilityTagLib attrs.get('properties') comments describe, on a Map this time.
  --}%
<g:each in="${g.jobComponentSectionProperties(section:sectionName,jobComponents:jobComponents)}" var="jobComponent">
    <g:if test="${jobComponent.get('properties')}">
        <g:set var="prefix" value="${g.jobComponentFieldPrefix(name:jobComponent.name)}"/>
        <g:set var="pluginConfig" value="${jobComponentValues?.get(jobComponent.name)}"/>
        <g:set var="validation" value="${jobComponentValidation?.get(jobComponent.name)}"/>
        <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                properties:jobComponent.get('properties'),
                report: validation,
                prefix:prefix,
                values:pluginConfig?:[:],
                fieldnamePrefix:prefix,
                origfieldnamePrefix:'orig.' + prefix,
                allowedScope:com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance,
                messagesType:g.jobComponentMessagesType(name:jobComponent.name)
        ]}"/>
    </g:if>
</g:each>
