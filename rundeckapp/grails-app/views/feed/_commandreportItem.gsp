<g:if test="${report}">
    <p>
        <g:if test="${report.status=='succeed'}">
            <img src="${resource(dir: 'images', file: 'icon-tiny-ok.png')}" alt="" width="12px" height="12px"/>
        </g:if>
        <g:else>
            <img src="${resource(dir: 'images', file: 'icon-tiny-warn.png')}" alt="" width="12px" height="12px"/>
        </g:else>
        ${report.ctxCommand} Command <g:message code="status.label.${report.status}"/>.
        <g:if test="${report.dateCompleted}">
            ( <g:formatDate date="${report.dateCompleted}" formatName="jobslist.date.format"/> )
        </g:if>
    </p>
    <p>
        <g:set var="ctxName" value="${report.ctxName}"/>
        <g:set var="ctxType" value="${report.ctxType? report.ctxType :  report.ctxController}"/>
        ${ctxName}
        <g:if test="${ctxType && report.ctxProject}">
            [${ctxType}]
        </g:if>
        <g:if test="${report.ctxProject}">
            (${report.ctxProject})
        </g:if>
    </p>
    <g:if test="${report.dateCompleted !=report.dateStarted}">
        <p>Duration: <g:timeDuration start="${report.dateStarted}" end="${report.dateCompleted}"/></p>
    </g:if>
    <p>User: ${report.author}</p>

    <blockquote>${report.message.encodeAsHTML()}</blockquote>
</g:if>
