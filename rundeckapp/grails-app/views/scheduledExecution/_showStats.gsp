<div class="jobstats" style="clear:both;">
    <table class="table table-bordered table-condensed" >

        <tr>
            <th style="width: 20%" class="text-muted text-center  text-header">
                <g:message code="Execution.plural" />
            </th>
        <g:if test="${lastrun}">
            <th style="width: 20%" class="text-muted text-center  text-header">
                <g:message code="success.rate" />
            </th>
        </g:if>
        <g:if test="${scheduledExecution.execCount > 0}">
            <th style="width: 20%" class="text-muted text-center  text-header">
                <g:message code="average.duration" />
            </th>
        </g:if>
        </tr>
        <tr>
            <td class="text-center">
                <span class="h3 ">
                    <g:formatNumber number="${total}" />
                </span>
            </td>
        <g:if test="${lastrun}">
            <g:set var="successrate" value="${params.float('success')?:successrate}"/>
            <g:set var="ratecolors" value="${['text-success','text-muted','text-warning','text-danger']}"/>
            <g:set var="ratelevels" value="${[0.9f,0.75f,0.5f]}"/>
            <g:set var="successindex" value="${ratelevels.findIndexOf{it<=(successrate)}}"/>
            <g:set var="successcolor" value="${successindex>=0?ratecolors[successindex]:ratecolors[-1]}"/>
            <td class="text-center">
                <span class="h3 ${successcolor}">
                    <g:formatNumber number="${successrate}" type="percent"/>
                </span>
            </td>
        </g:if>
        <g:if test="${scheduledExecution.execCount>0}">
            <td class="text-center">
                    <span class="h3 ">
                        <g:timeDuration time="${scheduledExecution.execCount>0?  scheduledExecution.totalTime /scheduledExecution.execCount  : 0}"/>
                    </span>
                </td>
        </g:if>

        </tr>
    </table>
</div>
