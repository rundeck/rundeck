<%@ page import="com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState" %>

<g:each in="${workflowState.stepStates}" var="wfstep" status="i">
    <g:render template="wfstateStepDisplay" model="[wfstep:wfstep,i:i]"/>
</g:each>
