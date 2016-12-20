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

    <g:set var="authProjectExport" value="${auth.resourceAllowedTest(
            context: 'application',
            type: 'project',
            action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT],
            any: true,
            name: scheduledExecution.project
    )}"/>
    <g:set var="authProjectImport" value="${auth.resourceAllowedTest(
            context: 'application',
            type: 'project',
            action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT],
            any: true,
            name: scheduledExecution.project
    )}"/>

    <g:set var="exportStatus" value="${authProjectExport && scmExportEnabled ? scmExportStatus?.get(scheduledExecution.extid) :null}"/>
    <g:set var="importStatus" value="${authProjectImport && scmImportEnabled ? scmImportStatus?.get(scheduledExecution.extid):null}"/>
        <g:render template="/scm/statusBadge"
                  model="[
                          showClean:true,
                          linkClean:true,
                          exportStatus: exportStatus?.synchState?.toString(),
                          importStatus: importStatus?.synchState?.toString(),
                          text  : '',
                          notext: false,
                          link: true,
                          integration:'export',
                          job:scheduledExecution,
                          exportCommit  : exportStatus?.commit,
                          importCommit  : importStatus?.commit,
                  ]"/>



    <g:if test="${jobActionButtons}">
        <g:render template="/scheduledExecution/jobActionButton" model="[scheduledExecution:scheduledExecution]"/>
    </g:if>
        <g:if test="${scheduledExecution.scheduled && nextExecution}">
            <span class="scheduletime">
                <g:if test="${serverNodeUUID && !remoteClusterNodeUUID}">
                    <span class="text-warning has_tooltip" title="${message(code:"scheduledExecution.scheduled.cluster.orphan.title")}"
                          data-placement="right"
                    >
                        <g:icon name="alert"/>
                    </span>
                </g:if>
                <g:else>
                    <g:icon name="time"/>
                </g:else>
                <g:set var="titleHint"
                       value="${remoteClusterNodeUUID ? g.message(code: "scheduled.to.run.on.server.0") : ''}"/>
                <span title="${remoteClusterNodeUUID ? g.message(code: "scheduled.to.run.on.server.0", args:[remoteClusterNodeUUID]) : ''} at ${enc(attr:g.relativeDate(atDate:nextExecution))}">
                    <g:relativeDate elapsed="${nextExecution}"
                                    untilClass="timeuntil"/>
                </span>
                <g:if test="${remoteClusterNodeUUID}">
                    on
                    <span data-server-uuid="${remoteClusterNodeUUID}" data-server-name="${remoteClusterNodeUUID}" class="rundeck-server-uuid text-muted">
                    </span>
                </g:if>
            </span>
        </g:if>
        <g:elseif test="${scheduledExecution.scheduled && !g.executionMode(is:'active')}">
            <span class="scheduletime disabled has_tooltip" data-toggle="tooltip"
                data-placement="auto left"
                  title="${g.message(code: 'disabled.schedule.run')}">
                <i class="glyphicon glyphicon-time"></i>
                <span class="detail"><g:message code="disabled.schedule.run" /></span>
            </span>
        </g:elseif>
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
