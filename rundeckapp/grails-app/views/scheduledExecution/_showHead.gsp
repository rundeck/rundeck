<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>
    <g:set var="execInfo" value="${scheduledExecution?scheduledExecution:execution}"/>
    <g:set var="jobPrimary" value="${scheduledExecution && !execution ? 'primary':'secondary'}"/>
    <g:set var="execPrimary" value="${execution ? 'primary':''}"/>

    <div class="jobInfo" id="jobInfo_${execution ? execution.id : ''}">
        <g:if test="${execution}">
            <div class="jobInfoSection">
                <g:link
                        controller="execution"
                        action="show"
                        id="${execution.id}"
                        absolute="${absolute ? 'true' : 'false'}"
                        params="${followparams?.findAll { it.value }}">
                <span class="jobIcon ${execution?.status == 'true' ? 'jobok' : execution?.cancelled ? 'jobwarn' : 'joberror'}">
                    <g:if test="${iconName}">
                        <g:if test="${!noimgs}"><img src="${resource(dir: 'images', file: iconName + '.png')}" alt="job"
                                                     style="border:0;"/></g:if>
                    </g:if>
                    <g:else>
                        <g:set var="fileName" value="job"/>
                        <g:if test="${execution}">
                            <g:set var="fileName"
                                   value="${execution.status == 'true' ? 'job-ok' : null == execution.dateCompleted ? 'job-running' : execution.cancelled ? 'job-warn' : 'job-error'}"/>
                        </g:if>
                        <g:if test="${!noimgs}"><img src="${resource(dir: 'images', file: "icon-med-" + fileName + ".png")}"
                                                     alt="job" style="border:0;"/></g:if>
                    </g:else>
                </span><span class="${execPrimary}">Execution #${execution.id}</span>
                    <g:render template="/scheduledExecution/execStatusText" model="${[execution: execution]}"/>

                    %{--started at <g:relativeDate--}%
                        %{--atDate="${execution.dateStarted}"/> by <span--}%
                        %{--class="username">${execution.user==session.user?'you': execution.user}</span>--}%
                </g:link>


            </div>
        </g:if>
    <g:if test="${scheduledExecution}">
        <div class="jobInfoSection">
            <span class="jobInfoPart ${jobPrimary}">
            <g:if test="${!runPage}">
                <span style="vertical-align:middle;margin-right: 10px;" class="floatl toolbar small">

                    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_RUN)}">
                        <g:link controller="scheduledExecution" action="execute" id="${scheduledExecution.extid}"
                                class="icon button floatl"
                                onclick="if(typeof(loadExec)=='function'){loadExec(${scheduledExecution.id});return false;}"><img
                                src="${resource(dir: 'images', file:  'icon-run.png')}"
                                title="Run ${g.message(code: 'domain.ScheduledExecution.title')}&hellip;" alt="run"
                                width="32" height="32"/></g:link>
                    </g:if>

                </span>
                </g:if>
                <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}"
                        class=" ${execution?.status == 'true' ? 'jobok' : null == execution?.dateCompleted ? 'jobrunning' : execution?.cancelled ? 'jobwarn' : 'joberror'}" absolute="${absolute ? 'true' :'false'}">
                <span class="jobName">${scheduledExecution?.jobName.encodeAsHTML()}</span></g:link>

                <g:if test="${execInfo.groupPath}">
                <span class="jobGroup">
                    <span class="grouplabel">
                        <g:link controller="menu" action="jobs" params="${[groupPath:execInfo.groupPath]}" title="${'View '+g.message(code:'domain.ScheduledExecution.title')+'s in this group'}"  absolute="${absolute?'true':'false'}">
                            <g:if test="${!noimgs}"><img src="${resource(dir:'images',file:'icon-small-folder.png')}" width="16px" height="15px" alt=""/></g:if>
                            ${execInfo.groupPath.encodeAsHTML()}
                        </g:link>
                    </span>
                </span>
                </g:if>

            </span>
        </div>
        <div class="jobInfoSection">
            <span class="jobdesc">${execInfo?.description?.encodeAsHTML()}</span>
        </div>
    </g:if>
    <g:if test="${execInfo instanceof ScheduledExecution && execInfo?.uuid && !execution}">
        <div class="jobInfoSection"><span class="jobuuid desc" title="UUID for this job">UUID: ${execInfo?.uuid.encodeAsHTML()}</span></div>
    %{--<div><span class="jobid desc" title="internal ID for this job">ID: ${execInfo?.id}</span></div>--}%
    </g:if>
    </div>
