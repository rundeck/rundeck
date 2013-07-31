<%@ page import="rundeck.ScheduledExecution" %>

<span class="jobInfo" id="jobInfo_${execution.id}">
    <g:if test="${execution}">
        <span class="jobInfoSection">
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
                        <g:if test="${!noimgs}"><img
                                src="${resource(dir: 'images', file: "icon-small-" + fileName + ".png")}"
                                alt="job" style="border:0;"/></g:if>
                    </g:else>
                </span><span class="primary">Execution #${execution.id}</span>
                <g:render template="/scheduledExecution/execStatusText" model="${[execution: execution]}"/>
                <g:if test="${execution.dateCompleted != null}">
                    in <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}"/>
                    <span class="timerel" title="${g.formatDate(date:execution.dateCompleted)} - ${execution.dateCompleted.time}">at <g:relativeDate atDate="${execution.dateCompleted}"/></span>
                </g:if>
            %{--started at <g:relativeDate--}%
            %{--atDate="${execution.dateStarted}"/> by <span--}%
            %{--class="username">${execution.user==session.user?'you': execution.user}</span>--}%
            </g:link>

        </span>
    </g:if>

</span>
