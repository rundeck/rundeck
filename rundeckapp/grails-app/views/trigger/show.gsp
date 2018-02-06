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
  Time: 12:16 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:appTitle/> - Trigger - ${trigger.name ?: trigger.uuid}</title>

</head>

<body>

<g:render template="/common/messages"/>

<div class="row " id="triggerShowPage">

    <div class="col-sm-3">
        <g:link
                class="h3"
                action="show"
                params="[id: trigger.uuid, project: project]">
            <g:icon name="send"/>
            ${trigger.name}
        </g:link>
    </div>

    <div class="col-sm-9">

        <g:basicData data="${trigger}"
                     classes="table-bordered table-condensed"
                     labelClasses="text-muted text-right"
                     fieldTitle="${[
                             'userCreated' : 'Created By',
                             'userModified': 'Modified By'
                     ]}"
                     fields="${[
                             'enabled',
                             'description',
                             'uuid',
                             'conditionType',
                             'actionType',
                             'userData',
                             'dateCreated',
                             'lastUpdated',
                             'userCreated',
                             'userModified'
                     ]}"/>

    </div>
</div>

<div class="row row-space">
    <div class="col-sm-12">
        <g:if test="${trigger.events}">
            <g:basicTable data="${trigger.events.sort { a, b -> b.dateCreated <=> a.dateCreated }}"
                          columns="['dateCreated', 'eventType', 'eventDataMap']"
                          classes="table-bordered table-condensed"/>
        </g:if>
    </div>
</div>

<div>
    <g:link action="delete" class="btn btn-danger-hollow btn-sm"
            params="[project: project, id: trigger.uuid]">
        <i class="glyphicon glyphicon-remove"></i>
        Delete
    </g:link>
    <g:link action="test" class="btn btn-default-hollow btn-sm"
            params="[project: project, id: trigger.uuid]">
        <i class="glyphicon glyphicon-question-sign"></i>
        Test
    </g:link>
    <g:link action="edit" class="btn btn-info-hollow btn-sm"
            params="[project: project, id: trigger.uuid]">
        <i class="glyphicon glyphicon-pencil"></i>
        Edit
    </g:link>
</div>
</body>
</html>