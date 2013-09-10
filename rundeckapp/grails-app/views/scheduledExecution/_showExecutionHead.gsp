<%@ page import="rundeck.ScheduledExecution" %>

<span class="jobInfo" id="jobInfo_${execution.id}">
    <g:if test="${execution}">
        <span class="h3">
            <g:link
                    controller="execution"
                    action="show"
                    class="primary"
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
                        <g:set var="gicon" value=""/>
                        <g:if test="${execution}">
                            <g:set var="fileName"
                                   value="${execution.status == 'true' ? 'job-ok' : null == execution.dateCompleted ? 'job-running' : execution.cancelled ? 'job-warn' : 'job-error'}"/>
                        </g:if>
                        <g:if test="${!noimgs}">
                            <img
                                src="${resource(dir: 'images', file: "icon-small-" + fileName + ".png")}"
                                alt="job" style="border:0;"/>
                        </g:if>
                        <g:if test="${execution}">
                            <g:set var="gicon" value="${execution.status == 'true' ? 'ok-circle' : null == execution.dateCompleted ? 'play-circle' : execution.cancelled ? 'minus-sign' : 'warning-sign'}"/>
                            <i class="exec-status icon ${!execution.dateCompleted? 'running' : execution.status == 'true' ? 'succeed' : execution.cancelled ? 'warn' : 'fail'}">
                            </i>
                        </g:if>
                    </g:else>
                </span>
                <g:if test="${scheduledExecution}">
                    <span class="primary"><g:message code="scheduledExecution.identity" args="[scheduledExecution.jobName,execution.id]" /></span>
                </g:if>
                <g:else>
                    <span class="primary"><g:message code="execution.identity" args="[execution.id]" /></span>
                </g:else>

            </g:link>

        </span>
        <h4 >
            <small>
                <g:render template="/scheduledExecution/execStatusText" model="${[execution: execution]}"/>
                <g:if test="${execution.dateCompleted != null}">
                    in <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}"/>
                    <span class="timerel"
                          title="${g.formatDate(date: execution.dateCompleted)} - ${execution.dateCompleted.time}">at <g:relativeDate
                            atDate="${execution.dateCompleted}"/></span>
                </g:if>
            </small>
        </h4>
    </g:if>

</span>
