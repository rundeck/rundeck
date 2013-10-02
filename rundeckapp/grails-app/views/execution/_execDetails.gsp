<%@ page import="com.dtolabs.rundeck.app.support.ExecutionContext; com.dtolabs.rundeck.server.authorization.AuthConstants; com.dtolabs.rundeck.core.plugins.configuration.Description; rundeck.ScheduledExecution" %>
<g:set var="rkey" value="${g.rkey()}"/>
<div class="row">
<div class="col-sm-6 pull-right">
<div class=" pull-right">
<g:if test="${showEdit && execdata != null && execdata.id && execdata instanceof ScheduledExecution && auth.jobAllowedTest(job: execdata, action: AuthConstants.ACTION_UPDATE)}">
    <g:link controller="scheduledExecution" title="Edit or Delete this Job" action="edit"
            id="${execdata.extid}" class="btn btn-info btn-sm">
        <b class="glyphicon glyphicon-edit"></b>
        edit job</g:link>

<g:if test="${scheduledExecution && auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_READ])}">
    <g:if test="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE)}">
        <g:link controller="scheduledExecution" title="Duplicate Job" action="copy"
                id="${scheduledExecution.extid}" class="btn btn-success btn-sm">
            <b class="glyphicon glyphicon-plus"></b>
            duplicate to a new job
        </g:link>
    </g:if>
    <div class="btn-group">
        <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
            Download Definition
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu" role="menu">
            <li><g:link controller="scheduledExecution" title="Download Job definition in  XML"
                        action="show"
                        id="${scheduledExecution.extid}.xml">
                <b class="glyphicon glyphicon-file"></b>
                xml format
            </g:link>
            </li>
            <li>
                <g:link controller="scheduledExecution" title="Download Job definition in YAML"
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
<div class="col-sm-6">
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
            <td>Workflow:</td>
            <td >
                <g:render template="/execution/execDetailsWorkflow" model="${[workflow:execdata.workflow,context:execdata,noimgs:noimgs,project:execdata.project]}"/>
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
                    <span class="argString">${execdata?.argString.encodeAsHTML()}</span>
                </td>
            </tr>
        </g:if>
    </g:if>
    <tr>
        <td>Log level:</td>
        <td >
            ${execdata?.loglevel}
        </td>
    </tr>
    <g:set var="NODE_FILTERS" value="${['','Name','Tags','OsName','OsFamily','OsArch','OsVersion']}"/>
    <g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>

    <g:if test="${execdata?.doNodedispatch}">
    <tbody>
    <g:if test="${!nomatchednodes}">
            <tr>
                <td>Node Filters:</td>
                <td id="matchednodes_${rkey}" class="matchednodes embed" >
                    <g:set var="jsdata" value="${execdata.properties.findAll{it.key==~/^node(In|Ex)clude.*$/ &&it.value}}"/>
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
            <td id="matchednodes_${rkey}" class="matchednodes embed" >
                <span class="action textbtn depress2 receiver"  title="Display matching nodes" onclick="_updateMatchedNodes({},'matchednodes_${rkey}','${execdata?.project}', true, {requireRunAuth:true})">Show Matches</span>
                
            </td>
        </tr>
        </tbody>
        </g:if>
    </g:else>
    <g:if test="${execdata?.doNodedispatch}">

        <tr>
            <td>Thread Count:</td>
            <td>${execdata?.nodeThreadcount}</td>
            <td class="displabel">Keep going:</td>
            <td>${execdata?.nodeKeepgoing}</td>
        </tr>
        <tr>
            <td><g:message code="scheduledExecution.property.nodeRankAttribute.label"/>:</td>
            <td>${execdata?.nodeRankAttribute? execdata?.nodeRankAttribute.encodeAsHTML() : 'Node Name'}</td>
            <td class="displabel"><g:message code="scheduledExecution.property.nodeRankOrder.label"/>:</td>
            <td><g:message code="scheduledExecution.property.nodeRankOrder.${null==execdata?.nodeRankOrderAscending || execdata?.nodeRankOrderAscending?'ascending':'descending'}.label"/></td>
        </tr>
    </g:if>
    <g:if test="${execdata instanceof ScheduledExecution && execdata.notifications}">
        <tr>
            <td class="displabel">Notification:</td>
            <g:set var="bytrigger" value="${execdata.notifications.groupBy{ it.eventTrigger }}"/>
            <g:each var="trigger" in="${bytrigger.keySet().sort()}" status="k">
                <td>
                    <span class=""><g:message code="notification.event.${trigger}"/>:</span>
                <g:if test="${bytrigger[trigger].size()>1}">
                <ul>
                <g:each var="notify" in="${bytrigger[trigger].sort{a,b->a.type.toLowerCase()<=>b.type.toLowerCase()}}" status="i">
                    <li>
                        <g:render template="/execution/execDetailsNotification" model="${[notification: notify]}"/>
                    </li>
                </g:each>
                </ul>
                </g:if>
                <g:else>
                    <g:render template="/execution/execDetailsNotification" model="${[notification: bytrigger[trigger][0]]}"/>
                </g:else>
                </td>
            </g:each>
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
    </g:if>
<g:if test="${scheduledExecution && auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_READ])}">
    <tr>
        <td>

        </td>
        <td>
            <span class="desc">



            </span>
        </td>
    </tr>
</g:if>

    
</table>
</div>
</div>
