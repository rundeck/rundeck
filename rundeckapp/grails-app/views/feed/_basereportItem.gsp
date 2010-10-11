<g:if test="${report}">
    <g:if test="${report.jcExecId || report.jcJobId}">
        <g:render template="jobreportItem" model="[report:report]"/>
    </g:if>
    <g:else>
        <g:render template="commandreportItem" model="[report:report]"/>
    </g:else>
</g:if>
