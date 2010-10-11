<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 13, 2008
  Time: 3:13:18 PM
  To change this template use File | Settings | File Templates.
--%>
<g:if test="${!subkey}">
    <g:set var="subkey" value="subsect"/>
</g:if>
<g:if test="${!index}">
    <g:set var="index" value="${0}"/>
</g:if>
<g:if test="${!colspan}">
    <g:set var="colspan" value="${1}"/>
</g:if>
<tr class=" subsection" id="${subkey}_${it.id}" style="display:none">
           <td colspan="${colspan}">
               <div class="left">
                   <div class="right">


                <g:if test="${it.jcJobId || it.jcExecId}">
                    <span class="title"><g:message code="domain.ScheduledExecution.title"/> <g:message code="status.label.${it.status}"/>.
                    </span>
                    <g:if test="${it.dateCompleted}">
                        <span class="date">
                            <g:formatDate date="${it?.dateCompleted}" formatName="jobslist.date.format"/>
                        </span>
                    </g:if>
                    <div class="rptitem">
                    <span class="jobname ${it?.status != 'succeed' ? 'fail' : ''}">




                                <g:if test="${it.jcJobId}">
                                    <g:set var="jobtitle" value="${it.title}"/>
                                </g:if>
                                <g:elseif test="${it?.adhocExecution}">
                                    <g:if test="${it?.adhocExecution && it.adhocScript}">
                                        <g:set var="jobtitle" value="${it.adhocScript}"/>
                                    </g:if>
                                    <g:elseif test="${it?.adhocExecution}">
                                        <g:set var="jobtitle" value="run"/>
                                    </g:elseif>
                                </g:elseif>
                                <g:else>
                                    <g:set var="jobtitle" value="${it.ctxCommand}"/>
                                </g:else>
                            <g:if test="${it.jcJobId}">
                                <g:link controller="scheduledExecution" action="show" id="${it.jcJobId}"><g:message code="domain.ScheduledExecution.title"/> Detail &raquo;</g:link>
                            </g:if>

                    </span>
                    </div>
                    <g:if test="${it.jcExecId }">
                        <div class="rptitem">
                            <g:link controller="execution" action="show" id="${it.jcExecId}" title="View execution output" >View Output &raquo;</g:link>
                        </div>
                    </g:if>
                </g:if>
                <g:else>
                    <span class="title">
                        <g:if test="${it?.ctxCommand}">
                            ${it?.ctxCommand} Command
                        </g:if>
                        <g:else>
                            ${it.title.encodeAsHTML()}
                        </g:else>
                    <g:message code="status.label.${it.status}"/>.</span>

                    <g:if test="${it.dateCompleted}">
                        <span class="date">
                            <g:formatDate date="${it?.dateCompleted}" formatName="jobslist.date.format"/>
                        </span>
                    </g:if>
                    <div class="rptitem">
                    <span class="cmdname ${it?.status != 'succeed' ? 'fail' : ''}">
                        <g:if test="${it.adhocScript}">
                            <g:set var="jobtitle" value="${it.adhocScript}"/>
                        </g:if>
                        <g:else >
                            <g:set var="jobtitle" value="run"/>
                        </g:else>
                        ${jobtitle.encodeAsHTML()}

                    </span>
                    </div>
                </g:else>

               <g:if test="${it.message}">
               <span class="title">Message:</span>
                   <div class="rptmsgx">
                   <div class="rptmessage msgtext">
                    ${it.message.encodeAsHTML()}
                   </div>
                   </div>
               </g:if>

               <g:if test="${it.reportId}">
                   <span class="info note">Report ID: ${it.reportId}</span>
               </g:if>
               </div>
               </div>
           </td>
        </tr>