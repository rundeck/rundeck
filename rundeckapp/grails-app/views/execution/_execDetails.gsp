<%@ page import="com.dtolabs.rundeck.app.support.ExecutionContext; com.dtolabs.rundeck.server.authorization.AuthConstants; com.dtolabs.rundeck.core.plugins.configuration.Description; rundeck.ScheduledExecution" %>
<g:set var="rkey" value="${g.rkey()}"/>
<div class="row" >
<div class="col-sm-12">
<table class="simpleForm execdetails">
    <g:if test="${execdata!=null && execdata.id && execdata instanceof ScheduledExecution && execdata.scheduled}">
        <tr>
        <td >Schedule:</td>
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
                      <span title="${enc(attr:remoteClusterNodeUUID)}"><g:message code="expecting.another.cluster.server.to.run"/></span>
                      <g:relativeDate elapsed="${nextExecution}" untilClass="desc"/>
                      at <span class="desc"><g:enc>${nextExecution}</g:enc></span>
                </g:if>
                <g:else>
                    <i class="glyphicon glyphicon-time"></i>
                        Next execution
                        <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
                        at <span class="timeabs"><g:enc>${nextExecution}</g:enc></span>
                </g:else>

                </g:if>
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
        </td>
        </tr>
    </g:if>

    <g:if test="${execdata instanceof ExecutionContext && execdata?.workflow}">
        <g:unless test="${hideAdhoc}">
        <tr>
            <td><g:message code="steps" />:</td>
            <td >
                <g:render template="/execution/execDetailsWorkflow" model="${[edit: false, workflow:execdata.workflow,context:execdata,noimgs:noimgs,project:execdata.project]}"/>
            </td>
        </tr>
        </g:unless>
        <g:if test="${execdata instanceof ScheduledExecution && execdata.options}">
            <tr>
                <td>Options:</td>
                <td >
                    <g:render template="/scheduledExecution/optionsSummary" model="${[options:execdata.options]}"/>
                </td>
            </tr>
        </g:if>
        <g:if test="${execdata.argString && (null==showArgString || showArgString)}">
            <tr>
                <td>
                    Options:
                </td>
                <td >
                    <g:render template="/execution/execArgString" model="[argString: execdata.argString]"/>
                </td>
            </tr>
        </g:if>
    </g:if>
<g:if test="${execdata?.loglevel=='DEBUG'}">
    <tr>
        <td>Verbose Logging:</td>
        <td >
            Enabled
        </td>
    </tr>
</g:if>
    <g:set var="NODE_FILTERS" value="${['','Name','Tags','OsName','OsFamily','OsArch','OsVersion']}"/>
    <g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>

    <g:if test="${execdata?.doNodedispatch}">
    <tbody>
    <g:if test="${!nomatchednodes}">
            <tr>
                <td><g:message code="Node.plural" />:</td>
                <td >
                    <span id="matchednodes_${rkey}" class="matchednodes embed">
                        <span class="text-muted"><g:message code="include.nodes.matching" /></span>
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
                        <g:embedJSON id="nodeFilterData" data="${jsdata}"/>
                        <g:javascript>
                            jQuery(function(){
                                var nfilter=loadJsonData('nodeFilterData');
                                jQuery('#nodeFilterUpdate').click(function(e){
                                    _updateMatchedNodes(nfilter,'matchednodes_${ enc(js: rkey) }','${enc(js:execdata?.project)}',false,{requireRunAuth:true});
                                });
                            });
                        </g:javascript>
                        <span class="action textbtn  textbtn query " title="Display matching nodes" id="nodeFilterUpdate">
                            <g:render template="/framework/displayNodeFilters" model="${[displayParams:execdata]}"/>
                        </span>
                    </g:else>
                    </span>

                    <div>
                        <span class="text-muted text-em">
                            <g:message code="execute.up.to"/>
                            <strong>
                                <g:enc>${execdata?.nodeThreadcount}</g:enc>
                                <g:message code="Node${execdata?.nodeThreadcount==1?'':'.plural'}"/>
                            </strong>
                            <g:message code="at.a.time"/>
                        </span>
                    </div>

                    <div>
                        <span class="text-muted text-em">
                            <g:message code="if.a.node.fails" />:
                            <strong>
                            <g:message
                                    code="scheduledExecution.property.nodeKeepgoing.${!!execdata?.nodeKeepgoing}.description"/>
                            </strong>
                        </span>
                    </div>
                    <div>
                    <span class="text-muted text-em">
                        <g:set value="${null == execdata?.nodeRankOrderAscending || execdata?.nodeRankOrderAscending}"
                               var="isAscending"/>

                        <g:message code="sort.nodes.by"  />
                        <strong><g:enc>${execdata?.nodeRankAttribute?: 'name'}</g:enc></strong>
                        in
                        <strong>
                            <g:message code="${isAscending ? 'ascending' : 'descending'}"/>
                        </strong>
                        order.
                    </span>
                    </div>

                </td>

            </tr>
        </g:if>
    </tbody>
    </g:if>
    <g:else>
        <g:if test="${!nomatchednodes}">
        <tbody>
        <tr>
            <td>Node:</td>
            <td class="matchednodes embed" id="matchednodes_${rkey}">
                <span class="text-muted"><g:message code="execute.on.the.server.node" /></span>
                <span class="btn btn-sm btn-default receiver"  title="Display matching nodes" id="serverNodeUpdate">Server Node</span>
            </td>
        </tr>
        </tbody>
            <g:javascript>
            jQuery('#serverNodeUpdate').click(function(e){
               _updateMatchedNodes({},'matchednodes_${enc(js: rkey)}','${enc(js: execdata?.project)}', true, {requireRunAuth:true});
            });
            </g:javascript>
        </g:if>
    </g:else>
    <g:if test="${execdata?.doNodedispatch}">


    </g:if>
    <g:if test="${execdata instanceof ScheduledExecution && execdata.notifications}">
        <tr>
            <td class="displabel">Notification:</td>
            <g:set var="bytrigger" value="${execdata.notifications.groupBy{ it.eventTrigger }}"/>
            <td class="container">
            <g:each var="trigger" in="${bytrigger.keySet().sort()}" status="k">
                <div class="row">
                    <div class="col-sm-12" >
                        <span class=""><g:message code="notification.event.${trigger}"/>:</span>
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
                <span title="Timeout duration"><g:enc>${execdata.timeout}</g:enc></span>
            </td>
        </tr>
    </g:if>
    <g:if test="${execdata instanceof rundeck.ScheduledExecution}">
        <tr>
            <td>
                <span class="jobuuid desc">UUID:</span>
            </td>
            <td>
                <span class="jobuuid desc" title="UUID for this job"><g:enc>${scheduledExecution.uuid}</g:enc></span>
            </td>
        </tr>
        <tr>
            <td >
                Created:
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
