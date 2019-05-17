%{--
  - Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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


<g:set var="parts" value="${groupPath.split('/')}"/>
<g:each in="${parts}" var="part" status="i">
    <g:if test="${i != 0}">/</g:if>
    <g:set var="subgroup" value="${parts[0..i].join('/')}"/>
    <g:if test="${groupBreadcrumbMode != 'static'}">
        <g:link controller="menu"
                action="jobs"
                class="${linkCss?:''}"
                params="${[groupPath: subgroup, project: project]}"
                title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                absolute="${absolute ? 'true' : 'false'}">
            <g:if test="${i == 0}"><g:if test="${!noimgs}"><b
                    class="glyphicon glyphicon-folder-close"></b></g:if></g:if>
            <g:enc>${part}</g:enc></g:link>
    </g:if>
    <g:if test="${groupBreadcrumbMode == 'static'}">
        <g:if test="${i == 0}"><g:if test="${!noimgs}"><b
                class="glyphicon glyphicon-folder-close"></b></g:if></g:if>
        <g:enc>${part}</g:enc>
    </g:if>
</g:each>
