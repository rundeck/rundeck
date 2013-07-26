<div class="jobstats" style="clear:both;">
    <table width="200px">
        <tr class="">

            <td class="statlabel">
                Executions:
            </td>
            <td class="statvalue">
                <g:formatNumber number="${total}" />
            </td>
        <g:if test="${lastrun}">
                <td class="statlabel">
                    Success rate:
                </td>
                <td class="statvalue">
                    <g:formatNumber number="${successrate}" type="percent"/>
                </td>
        </g:if>
        <g:if test="${scheduledExecution.execCount>0}">
                <td class="statlabel">
                    Average duration:

                </td>
                <td class="statvalue">
                    <g:timeDuration time="${scheduledExecution.execCount>0?  scheduledExecution.totalTime /scheduledExecution.execCount  : 0}"/>

                </td>
        </g:if>
            <td class="statlabel">
                Created:

            </td>
            <td class="statvalue">
                <span class="when">
                    <g:relativeDate elapsed="${scheduledExecution.dateCreated}"/>
                </span>
            </td>
        </tr>
    </table>
</div>
