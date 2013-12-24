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
<ul class="nav nav-tabs activity_links">
    <li>
        <g:link controller="reports" action="index" class="activity_link"
                title="Activity within the last day"
                params="${linkParams+[recentFilter: '1d']}">
            <i class="glyphicon glyphicon-time"></i>
            last day
        </g:link>
    </li>

    <li>
        <g:link controller="reports" action="index" class="activity_link"
                title="Activity within the last week"
                params="${linkParams+[ recentFilter: '1w']}">
            <i class="glyphicon glyphicon-time"></i>
            last week
        </g:link>
    </li>

    <li>
        <g:link controller="reports" action="index" class="activity_link"
                title="Failed executions"
                params="${linkParams+[ statFilter: 'fail']}">
            <i class="glyphicon glyphicon-minus-sign"></i>
            failed
        </g:link>
    </li>

    <g:if test="${!execution || execution.user != session.user}">
        <li>
            <g:link controller="reports" action="index" class="activity_link"
                    title="Executions by you"
                    params="${linkParams+[ userFilter: session.user]}">
                <i class="glyphicon glyphicon-user"></i>
                by you
            </g:link>
        </li>
    </g:if>

    <g:if test="${execution}">
        <li>
            <g:link controller="reports" action="index" class="activity_link"
                    title="Executions by ${execution.user.encodeAsHTML()}"
                    params="${linkParams+[ userFilter: execution.user]}">
                <i class="glyphicon glyphicon-user"></i>
                by <g:username user="${execution.user}"/>
            </g:link>
        </li>
    </g:if>
    <li>
        <g:link controller="reports" action="index" class="activity_link"
                title="All activity for this job"
                params="${linkParams}">
            <i class="glyphicon glyphicon-list"></i>
            all
        </g:link>
    </li>

</ul>
<g:if test="${knockoutBinding}">

<div data-bind="visible: selected()"  class="panel panel-default panel-tab-content" style="display: none;">
    <table class=" table table-hover table-condensed "
           style="width:100%; display: none"
           data-bind="visible: reports().length > 0, foreach: reports ">
        <tr class="link"
            data-bind="css: { 'succeed': status()=='succeed', 'fail': status()=='fail' } "
            onclick="$(this).down('a._defaultAction').click();"
            >
            <td style="width:12px;" class="eventicon">
                <i class="exec-status icon"
                   data-bind="css: { 'succeed': status()=='succeed', 'fail': status()=='fail', 'warn': status()=='cancel' } "
                ></i>
            </td>
            <td class="eventtitle" data-bind="css: { job: jcJobId(), adhoc: !jcJobId() }, attr: { colspan: jcJobId()?'1':'2' }">
                <a href="#" data-bind="text: '#'+jcExecId(), attr: { href: showExecutionLink() }" class="_defaultAction"></a>
                <span data-bind="text: statusText()"></span>
            </td>
            <td class="eventargs" data-bind="if: jcJobId() && execution()">
                <span data-bind="text: execution().argString"></span>
            </td>
            <td style="white-space:nowrap" class="right date">
                <span data-bind="if: dateCompleted()">
                    <span class="timeabs" data-bind="text: endTimeFormat('${g.message(code:'jobslist.date.format.ko')}')">

                    </span>
                    <span title="">
                        in <span class="duration" data-bind="text: durationHumanize()"></span>
                    </span>
                </span>
            </td>

            <td class="  user" style="white-space: nowrap">
                <em>by</em>
                <span data-bind="text: author"></span>
            </td>

            <td style="white-space:nowrap;text-align:right;"
                class="  nodecount "
                data-bind="css: { fail: nodeFailCount() > 0 , ok: nodeFailCount() < 1 } "
            >
                <span data-bind="if: nodeFailCount() > 0">
                    <span data-bind="text: nodeFailCount()"></span> failed
                </span>
                <span data-bind="if: nodeFailCount() < 1 ">
                    <span data-bind="text: nodeSucceedCount()"></span> ok
                </span>
            </td>
            <td class=" outputlink">
                <span data-bind="if: jcExecId()">
                    <a href="#" data-bind="attr: { href: showExecutionLink() }"
                       class="_defaultAction">Show &raquo;</a>
                </span>
            </td>
        </tr>
    </table>


    <div data-bind="visible: selected() && reports().length < 1 " class="panel-body" style="display: none;">
        <div class="">
        <span class="text-muted">No matching activity found</span>
        </div>
    </div>

    <div data-bind="visible: selected()" class="panel-footer" style="display: none">
        <span data-bind="if: max() > 0" class="text-info">
            showing
            <span data-bind="text: count() + ' of ' + total()"></span>
        </span>
        <a href="#" class="" data-bind="attr: { href: href() } ">
                Show matching activity…
            <i class="glyphicon glyphicon-circle-arrow-right"></i>
        </a>

    </div>
</div>

</g:if>
