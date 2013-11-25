    <g:render template="/scheduledExecution/execStatusText" model="${[execution: execution]}"/>
    <span style="${wdgt.styleVisible(if: execution.dateCompleted != null )}"
        data-bind="visible: completed()"
    >
        after <span data-bind="text: execDurationHumanized(), attr: {title: execDurationSimple() } ">
        <g:if test="${execution.dateCompleted}">
            <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}"/>
        </g:if>
        </span>
        <span class="timerel">at
            <span data-bind="text: formatTimeAtDate(endTime()), attr: {title: endTime() }">
            <g:if test="${execution.dateCompleted}">
                <g:relativeDate atDate="${execution.dateCompleted}"/>
            </g:if>
            </span>
        </span>
    </span>
