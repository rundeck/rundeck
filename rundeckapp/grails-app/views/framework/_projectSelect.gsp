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

<div class="dropdown" style="line-height: 3.5em;margin-left: .8em;">
  <a data-toggle="dropdown" class="dropdown-toggle" id="userLabel" style="color: #65605a;">
    <span class="fa-stack fa-2x" style="font-size:.75em;">
      <i class="fas fa-circle fa-stack-2x"></i>
      <i class="fas fa-caret-down fa-stack-1x fa-inverse"></i>
    </span>

    <!-- ${project && selectItemTitle ? selectItemTitle : emptyTitle} -->
  </a>
  <ul class="dropdown-menu">
    <li class="dropdown-header">
      <g:message code="Project.plural"/>
    </li>
    <auth:resourceAllowed action="create" kind="project" context="application">
      <g:if test="${!params.nocreate}">
        <li>
          <g:link controller="framework" action="createProject">
            <g:message code="page.home.new.project.button.label"/>
            <b class="glyphicon glyphicon-plus"></b>
          </g:link>
        </li>
      </g:if>
    </auth:resourceAllowed>
    <g:each var="project" in="${projectSet}">
        <li>
          <g:link controller="menu" action="index" params="${selectParams + [project: project]}">
            <b class="glyphicon glyphicon-task"></b>
            ${labels?labels[project]:project}
          </g:link>
        </li>
    </g:each>
  </ul>
</div>
