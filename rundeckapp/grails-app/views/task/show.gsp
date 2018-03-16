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

<%@ page import="org.rundeck.core.tasks.TaskPluginTypes; com.dtolabs.rundeck.plugins.ServiceNameConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="triggers"/>
    <asset:javascript src="task/show.js"/>
    <g:jsMessages code="job.not.found.with.id.0"/>
    <title><g:appTitle/> - <g:message code="Task.domain.title" /> - ${task.name ?: task.uuid}</title>
    <g:javascript>"use strict";
    jQuery(function () {
        initKoBind(null, {});
    });


    </g:javascript>
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
                <g:icon name="check" css="text-success"/> <g:message code="badge.Enabled.title" />
            </g:if>
            <g:else>
                <g:icon name="unchecked" css="text-muted"/> <g:message code="badge.Disabled.title" />
            </g:else>
        </section>
        <section class="section-space">
            <g:link action="delete" class="btn btn-danger-hollow btn-xs"
                    params="[project: project, id: task.uuid]">
                <i class="glyphicon glyphicon-remove"></i>
                <g:message code="button.action.Delete" />
            </g:link>
            <g:link action="test" class="btn btn-default-hollow btn-xs"
                    params="[project: project, id: task.uuid]">
                <i class="glyphicon glyphicon-question-sign"></i>
                Test
            </g:link>
            <g:link action="edit" class="btn btn-info-hollow btn-xs"
                    params="[project: project, id: task.uuid]">
                <i class="glyphicon glyphicon-pencil"></i>
                <g:message code="button.Edit.label" />
            </g:link>
        </section>
        <section class="section-space">
            <span class="text-muted">${task.uuid}</span>
        </section>
        <g:if test="${task.userData}">
            <section class="section-space">

                <g:basicData data="${task.userData}"
                             fields="${task.userData?.keySet()?.sort()}"
                             classes="table-bordered table-condensed"/>
            </section>
        </g:if>
    </div>

    <div class="col-sm-6">
        <div class="list-group">
            <div class="list-group-item">
                <h4 class="list-group-item-heading">

                    <g:message code="task.trigger.display.title" />
                </h4>

                <g:render template="/framework/renderPluginConfig"
                          model="${[serviceName   : org.rundeck.core.tasks.TaskPluginTypes.TaskTrigger,
                                    showPluginIcon: true,
                                    values        : task.triggerConfig,
                                    description   : triggerPlugins[task.triggerType].description,
                                    hideTitle     : false]}"/>

            </div>

            <div class="list-group-item">
                <h4 class="list-group-item-heading">
                    <g:message code="Task.domain.conditions.title" />
                </h4>

                <g:each in="${task.conditionList}" var="condition">
                    <g:render template="/framework/renderPluginConfig"
                              model="${[serviceName   : org.rundeck.core.tasks.TaskPluginTypes.TaskCondition,
                                        showPluginIcon: true,
                                        values        : condition.config,
                                        description   : conditionPlugins[condition.type].description,
                                        hideTitle     : false]}"/>
                </g:each>

            </div>

            <div class="list-group-item">
                <h4 class="list-group-item-heading"><g:message code="task.action.display.title" /></h4>
                <g:render template="/framework/renderPluginConfig"
                          model="${[serviceName   : org.rundeck.core.tasks.TaskPluginTypes.TaskAction,
                                    showPluginIcon: true,
                                    values        : task.actionConfig,
                                    description   : actionPlugins[task.actionType].description,
                                    hideTitle     : false]}"/>
            </div>
        </div>
    </div>

    <div class="col-sm-3">

        <g:basicData data="${task}"
                     classes="table-bordered table-condensed"
                     labelClasses="text-muted text-right"
                     fieldTitle="${[
                             'userCreated' : message(code:"Task.created.by.title"),
                             'userModified': message(code:"Task.modified.by.title")
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
                                <g:if test="${event.eventType =~ '^error'}">
                                    <span class="text-danger">
                                        <g:icon name="flag"/>
                                    </span>
                                </g:if>
                                <g:if test="${event.eventType =~ '^warn'}">
                                    <span class="text-warning">
                                        <g:icon name="warning-sign"/>
                                    </span>
                                </g:if>
                                <g:if test="${event.eventType == 'fired'}">
                                    <span class="text-info">
                                        <g:icon name="play-circle"/>
                                    </span>
                                </g:if>
                                <g:if test="${event.eventType == 'condition:notmet'}">
                                    <span class="text-muted">
                                        <g:icon name="exclamation-sign"/>
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
                                <g:if test="${event.eventType =~ '^(error|warn)' && event.eventDataMap?.error}">
                                    <span class="text-warning">
                                        <g:autoLink>${event.eventDataMap.error}</g:autoLink>
                                    </span>
                                </g:if>
                                <g:if test="${event.eventType == 'condition:notmet' && event.eventDataMap?.message}">
                                    <span class="text-muted">
                                        <g:autoLink>${event.eventDataMap.message}</g:autoLink>
                                    </span>
                                    <g:if test="${event.eventDataMap?.condition?.data}">
                                        <span data-ko-controller="UIToggle">

                                            <span class="btn btn-link btn-muted"
                                                  data-bind="click: toggle">
                                                <g:message code="more"/>
                                                <i class="glyphicon"
                                                   data-bind="css: {'glyphicon-chevron-right':!value(),'glyphicon-chevron-down':value }"></i>
                                            </span>

                                            <div data-bind="if: value">

                                                <g:basicData data="${event.eventDataMap?.condition?.data}"
                                                             classes="table-bordered table-compact"/>

                                            </div>
                                        </span>

                                    </g:if>
                                </g:if>
                                <g:if test="${event.associatedType == 'Execution' && event.associatedId}">
                                    <g:link controller="execution" action="show"
                                            params="${[project: project, id: event.associatedId]}">
                                        <g:icon name="circle-arrow-right"/>
                                        <g:message code="domain.Execution.title" /> #${event.associatedId}
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