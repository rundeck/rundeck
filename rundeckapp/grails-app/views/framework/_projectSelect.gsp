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
    <a class="dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        ${project && selectItemTitle ? selectItemTitle : emptyTitle}
        <span class="caret"></span>
        <span class="sr-only">Toggle Dropdown</span>
    </a>

    <ul class="dropdown-menu " role="menu" aria-labelledby="dLabel">
<auth:resourceAllowed action="create" kind="project" context="application">
        <g:if test="${!params.nocreate}">
            <li>
                <g:link controller="framework" action="createProject">
                    <g:message code="page.home.new.project.button.label"/>
                    <b class="glyphicon glyphicon-plus"></b>
                </g:link>
            </li>
            <li class="divider">
            </li>

        </g:if>
</auth:resourceAllowed>
        <g:each var="project" in="${projectSet}">
            <li>
                <g:link controller="menu" action="index" params="${selectParams + [project: project]}" >
                    <i class="glyphicon glyphicon-tasks"></i>
                    <g:enc>${project}</g:enc>
                </g:link>
            </li>
        </g:each>
    </ul>
