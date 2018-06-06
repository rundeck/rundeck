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

<div class="subnav" style="display:none">
  <ul class="nav" style="" data-old-padding-top="" data-old-padding-bottom="" data-old-overflow="">
    <auth:resourceAllowed action="create" kind="project" context="application">
      <g:if test="${!params.nocreate}">
        <li>
          <g:link controller="framework" action="createProject">
            <span class="sidebar-mini"><i class="fas fa-plus"></i></span>
            <span class="sidebar-normal">
              <g:message code="page.home.new.project.button.label"/>
            </span>
          </g:link>
        </li>
      </g:if>
    </auth:resourceAllowed>
    <g:each var="project" in="${projectSet}">
        <li>
          <g:link controller="menu" action="index" params="${selectParams + [project: project]}">
            <span class="sidebar-mini">${(labels?labels[project]:project).charAt(0)}</span>
            <span class="sidebar-normal">
              <b class="glyphicon glyphicon-task"></b>
              ${labels?labels[project]:project}
            </span>
          </g:link>
        </li>
    </g:each>
  </nav>
</div>
