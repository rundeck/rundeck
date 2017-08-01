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

<g:set var="projectSet" value="${projects.sort()}"/>
<g:set var="selectParams" value="${selectParams?:[:]}"/>
<bs:dropdownToggle>
        ${project && selectItemTitle ? selectItemTitle : emptyTitle}
</bs:dropdownToggle>
<bs:dropdown labelId="dLabel">
    <bs:menuitem headerCode="Project.plural"/>

<auth:resourceAllowed action="create" kind="project" context="application">
        <g:if test="${!params.nocreate}">
            <bs:menuitem controller="framework" action="createProject">
                <g:message code="page.home.new.project.button.label"/>
                <b class="glyphicon glyphicon-plus"></b>
            </bs:menuitem>
            <bs:menuitem/>
        </g:if>
</auth:resourceAllowed>
        <g:each var="project" in="${projectSet}">
            <bs:menuitem controller="menu" action="index" params="${selectParams + [project: project]}"
                         icon="tasks">
                ${project}
            </bs:menuitem>
        </g:each>
</bs:dropdown>
