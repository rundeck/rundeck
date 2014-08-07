<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:if test="${scheduledExecution}">
    <div class="jobInfoSection">
        <span class="jobInfoPart secondary">
            <g:if test="${!groupOnly}">
            <g:link controller="scheduledExecution" action="show"
                    id="${scheduledExecution.extid}"
                    params="[project:scheduledExecution.project]"
                    class="primary" absolute="${absolute ? 'true' :'false'}"
                title="${scheduledExecution?.description}"
            >
                <i class="glyphicon glyphicon-book"></i>
                ${g.enc(html:scheduledExecution?.jobName)}
            </g:link>

            </g:if>
            <g:if test="${scheduledExecution.groupPath && !nameOnly}">
            <span class="jobGroup " >
                <span class="grouplabel h4 ">
                    <g:link controller="menu" action="jobs"
                            class="secondary"
                            params="${[groupPath: scheduledExecution.groupPath, project:scheduledExecution.project]}"
                            title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                            absolute="${absolute ? 'true' : 'false'}"
                            >
                        <g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if>
                        ${g.enc(html:scheduledExecution.groupPath)}
                    </g:link>
                </span>
            </span>
            </g:if>
        </span>
    </div>
</g:if>
