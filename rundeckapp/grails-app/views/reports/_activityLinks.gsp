%{--
  Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%

<g:set var="linkParams" value="${filter?:[projFilter:project?:session.project]}"/>
<g:if test="${scheduledExecution}">
    <g:set var="linkParams" value="${[jobIdFilter: scheduledExecution.id, projFilter: scheduledExecution.project]}"/>
</g:if>
<ul class="nav nav-links">


    <li>
        <g:link controller="reports" action="index"
                title="Activity within the last day"
                params="${linkParams+[recentFilter: '1d']}">
            <i class="glyphicon glyphicon-time"></i>
            last day
        </g:link>
    </li>

    <li>
        <g:link controller="reports" action="index"
                title="Activity within the last week"
                params="${linkParams+[ recentFilter: '1w']}">
            <i class="glyphicon glyphicon-time"></i>
            last week
        </g:link>
    </li>

    <li>
        <g:link controller="reports" action="index"
                title="Failed executions"
                params="${linkParams+[ statFilter: 'fail']}">
            <i class="glyphicon glyphicon-minus-sign"></i>
            failed
        </g:link>
    </li>

    <g:if test="${!execution || execution.user != session.user}">
        <li>
            <g:link controller="reports" action="index"
                    title="Executions by you"
                    params="${linkParams+[ userFilter: session.user]}">
                <i class="glyphicon glyphicon-user"></i>
                by you
            </g:link>
        </li>
    </g:if>

    <g:if test="${execution}">
        <li>
            <g:link controller="reports" action="index"
                    title="Executions by ${execution.user.encodeAsHTML()}"
                    params="${linkParams+[ userFilter: execution.user]}">
                <i class="glyphicon glyphicon-user"></i>
                by <g:username user="${execution.user}"/>
            </g:link>
        </li>
    </g:if>
    <li>
        <g:link controller="reports" action="index"
                title="All activity for this job"
                params="${linkParams}">
            <i class="glyphicon glyphicon-list"></i>
            all
        </g:link>
    </li>

</ul>
