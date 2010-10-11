<%--
TODO: Remove model report stuff
--%>
<g:if test="${report}">
    <p> Workbench Model was Changed.
        <g:if test="${report.dateCompleted}">
            ( <g:formatDate date="${report.dateCompleted}" formatName="jobslist.date.format"/> )
        </g:if>
    </p>
    <p>
        <g:if test="${report.title}">
            ${report.title.encodeAsHTML()}
        </g:if>

        <g:if test="${report.rptItemType=='object'}">
            ${report?.ctxName}
            
            <g:if test="${report.ctxProject}">
                (${report.ctxProject})
            </g:if>
        </g:if>
        <g:if test="${report.rptItemType=='type'}">
            ${report?.ctxType}

            <g:if test="${report.ctxProject}">
                (${report.ctxProject})
            </g:if>
        </g:if>
        <g:elseif test="${report.rptItemType=='project'}">
            ${report?.ctxProject}
        </g:elseif>
    </p>
    <p>by ${report.author} @ <g:relativeDate atDate="${report.dateCompleted}"/></p>

    <blockquote>
        ${report.message.encodeAsHTML()}
    </blockquote>
</g:if>
