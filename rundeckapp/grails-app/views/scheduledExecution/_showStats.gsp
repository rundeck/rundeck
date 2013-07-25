<div class="jobstats" style="clear:both;">
    <table width="200px">
        <g:if test="${lastrun}">
            <tr class="lastrun">
                <td class="statlabel">
                    Last run:

                </td>
                <td class="statvalue">
                    <g:link action="show" controller="execution" id="${lastrun.id}" title="View execution output">
                    by <span class="username"><g:username user="${lastrun.user}"/></span>,
                    <span class="when">
                        <g:relativeDate elapsed="${lastrun.dateCompleted}"/>
                    </span>
                    <img
                        src="${resource(dir: 'images', file: 'icon-tiny-' + (lastrun?.status == 'true' ? 'ok' : 'warn') + '.png')}"
                        alt="" width="12px" height="12px"/></g:link>
                </td>
            </tr>
            <tr>
                <td class="statlabel">
                    Success rate:
                </td>
                <td class="statvalue">
                    <g:formatNumber number="${successrate}" type="percent"/>
                </td>
            </tr>
        </g:if>
        <g:if test="${scheduledExecution.execCount>0}">
            <tr>
                <td class="statlabel">
                    Average duration:

                </td>
                <td class="statvalue">
                    <g:timeDuration time="${scheduledExecution.execCount>0?  scheduledExecution.totalTime /scheduledExecution.execCount  : 0}"/>

                </td>
            </tr>
        </g:if>
        <tr>
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
