<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>

<div class="jobHead col-sm-12" >
<div class="jobInfo " id="jobInfo_">
    <span class="jobInfoSection h3">
        <g:link controller="scheduledExecution" action="show"
            class="primary"
            params="[project: scheduledExecution.project]"
                id="${scheduledExecution.extid}"
                absolute="${absolute ? 'true' : 'false'}">
            <i class="glyphicon glyphicon-book"></i>
            <g:enc>${scheduledExecution?.jobName}</g:enc></g:link>
    </span>
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

        <span class="h4  jobGroup ">
            <g:if test="${scheduledExecution.groupPath}">
                <g:link controller="menu" action="jobs"
                        class="secondary"
                        params="${[groupPath: scheduledExecution.groupPath, project:scheduledExecution.project]}"
                        title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                        absolute="${absolute ? 'true' : 'false'}">
                    <g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if>
                    <g:enc>${scheduledExecution.groupPath}</g:enc>
                </g:link>
            </g:if>
        </span>
    <g:if test="${jobActionButtons}">
        <div class="btn-group">
            <button type="button" class="btn btn-sm btn-link btn-default dropdown-toggle" data-toggle="dropdown"
                    aria-expanded="false">
                <i class="glyphicon glyphicon-list"></i>
                Action
                <span class="caret"></span>
            </button>
            <ul class="dropdown-menu" role="menu">
            <g:if test="${ auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
                <li>
                <g:link controller="scheduledExecution" title="Edit or Delete this Job"
                        action="edit"
                        params="[project: scheduledExecution.project]"
                        id="${scheduledExecution.extid}" class="">
                    <i class="glyphicon glyphicon-edit"></i>
                    <g:message code="scheduledExecution.action.edit.button.label"/>
                </g:link>
                </li>
                <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_READ])}">
                    <g:if test="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE, project: scheduledExecution.project)}">
                        <li>
                        <g:link controller="scheduledExecution" title="Duplicate Job" action="copy"
                                params="[project: scheduledExecution.project]"
                                id="${scheduledExecution.extid}" class="">
                            <i class="glyphicon glyphicon-plus"></i>
                            <g:message code="scheduledExecution.action.duplicate.button.label"/>
                        </g:link>
                        </li>
                    </g:if>
                    <li class="divider"></li>
                    <li><g:link controller="scheduledExecution"
                                title="${g.message(code:'scheduledExecution.action.downloadformat.button.label',args:['XML'])}"
                                params="[project: scheduledExecution.project]"
                                action="show"
                                id="${scheduledExecution.extid}.xml">
                        <b class="glyphicon glyphicon-file"></b>
                        <g:message code="scheduledExecution.action.downloadformat.button.label" args="['XML']"/>
                    </g:link>
                    </li>
                    <li>
                        <g:link controller="scheduledExecution"
                                title="${g.message(code: 'scheduledExecution.action.downloadformat.button.label', args: ['YAML'])}"
                                params="[project: scheduledExecution.project]"
                                action="show"
                                id="${scheduledExecution.extid}.yaml">
                            <b class="glyphicon glyphicon-file"></b>
                            <g:message code="scheduledExecution.action.downloadformat.button.label"
                                       args="['YAML']"/>
                        </g:link>
                    </li>
                </g:if>
            </g:if>
            </ul>
        </div>
    </g:if>
    <div class="jobInfoSection">
        <g:render template="/scheduledExecution/description" model="[description:scheduledExecution.description,textCss:'h4 text-muted', mode: jobDescriptionMode?:'expanded', rkey: g.rkey()]"/>
    </div>

</div>
</div>
