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

<%@ page import="com.dtolabs.rundeck.app.support.ExecutionContext; com.dtolabs.rundeck.server.authorization.AuthConstants; com.dtolabs.rundeck.core.plugins.configuration.Description; rundeck.ScheduledExecution; rundeck.controllers.ScheduledExecutionController" %>
<g:set var="rkey" value="${g.rkey()}"/>
<div class="row" style="margin-top:1em">
<div class="col-sm-12 table-responsive">
<table class="table item_details">
    <g:if test="${execdata!=null && execdata.id && execdata instanceof ScheduledExecution && execdata.scheduled}">
        <tr>
        <td ><g:message code="scheduledExecution.property.crontab.detail.prompt" /></td>
        <td>
            <g:render template="/scheduledExecution/showCrontab" model="${[scheduledExecution:execdata,crontab:crontab]}"/>
        </td>
        </tr>
            <tr>
            <td></td>
            <td class="scheduletime ${remoteClusterNodeUUID?'willnotrun':''}">
                <g:if test="${nextExecution}">
                <g:if test="${remoteClusterNodeUUID}">
                    <i class="glyphicon glyphicon-time"></i>
                      <span title="${enc(attr:remoteClusterNodeUUID)}"><g:message code="scheduled.to.run.on.server.0" args="${[remoteClusterNodeUUID]}"/></span>
                      <g:relativeDate elapsed="${nextExecution}" untilClass="desc"/>
                      <g:message code="job.detail.time.at" /> <span class="desc"><g:enc>${nextExecution}</g:enc></span>
                </g:if>
                <g:else>
                    <i class="glyphicon glyphicon-time"></i>
                        <g:message code="job.detail.next.execution" />
                        <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
                        <g:message code="job.detail.time.at" /> <span class="timeabs"><g:enc>${nextExecution}</g:enc></span>
                </g:else>

                </g:if>

                <g:elseif test="${scheduledExecution.scheduled && !g.executionMode(is:'active',project:scheduledExecution.project)}">
                    <span class="scheduletime disabled has_tooltip" data-toggle="tooltip"
                          data-placement="auto right"
                          title="${g.message(code: 'disabled.schedule.run')}">
                        <i class="glyphicon glyphicon-time"></i>
                        <span class="detail"><g:message code="disabled.schedule.run" /></span>
                    </span>
                </g:elseif>
                <g:elseif test="${scheduledExecution.scheduled && !nextExecution}">
                    <span class="scheduletime willnotrun">
                        <i class="glyphicon glyphicon-time"></i>
                        <span class="detail"><g:message code="job.schedule.will.never.fire" /></span>
                    </span>
                </g:elseif>
            </td>
            </tr>
    </g:if>
    <g:if test="${execdata!=null && execdata.id && execdata instanceof ScheduledExecution && execdata.multipleExecutions}">
        <tr>
        <td ><g:message code="scheduledExecution.property.multipleExecutions.label"/></td>
        <td >
            <g:message code="yes" />
        <g:if test="${execdata!=null && execdata.id && execdata instanceof ScheduledExecution && execdata.maxMultipleExecutions}">
            <code>
                <g:message code="up.to" /> ${execdata.maxMultipleExecutions}
            </code>
        </g:if>
        </td>
        </tr>
    </g:if>

    <g:if test="${execdata instanceof ExecutionContext && execdata?.workflow}">
        <g:unless test="${hideAdhoc}">
        <tr>
            <td><g:message code="steps" /></td>
            <td >
                <g:render template="/execution/execDetailsWorkflow" model="${[strategyPlugins:strategyPlugins, edit: false, workflow:execdata.workflow,context:execdata,noimgs:noimgs,project:execdata.project]}"/>
            </td>
        </tr>
        </g:unless>
        <g:if test="${execdata instanceof ScheduledExecution && execdata.options}">
            <tr>
                <td><g:message code="scheduledExecution.options.prompt" /></td>
                <td >
                    <g:render template="/scheduledExecution/optionsSummary" model="${[options:execdata.options]}"/>
                </td>
            </tr>
        </g:if>
        <g:if test="${execdata.argString && (null==showArgString || showArgString)}">
            <tr>
                <td>
                    <g:message code="scheduledExecution.options.prompt" />
                </td>
                <td >
                    <g:render template="/execution/execArgString" model="[argString: execdata.argString]"/>
                </td>
            </tr>
        </g:if>
    </g:if>
<g:if test="${execdata?.loglevel=='DEBUG'}">
    <tr>
        <td><g:message code="scheduledExecution.property.verbose.logging.prompt" /></td>
        <td >
            <g:message code="badge.Enabled.title" />
        </td>
    </tr>
</g:if>

    <g:if test="${execdata?.doNodedispatch}">
    <tbody>
    <g:if test="${!nomatchednodes}">
            <tr>
                <td><g:message code="Node.plural" /></td>
                <td >
                    <span id="matchednodes_${rkey}" class="matchednodes embed">
                        <span class="text-primary"><g:message code="include.nodes.matching" /></span>
                        <g:set var="filterstring" value="${execdata.asFilter()}"/>
                        <g:set var="jsdata" value="${[filter:filterstring]}"/>
                    <g:set var="varStr" value=""/>
                    <%varStr='${'%>
                    <g:set var="hasVar" value="${filterstring?.contains(varStr)}"/>
                    <g:if test="${hasVar}">
                        <span class="query">
                            <span class="queryvalue text"><g:enc>${filterstring}</g:enc></span>
                        </span>
                    </g:if>
                    <g:else>
                        <g:if test="${!knockout}">
                            <g:embedJSON id="nodeFilterData" data="${jsdata}"/>
                            <g:javascript>
                                jQuery(function(){
                                    var nfilter=loadJsonData('nodeFilterData');
                                    jQuery('#nodeFilterUpdate').click(function(e){
                                        _updateMatchedNodes(nfilter,'matchednodes_${ enc(js: rkey) }','${enc(js:execdata?.project)}',false,{requireRunAuth:true});
                                    });
                                });
                            </g:javascript>
                            <span class="action textbtn  textbtn query " title="${message(code:"display.matching.nodes")}" id="nodeFilterUpdate">
                                <g:render template="/framework/displayNodeFilters" model="${[displayParams:execdata]}"/>
                            </span>
                            <g:link
                                    controller="framework"
                                    action="nodes"
                                    params="[project: params.project ?: request.project, filter: filterstring]"
                                    title="${message(code: 'view.in.nodes.page')}"><g:icon name="arrow-right"/></g:link>
                        </g:if>
                        <g:if test="${knockout}">
                            <span class="ko-wrap">
                                    <span class="action textbtn  textbtn query "
                                          title="${message(code:"display.matching.nodes")}"
                                          data-bind="click: updateMatchedNodes"
                                          >
                                        <g:render template="/framework/displayNodeFilters" model="${[displayParams:execdata]}"/>
                                    </span>
                                <g:link
                                        controller="framework"
                                        action="nodes"
                                        params="[project: params.project ?: request.project, filter: filterstring]"
                                        title="${message(code: 'view.in.nodes.page')}"><g:icon name="arrow-right"/></g:link>
                                <span >
                                    <g:render template="/framework/nodesEmbedKO" model="[showLoading:true,showTruncated:true]"/>
                                </span>
                            </span>
                        </g:if>
                    </g:else>
                    </span>

                    <div>
                        <span class="text-primary text-em">
                            <g:message code="execute.up.to"/>
                            <strong>
                                <g:enc>${execdata?.nodeThreadcount}</g:enc>
                                <g:message code="Node${execdata?.nodeThreadcount==1?'':'.plural'}"/>
                            </strong>
                            <g:message code="at.a.time"/>
                        </span>
                    </div>

                    <div>
                        <span class="text-primary text-em">
                            <g:message code="if.a.node.fails" />:
                            <strong>
                            <g:message
                                    code="scheduledExecution.property.nodeKeepgoing.${!!execdata?.nodeKeepgoing}.description"/>
                            </strong>
                        </span>
                    </div>
                    <div>
                    <span class="text-primary text-em">
                        <g:set value="${null == execdata?.nodeRankOrderAscending || execdata?.nodeRankOrderAscending}"
                               var="isAscending"/>

                        <g:message code="sort.nodes.by"  />
                        <strong><g:enc>${execdata?.nodeRankAttribute?: 'name'}</g:enc></strong>
                        <g:if test="${isAscending}">
                            <g:message code="scheduledExecution.property.nodeRankOrder.ascending.message"/>
                        </g:if>
                        <g:else>
                            <g:message code="scheduledExecution.property.nodeRankOrder.descending.message"/>
                        </g:else>
                    </span>
                    </div>
                    <g:if test="${execdata instanceof ScheduledExecution}">
                    <div>
                        <span class="text-primary text-em">
                            <g:message code="scheduledExecution.property.nodesSelectedByDefault.label" />:
                            <strong>
                                <g:message
                                        code="scheduledExecution.property.nodesSelectedByDefault.${execdata.hasNodesSelectedByDefault()}.description"/>
                            </strong>
                        </span>
                    </div>
                    </g:if>
                </td>

            </tr>
        </g:if>
    </tbody>
    </g:if>
    <g:else>
        <g:if test="${!nomatchednodes}">
        <tbody>
        <tr>
            <td><g:message code="job.detail.node.prompt" /></td>
            <td class="matchednodes embed" id="matchednodes_${rkey}">
                <span class="text-primary"><g:message code="execute.on.the.server.node" /></span>

                <g:if test="${knockout}">
                    <span class="ko-wrap">
                        <span class="btn btn-sm btn-default receiver"
                              title="${message(code:"display.matching.nodes")}"
                              data-bind="click: updateMatchedNodes"
                        >
                            <g:message code="server.node.label" />
                        </span>
                        <g:link
                                controller="framework"
                                action="nodes"
                                params="[project: params.project ?: request.project, filterLocalNodeOnly: true]"
                                title="${message(code: 'view.in.nodes.page')}"><g:icon name="arrow-right"/></g:link>
                        <div >
                            <g:render template="/framework/nodesEmbedKO" model="[showLoading:true,showTruncated:true]"/>
                        </div>
                    </span>
                </g:if>
            </td>
        </tr>
        </tbody>

            <g:if test="${!knockout}">
                <g:javascript>
                jQuery('#serverNodeUpdate').click(function(e){
                   _updateMatchedNodes({},'matchednodes_${enc(js: rkey)}','${enc(js: execdata?.project)}', true, {requireRunAuth:true});
                });
                </g:javascript>
            </g:if>
        </g:if>
    </g:else>
    <g:if test="${execdata?.doNodedispatch}">


    </g:if>
    <g:if test="${execdata instanceof ScheduledExecution && execdata.notifications}">
        <tr>
            <td class="displabel"><g:message code="scheduledExecution.property.notification.prompt" /></td>
            <g:set var="bytrigger" value="${execdata.notifications.groupBy{ it.eventTrigger }}"/>
            <td class="container">
            <g:each var="trigger" in="${bytrigger.keySet().sort()}" status="k">
                <div class="row">
                    <div class="col-sm-12" >
                        <span class=""><g:message code="notification.event.${trigger}"/>:</span>

                        <g:if test="${trigger == ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME}">
                            <div class="">
                                <g:message code="scheduledExecution.property.notifyAvgDurationThreshold.label" default="Threshold"/>:
                                <code class="argstring optvalue"><g:enc>${scheduledExecution.notifyAvgDurationThreshold}</g:enc></code>
                            </div>
                        </g:if>
                <g:if test="${bytrigger[trigger].size()>1}">
                <ul class="overflowx">
                <g:each var="notify" in="${bytrigger[trigger].sort{a,b->a.type.toLowerCase()<=>b.type.toLowerCase()}}" status="i">
                    <li>
                        <g:render template="/execution/execDetailsNotification" model="${[notification: notify]}"/>
                    </li>
                </g:each>
                </ul>
                </g:if>
                <g:else>
                    <div class="overflowx" >
                    <g:render template="/execution/execDetailsNotification" model="${[notification: bytrigger[trigger][0]]}"/>
                    </div>
                </g:else>
                </div>
                </div>
            </g:each>
        </td>
    </tr>
    </g:if>
    <g:if test="${execdata.timeout}">
        <tr>
            <td>
                <g:message code="scheduledExecution.property.timeout.label" />
            </td>
            <td>
                <span title="${message(code:"scheduledExecution.property.timeout.title")}"><g:enc>${execdata.timeout}</g:enc></span>
            </td>
        </tr>
    </g:if>
    <g:if test="${execdata.orchestrator}">
        <tr>
            <td class="displabel"><g:message code="scheduledExecution.property.orchestrator.prompt" /></td>
            <td class="container">
                <g:render template="/execution/execDetailsOrchestrator" model="${[orchestrator: execdata.orchestrator]}"/>
            </td>
    </g:if>
    <g:if test="${execdata instanceof rundeck.ScheduledExecution}">

        <g:if test="${execdata.logOutputThreshold}">
            <tr>
                <td>
                    <g:message code="scheduledExecution.property.logOutputThreshold.label" />
                </td>
                <td>
                    <span title="${message(code:'scheduledExecution.property.logOutputThreshold.description')}">
                        <code><g:enc>${execdata.logOutputThreshold}</g:enc></code>
                    </span>
                </td>
            </tr>
            <tr>
                <td>
                    <g:message code="scheduledExecution.property.logOutputThresholdAction.label" />
                </td>
                <td>
                    <g:enc><g:message code="scheduledExecution.property.logOutputThresholdAction.${execdata.logOutputThresholdAction}.label"/></g:enc>
                    <g:if test="${execdata.logOutputThresholdAction == 'halt'}">
                        <g:enc>${execdata.logOutputThresholdStatus ?: 'failed'}</g:enc>
                    </g:if>
                </td>
            </tr>
        </g:if>
        <tr>
            <td>
                <span class="jobuuid desc"><g:message code="scheduledExecution.property.uuid.prompt" /></span>
            </td>
            <td>
                <span class="jobuuid desc" title="${message(code:"scheduledExecution.property.uuid.description")}"><g:enc>${scheduledExecution.uuid}</g:enc></span>
            </td>
        </tr>
        <tr>
            <td >
                <g:message code="scheduledExecution.property.datecreated.prompt" />
            </td>
            <td >
                <span class="when">
                    <g:relativeDate elapsed="${scheduledExecution.dateCreated}"/>
                </span>
            </td>
        </tr>
    </g:if>

</table>
</div>
</div>
