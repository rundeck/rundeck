<div class="jobstats" style="clear:both;">
    <table class="table table-bordered table-condensed" >

        <tr>
            <th style="width: 20%" class="text-muted text-center  text-header">
                Executions
            </th>
        <g:if test="${lastrun}">
            <th style="width: 20%" class="text-muted text-center  text-header">
                Success rate
            </th>
        </g:if>
        <g:if test="${scheduledExecution.execCount > 0}">
            <th style="width: 20%" class="text-muted text-center  text-header">
                Average duration

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

            <td class="text-center">
                <span class="h3 text-success">
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
