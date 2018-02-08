%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 1/30/18
  Time: 11:13 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="triggers"/>
    <title><g:appTitle/> - Trigger List</title>
</head>

<body>
<g:render template="/common/messages"/>

<div class="row " id="triggerListPage">

    <div class="col-sm-10 col-sm-offset-1">
        <h3>Triggers
            <g:link action="create" class="btn btn-success btn-sm pull-right" params="[project: project]">
                <i class="glyphicon glyphicon-plus"></i>
                New Trigger &hellip;
            </g:link>
        </h3>


        <table cellpadding="0" cellspacing="0" width="100%" class="table table-bordered table-condensed">
            <tr>
                <th class="table-header">Name</th>
                <th class="table-header">Description</th>
                <th class="table-header">Action</th>
            </tr>
            <g:each in="${triggers}" var="trigger" status="index">
                <tr>
                    <td>
                        <g:if test="${trigger.enabled}">
                            <g:icon name="check" css="text-success"/>
                        </g:if>
                        <g:else>
                            <g:icon name="unchecked" css="text-muted"/>
                        </g:else>
                        <g:link action="show"
                                params="[project: project, id: trigger.uuid]">
                            ${trigger.name ?: trigger.uuid}
                        </g:link>
                    </td>
                    <td>${trigger.description}</td>
                    <td>
                        <g:link action="delete" class="btn btn-danger-hollow btn-sm"
                                params="[project: project, id: trigger.uuid]">
                            <i class="glyphicon glyphicon-remove"></i>
                            Delete
                        </g:link>
                        <g:link action="edit" class="btn btn-info-hollow btn-sm"
                                params="[project: project, id: trigger.uuid]">
                            <i class="glyphicon glyphicon-pencil"></i>
                            Edit
                        </g:link>
                    </td>
                </tr>
            </g:each>
        </table>
    </div>

</div>
</body>
</html>
