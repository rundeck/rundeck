<div class="jobstats" style="clear:both;">
    <table width="200px">
        <g:if test="${lastrun}">
            <tr class="lastrun">
                <td class="statlabel">
                    Last run:

                </td>
                <td class="statvalue">
                    by <span class="username">${lastrun.user}</span>,
                    <span class="when">
                        <g:relativeDate elapsed="${lastrun.dateCompleted}"/>
                    </span>
                </td>
                <td style="width:12px">
                    <g:link action="show" controller="execution" id="${lastrun.id}" title="View execution output"><img
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
                <g:if test="${scheduledExecution.user}">
                    by <span class="username">${scheduledExecution.user}</span>
                </g:if>
                <span class="when">
                    <g:relativeDate elapsed="${scheduledExecution.dateCreated}"/>
                </span>
            </td>
        </tr>
    </table>
</div>