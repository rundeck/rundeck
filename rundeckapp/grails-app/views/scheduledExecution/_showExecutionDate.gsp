    <g:render template="/scheduledExecution/execStatusText" model="${[execution: execution]}"/>
    <g:if test="${execution.dateCompleted != null}">
        in <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}"/>
        <span class="timerel"
              title="${g.formatDate(date: execution.dateCompleted)} - ${execution.dateCompleted.time}">at <g:relativeDate
                atDate="${execution.dateCompleted}"/></span>
    </g:if>
