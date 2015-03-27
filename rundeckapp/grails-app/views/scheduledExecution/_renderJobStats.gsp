<%@ page import="rundeck.Execution" %>
<g:set var="lastrun"
       value="${scheduledExecution.id ? Execution.findByScheduledExecutionAndDateCompletedIsNotNull(scheduledExecution, [max: 1, sort: 'dateStarted', order: 'desc']) : null}"/>
<g:set var="successcount"
       value="${scheduledExecution.id ? Execution.countByScheduledExecutionAndStatusInList(scheduledExecution, ['true','succeeded']) : 0}"/>
<g:set var="execCount"
       value="${scheduledExecution.id ? Execution.countByScheduledExecution(scheduledExecution) : 0}"/>
<g:set var="successrate" value="${execCount > 0 ? (successcount / execCount) : 0}"/>
<g:render template="/scheduledExecution/showStats"
          model="[scheduledExecution: scheduledExecution, lastrun: lastrun ? lastrun : null, successrate: successrate]"/>
