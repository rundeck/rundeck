<g:if test="${report}">
    <p>
        <g:if test="${report.status=='succeed'}">
            <img src="${resource(dir: 'images', file: 'icon-tiny-ok.png', absolute:true)}" alt="" width="12px" height="12px"/>
        </g:if>
        <g:else>
            <img src="${resource(dir: 'images', file: 'icon-tiny-warn.png', absolute:true)}" alt="" width="12px" height="12px"/>
        </g:else>
        %{--<g:if test="${report.jcJobId}"><g:message code="domain.ScheduledExecution.title"/></g:if>
        <g:elseif test="${report.jcExecId}"><g:message code="domain.Execution.title"/></g:elseif>
        <g:else>${(report.reportId ? report.reportId : report.title).encodeAsHTML()}</g:else>--}%
        <g:message code="status.label.${report.status}"/>
        <g:if test="${(report.status == 'cancel') }">
            by: ${report.abortedByUser}
        </g:if>
        ( <g:formatDate date="${report.dateCompleted}" formatName="jobslist.date.format"/>) 
    </p>

    <g:set var="vals" value="${['?','?','?']}"/>
    <g:if test="${report?.node=~/^\d+\/\d+\/\d+$/}">
        <g:set var="vals" value="${report.node.split('/') as List}"/>
    </g:if>
    <g:else>
        <g:set var="vals" value="${[report?.status=='succeed'?'1':'0',report?.status=='succeed'?'0':'1','1']}"/>
    </g:else>
    <g:set var="summary" value=""/>
    <g:if test="${vals.size()>2 && vals[2]!='0'}">
        <g:set var="a" value="${Integer.parseInt(vals[0])}"/>
        <g:set var="den" value="${Integer.parseInt(vals[2])}"/>
        <g:set var="fai" value="${Integer.parseInt(vals[1])}"/>
        <g:set var="sucperc" value="${(int)Math.floor((a/den)*100)}"/>
        <g:set var="perc" value="${(int)Math.floor((fai/den)*100)}"/>
        <g:if test="${vals[0] && vals[2]}">
            <g:set var="sucsummary" value="${vals[0]+' of '+vals[2]}"/>
            <g:set var="summary" value="${vals[1]+' of '+vals[2]}"/>
        </g:if>
    </g:if>
    <g:else>
        <g:set var="perc" value="${0}"/>
    </g:else>
    <div style="text-indent:2em;">
    <p>
        Nodes:
    <g:if test="${vals[1]!='0'}">
        ${vals[1]} failed (${perc}%)
    </g:if>
    <g:else>
        ${vals[0]} ok
    </g:else>
    </p>
    </div>
    <p>
        <g:if test="${report.adhocScript}">
            <g:set var="jobtitle" value="${report.adhocScript}"/>
        </g:if>
        <g:else>
            <g:set var="jobtitle" value="${report.title}"/>
        </g:else>
        ${jobtitle.encodeAsHTML()}
    </p>
    <g:if test="${report.jcJobId}">
        <p><g:link title="View ${g.message(code:'domain.ScheduledExecution.title')} in ${g.message(code:'main.app.name')}" controller="scheduledExecution" action="show" id="${report.jcJobId}" absolute="true"><g:message code="domain.ScheduledExecution.title"/> Detail &raquo;</g:link></p>
    </g:if>
    <g:if test="${report.jcExecId }">
    <p>
        <g:link controller="execution" action="show" id="${report.jcExecId}" title="View execution output" absolute="true" >View Output &raquo;</g:link>
    </p>
    </g:if>
    <div style="text-indent:2em;">
        <p>Duration: <g:timeDuration start="${report.dateStarted}" end="${report.dateCompleted}"/></p>
        <p>User: ${report.author}</p>
    </div>
</g:if>
