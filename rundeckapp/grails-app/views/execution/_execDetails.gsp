<%@ page import="com.dtolabs.rundeck.app.support.ExecutionContext; com.dtolabs.rundeck.server.authorization.AuthConstants; com.dtolabs.rundeck.core.plugins.configuration.Description; rundeck.ScheduledExecution" %>
<g:set var="rkey" value="${g.rkey()}"/>
<div class="row" >
<div class="col-sm-4 pull-right">
<div class=" pull-right btn-group-vertical">
<g:if test="${showEdit && execdata != null && execdata.id && execdata instanceof ScheduledExecution && auth.jobAllowedTest(job: execdata, action: AuthConstants.ACTION_UPDATE)}">
    <g:link controller="scheduledExecution" title="Edit or Delete this Job" action="edit"
        params="[project:execdata.project]"
            id="${execdata.extid}" class="btn btn-info ">
        <g:message code="scheduledExecution.action.edit.button.label" />
        <i class="glyphicon glyphicon-edit"></i>
    </g:link>

<g:if test="${scheduledExecution && auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_READ])}">
    <g:if test="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE,project:scheduledExecution.project)}">
        <g:link controller="scheduledExecution" title="Duplicate Job" action="copy"
                params="[project: execdata.project]"
                id="${scheduledExecution.extid}" class="btn btn-success ">
            <g:message code="scheduledExecution.action.duplicate.button.label" />
            <i class="glyphicon glyphicon-plus"></i>
        </g:link>
    </g:if>
    <div class="btn-group">
        <button type="button" class="btn btn-default  dropdown-toggle" data-toggle="dropdown">
            <g:message code="scheduledExecution.action.download.button.label" />
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu" role="menu">
            <li><g:link controller="scheduledExecution" title="Download Job definition in  XML"
                        params="[project: execdata.project]"
                        action="show"
                        id="${scheduledExecution.extid}.xml">
                <b class="glyphicon glyphicon-file"></b>
                xml format
            </g:link>
            </li>
            <li>
                <g:link controller="scheduledExecution" title="Download Job definition in YAML"
                        params="[project: execdata.project]"
                        action="show"
                        id="${scheduledExecution.extid}.yaml">
                    <b class="glyphicon glyphicon-file"></b>
                    yaml format
                </g:link>
            </li>
        </ul>
    </div>
    </g:if>
</g:if>
</div>
</div>
<div class="col-sm-8">
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
                      <span title="${remoteClusterNodeUUID}"><g:message code="expecting.another.cluster.server.to.run"/></span>
                      <g:relativeDate elapsed="${nextExecution}" untilClass="desc"/>
                      at <span class="desc">${nextExecution}</span>
                </g:if>
                <g:else>
                    <i class="glyphicon glyphicon-time"></i>
                        Next execution
                        <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
                        at <span class="timeabs">${nextExecution}</span>
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
                        <g:set var="jsdata" value="${execdata.properties.findAll{it.key==~/^(filter|node(In|Ex)clude.*)$/ &&it.value}}"/>
                    <g:set var="varStr" value=""/>
                    <%varStr='${'%>
                    <g:set var="hasVar" value="${jsdata.find{it.value.toString()?.contains(varStr)}}"/>
                    <g:if test="${hasVar}">
                        <span class="query">
                        <g:render template="/framework/displayNodeFilters" model="${[displayParams: execdata]}"/>
                        </span>
                    </g:if>
                    <g:else>
                    <g:javascript>
                        _g_nodeFilterData['${rkey}']=${jsdata.encodeAsJSON()};
                    </g:javascript>
                    <span class="action textbtn  textbtn query " title="Display matching nodes" onclick="_updateMatchedNodes(_g_nodeFilterData['${rkey}'],'matchednodes_${rkey}','${execdata?.project}',false,{requireRunAuth:true});">
                        <g:render template="/framework/displayNodeFilters" model="${[displayParams:execdata]}"/>
                    </span>
                    </g:else>
                    </span>

                    <div>
                        <span class="text-muted text-em">
                            <g:message code="execute.up.to"/>
                            <strong>
                                ${execdata?.nodeThreadcount}
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
                        <strong>${execdata?.nodeRankAttribute ? execdata?.nodeRankAttribute?.encodeAsHTML() : 'name'}</strong>
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
                <span class="btn btn-sm btn-default receiver"  title="Display matching nodes" onclick="_updateMatchedNodes({},'matchednodes_${rkey}','${execdata?.project}', true, {requireRunAuth:true})">Server Node</span>
            </td>
        </tr>
        </tbody>
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
                <span title="Timeout duration">${execdata.timeout.encodeAsHTML()}</span>
            </td>
        </tr>
    </g:if>
    <g:if test="${execdata instanceof rundeck.ScheduledExecution}">
        <tr>
            <td>
                <span class="jobuuid desc">UUID:</span>
            </td>
            <td>
                <span class="jobuuid desc" title="UUID for this job">${scheduledExecution.uuid.encodeAsHTML()}</span>
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
