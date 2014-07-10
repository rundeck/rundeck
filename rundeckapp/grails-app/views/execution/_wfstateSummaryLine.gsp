
<span class=" execstate execstatedisplay overall h4"
      data-execstate="${execState}"
      data-bind="attr: { 'data-execstate': executionState() } ">
</span>

<span data-bind="visible: completed()" style="${wdgt.styleVisible(if: execution.dateCompleted)}">
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

started
<span class="timerel">at
    <span data-bind="text: formatTimeAtDate(startTime()), attr: {title: startTime() }">
        <g:if test="${execution.dateStarted}">
            <g:relativeDate atDate="${execution.dateStarted}"/>
        </g:if>
    </span>
</span>
by <g:username user="${execution.user}"/>

<div data-bind="visible: retryExecutionId()" class="">
    <span class="execstate h4" data-execstate="RETRY"><g:message code="retried" /></span> <g:message code="as.execution" />
    <a data-bind="attr: { 'href': retryExecutionUrl() }">
        <span data-bind="text: '#'+retryExecutionId()"></span>
    </a>

    <span class="text-muted"><g:message code="execution.retry.attempt.x.of.max.ko" args="${['text: retryExecutionAttempt()','text: retry()']}"/></span>
</div>
