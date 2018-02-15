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

<%@ page import="com.dtolabs.rundeck.plugins.ServiceNameConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="triggers"/>
    <asset:javascript src="task/show.js"/>
    <title><g:appTitle/> - Task - ${task.name ?: task.uuid}</title>

</head>

<body>

<g:render template="/common/messages"/>

<div class="row " id="triggerShowPage">

    <div class="col-sm-3">
        <section>
            <g:link
                    class="h3"
                    action="show"
                    params="[id: task.uuid, project: project]">
                <g:icon name="send"/>
                ${task.name ?: task.uuid}</g:link>


            <g:render template="/scheduledExecution/description"
                      model="[
                              description: task.description,
                              textCss    : 'h4 text-muted',
                              mode       : 'expanded',
                              rkey       : g.rkey()
                      ]"/>
        </section>

        <section class="section-space">
            <g:if test="${task.enabled}">
                <g:icon name="check" css="text-success"/> Enabled
            </g:if>
            <g:else>
                <g:icon name="unchecked" css="text-muted"/> Disabled
            </g:else>
        </section>
        <section class="section-space">
            <g:link action="delete" class="btn btn-danger-hollow btn-xs"
                    params="[project: project, id: task.uuid]">
                <i class="glyphicon glyphicon-remove"></i>
                Delete
            </g:link>
            <g:link action="test" class="btn btn-default-hollow btn-xs"
                    params="[project: project, id: task.uuid]">
                <i class="glyphicon glyphicon-question-sign"></i>
                Test
            </g:link>
            <g:link action="edit" class="btn btn-info-hollow btn-xs"
                    params="[project: project, id: task.uuid]">
                <i class="glyphicon glyphicon-pencil"></i>
                Edit
            </g:link>
        </section>
        <section class="section-space">
            <span class="text-muted">${task.uuid}</span>
        </section>
        <g:if test="${task.userData}">
            <section class="section-space">

                <g:basicData data="${task.userData}" fields="${task.userData?.keySet()?.sort()}"/>
            </section>
        </g:if>
    </div>

    <div class="col-sm-6">
        <div class="list-group">
            <div class="list-group-item">
                <h4 class="list-group-item-heading">

                    Task
                </h4>

                <g:render template="/framework/renderPluginConfig"
                          model="${[serviceName: com.dtolabs.rundeck.plugins.ServiceNameConstants.TaskTrigger,
                                    values     : task.triggerConfig,
                                    description: triggerPlugins[task.triggerType].description,
                                    hideTitle  : false]}"/>

            </div>

            <div class="list-group-item">
                <h4 class="list-group-item-heading">Action</h4>
                <g:render template="/framework/renderPluginConfig"
                          model="${[serviceName: com.dtolabs.rundeck.plugins.ServiceNameConstants.TaskAction,
                                    values     : task.actionConfig,
                                    description: actionPlugins[task.actionType].description,
                                    hideTitle  : false]}"/>
            </div>
        </div>
    </div>

    <div class="col-sm-3">

        <g:basicData data="${task}"
                     classes="table-bordered table-condensed"
                     labelClasses="text-muted text-right"
                     fieldTitle="${[
                             'userCreated' : 'Created By',
                             'userModified': 'Modified By'
                     ]}"
                     fields="${[
                             'dateCreated',
                             'lastUpdated',
                             'userCreated',
                             'userModified'
                     ]}"/>

    </div>
</div>

<div class="row row-space">
    <div class="col-sm-12">
        <g:if test="${task.events}">
            <table class="table table-bordered table-condensed">
                <g:set var="renderedEvents" value="${[]}"/>

                <g:each in="${task.events.sort { a, b -> b.dateCreated <=> a.dateCreated }}" var="event">
                    <g:if test="${event.associatedEvent}">
                        %{ renderedEvents << event.associatedEvent.id }%
                    </g:if>
                    <g:if test="${event.associatedEvent || !(event.id in renderedEvents)}">
                        <tr>
                            <td>
                                <g:if test="${event.eventType == 'result'}">
                                    <span class="text-success">
                                        <g:icon name="ok-sign"/>
                                    </span>
                                </g:if>
                                <g:if test="${event.eventType == 'error'}">
                                    <span class="text-danger">
                                        <g:icon name="flag"/>
                                    </span>
                                </g:if>
                                <g:if test="${event.eventType == 'fired'}">
                                    <span class="text-info">
                                        <g:icon name="play-circle"/>
                                    </span>
                                </g:if>
                                at
                                <g:if test="${event.associatedEvent}">
                                    <g:relativeDate atDate="${event.associatedEvent.dateCreated}"/>
                                </g:if>
                                <g:else>
                                    <g:relativeDate atDate="${event.dateCreated}"/>
                                </g:else>
                            </td>
                            <td>
                                <g:if test="${event.eventType == 'result' && event.eventDataMap}">
                                %{--<g:basicData data="${event.eventDataMap}"--}%
                                %{--classes="table-condensed table-bordered"--}%
                                %{--fields="${event.eventDataMap.keySet().sort()}"/>--}%
                                </g:if>
                                <g:if test="${event.eventType == 'error' && event.eventDataMap?.error}">
                                    <span class="text-warning">
                                        ${event.eventDataMap.error}
                                    </span>
                                </g:if>
                                <g:if test="${event.associatedType == 'Execution' && event.associatedId}">
                                    <g:link controller="execution" action="show"
                                            params="${[project: project, id: event.associatedId]}">
                                        <g:icon name="circle-arrow-right"/>
                                        Execution #${event.associatedId}
                                    </g:link>
                                </g:if>
                            </td>
                        </tr>
                    </g:if>
                </g:each>
            </table>
        </g:if>
    </div>
</div>

<div>

</div>
</body>
</html>