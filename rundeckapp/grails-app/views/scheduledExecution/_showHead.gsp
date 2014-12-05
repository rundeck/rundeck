<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>

<div class="col-sm-12 jobInfoSection">
<g:if test="${scheduledExecution.groupPath}">
    <section>
        <g:set var="parts" value="${scheduledExecution.groupPath.split('/')}"/>
        <g:each in="${parts}" var="part" status="i">
            <g:if test="${i != 0}">/</g:if>
            <g:set var="subgroup" value="${parts[0..i].join('/')}"/>
            <g:link controller="menu"
                    action="jobs"
                    class="secondary"
                    params="${[groupPath: subgroup, project: scheduledExecution.project]}"
                    title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                    absolute="${absolute ? 'true' : 'false'}">
                <g:if test="${i==0}"><g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if></g:if>
                <g:enc>${part}</g:enc></g:link>
        </g:each>
    </section>
</g:if>
<section class="${scheduledExecution.groupPath?'section-space':''}" id="jobInfo_">
    <span class=" h3">
        <g:link controller="scheduledExecution" action="show"
            class="primary"
            params="[project: scheduledExecution.project]"
                id="${scheduledExecution.extid}"
                absolute="${absolute ? 'true' : 'false'}">
            <i class="glyphicon glyphicon-book"></i>
            <g:enc>${scheduledExecution?.jobName}</g:enc></g:link>
    </span>

    <g:if test="${jobActionButtons}">
        <g:render template="/scheduledExecution/jobActionButton" model="[scheduledExecution:scheduledExecution]"/>
    </g:if>
        <g:if test="${scheduledExecution.scheduled && nextExecution}">
            <span class="scheduletime">
                <i class="glyphicon glyphicon-time"></i>
                <g:set var="titleHint"
                       value="${remoteClusterNodeUUID ? g.message(code: "expecting.another.cluster.server.to.run") : ''}"/>
                <span title="${clusterUUID ? g.message(code: "expecting.another.cluster.server.to.run") : ''} at ${enc(attr:g.relativeDate(atDate:nextExecution))}">
                    <g:relativeDate elapsed="${nextExecution}"
                                    untilClass="timeuntil"/>
                </span>
            </span>
        </g:if>
        <g:elseif test="${scheduledExecution.scheduled && !nextExecution}">
            <span class="scheduletime willnotrun has_tooltip" data-toggle="tooltip"
                data-placement="auto left"
                  title="${g.message(code: 'job.schedule.will.never.fire')}">
                <i class="glyphicon glyphicon-time"></i>
                <span class="detail"><g:message code="never" /></span>
            </span>
        </g:elseif>

</section>
<section class="section-space">
        <g:render template="/scheduledExecution/description"
                  model="[description: scheduledExecution.description, textCss: 'h4 text-muted', mode: jobDescriptionMode ?: 'expanded', rkey: g.rkey()]"/>
</section>
</div>
