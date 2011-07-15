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
                    <span class="title"><g:message code="domain.ScheduledExecution.title"/> <g:message code="status.label.${it.status}"/>
                    </span>
                    <g:if test="${(it.status == 'cancel') }">
                        by: ${it.abortedByUser}
                    </g:if>
                    <g:if test="${it.dateCompleted}">
                        <span class="date">
                            <g:formatDate date="${it?.dateCompleted}" formatName="jobslist.date.format"/>
                        </span>
                    </g:if>
                    <div class="rptitem">
                        <g:if test="${it.jcJobId}">
                            <g:set var="foundJob" value="${ ScheduledExecution.get(it.jcJobId)}"/>
                            <g:if test="${foundJob}">
                                <g:link controller="scheduledExecution" action="show" id="${foundJob.extid}"><g:message code="domain.ScheduledExecution.title"/> Detail &raquo;</g:link>
                            </g:if>
                            <g:else>
                                <span class="warning note">(<g:message code="domain.ScheduledExecution.title"/> ID ${it.jcJobId} has been deleted)</span>
                            </g:else>
                        </g:if>
                    </div>
                    <g:if test="${it.jcExecId }">
                        <div class="rptitem">
                            <g:link controller="execution" action="show" id="${it.jcExecId}" title="View execution output" >View Output &raquo;</g:link>
                        </div>
                    </g:if>
                </g:if>
                <g:else>
                    <span class="title">
                        ${(it.reportId?it.reportId:it.title).encodeAsHTML()}
                    <g:message code="status.label.${it.status}"/></span>

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
                            <g:set var="jobtitle" value="${it.title}"/>
                        </g:else>
                        ${jobtitle.encodeAsHTML()}

                    </span>
                    </div>
                </g:else>
                <g:if test="${it instanceof ExecReport}">
                    <g:if test="${it?.node=~/^\d+\/\d+\/\d+$/}">
                        <g:set var="vals" value="${it.node.split('/')}"/>
                        <g:if test="${vals.length>2 && vals[2]!='0'}">
                            <g:set var="suc" value="${Integer.parseInt(vals[0])}"/>
                            <g:set var="fail" value="${Integer.parseInt(vals[1])}"/>
                            <g:set var="tot" value="${Integer.parseInt(vals[2])}"/>
                            <g:set var="sucperc" value="${Math.floor((suc/tot)*100)}"/>
                            <g:set var="perc" value="${Math.floor((fail/tot)*100)}"/>
                        </g:if>
                        <g:else>
                            <g:set var="perc" value="${0}"/>
                        </g:else>
                        <span class="title"><g:message code="events.history.title.Completed"/>:</span>
                        <div class="rptitem">
                            <g:message code="events.history.title.PercentFailed"/>: ${perc}%
                        
                            <div class="rptitem">
                                <div>
                                    <g:message code="status.label.fail"/>: ${vals[1]}
                                </div>
                                <div>
                                    <g:message code="status.label.succeed"/>: ${vals[0]}
                                </div>
                            </div>
                        </div>
                    </g:if>
                </g:if>


               %{--<g:if test="${it.message}">--}%
               %{--<span class="title">Message:</span>--}%
                   %{--<div class="rptmsgx">--}%
                   %{--<div class="rptmessage msgtext">--}%
                    %{--${it.message.encodeAsHTML()}--}%
                   %{--</div>--}%
                   %{--</div>--}%
               %{--</g:if>--}%

               </div>
               </div>
           </td>
        </tr>