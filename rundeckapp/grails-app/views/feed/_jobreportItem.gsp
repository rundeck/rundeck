<g:if test="${report}">
    <p>
        <g:if test="${report.status=='succeed'}">
            <img src="${resource(dir: 'images', file: 'icon-tiny-ok.png')}" alt="" width="12px" height="12px"/>
        </g:if>
        <g:else>
            <img src="${resource(dir: 'images', file: 'icon-tiny-warn.png')}" alt="" width="12px" height="12px"/>
        </g:else>
        <g:message code="main.app.name"/> <g:message code="domain.ScheduledExecution.title"/> <g:message code="status.label.${report.status}"/>

        ( <g:formatDate date="${report.dateCompleted}" formatName="jobslist.date.format"/>) 
    </p>
    <p>

        <g:if test="${report.jcJobId}">
            <g:set var="jobtitle" value="${report.title}"/>
        </g:if>
        <g:elseif test="${report.adhocExecution}">
            <g:if test="${report.adhocExecution && report.adhocScript}">
                <g:set var="jobtitle" value="${report.adhocScript}"/>
            </g:if>
            <g:elseif test="${report.adhocExecution}">
                <g:set var="jobtitle" value="run"/>
            </g:elseif>
        </g:elseif>
        <g:else>
            <g:set var="jobtitle" value="${report.ctxCommand}"/>
        </g:else>
        <g:link title="View ${g.message(code:'domain.ScheduledExecution.title')} in ${g.message(code:'main.app.name')}" controller="scheduledExecution" action="show" id="${report.jcJobId}">${jobtitle.encodeAsHTML()}</g:link>
    </p>
    <p>
        <g:link controller="execution" action="show" id="${report.jcExecId}" title="View execution output" >View Output &raquo;</g:link>
    </p>
    <p>Duration: <g:timeDuration start="${report.dateStarted}" end="${report.dateCompleted}"/></p>
    <p>User: ${report.author}</p>

   <g:if test="${report.message}">
       <p>
           Message:
       </p>
       <blockquote>
        ${report.message.encodeAsHTML()}
       </blockquote>
   </g:if>

</g:if>
