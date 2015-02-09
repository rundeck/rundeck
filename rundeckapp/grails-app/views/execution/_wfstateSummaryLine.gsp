
<span class=" execstate execstatedisplay overall h4"
      data-execstate="${enc(attr:execState)}"
      data-bind="attr: { 'data-execstate': executionState() } ">
</span>
<span data-bind="if: executionStatusString()!=null && executionState() != executionStatusString().toUpperCase()">
<span class="  h4 exec-status-text custom-status"
      data-bind="text: executionStatusString() ">
</span>
</span>

<span data-bind="visible: completed()" style="${wdgt.styleVisible(if: execution.dateCompleted)}">
    <g:message code="after" />
    <span data-bind="text: execDurationHumanized(), attr: {title: execDurationSimple() } " class="text-info">
    <g:if test="${execution.dateCompleted}">
        <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}"/>
    </g:if>
</span>
    <span class="timerel">
        <g:message code="at" />
        <span data-bind="text: formatTimeAtDate(endTime()), attr: {title: endTime() }">
            <g:if test="${execution.dateCompleted}">
                <g:relativeDate atDate="${execution.dateCompleted}"/>
            </g:if>
        </span>
    </span>

</span>

<g:message code="started" />
<span class="timerel">
    <g:message code="at" />
    <span data-bind="text: formatTimeAtDate(startTime()), attr: {title: startTime() }">
        <g:if test="${execution.dateStarted}">
            <g:relativeDate atDate="${execution.dateStarted}"/>
        </g:if>
    </span>
</span>
<g:message code="by" />
<g:username user="${execution.user}"/>
<span data-bind="if: completed() || jobAverageDuration() <= 0 ">
    <span class="text-muted">
        <i class="glyphicon glyphicon-time"></i>
        %{--<g:message code="elapsed.time.prompt" />--}%
    </span>
    <span data-bind="text: execDurationSimple()" class="text-info"></span>
</span>

<div data-bind="visible: retryExecutionId()" class="">
    <span class="execstate h4" data-execstate="RETRY"><g:message code="retried" /></span> <g:message code="as.execution" />
    <a data-bind="attr: { 'href': retryExecutionUrl() }">
        <span data-bind="text: '#'+retryExecutionId()"></span>
    </a>

    <span class="text-muted"><g:message code="execution.retry.attempt.x.of.max.ko" args="${['text: retryExecutionAttempt()','text: retry()']}"/></span>
</div>
