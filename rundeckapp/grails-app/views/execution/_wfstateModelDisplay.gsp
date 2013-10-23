<%@ page import="com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState" %>
<style>
    .execstate.SUCCEEDED{
        color: green;
    }
    .execstate.RUNNING{
        color: blue;
    }
    .execstate.FAILED{
        color: red;
    }
    .execstate.WAITING{
        color: gray;
    }
    .wfstepstate .stepnodes{
        margin-left:10px;
    }
</style>
<g:each in="${workflowState.stepStates}" var="wfstep" status="i">
    <g:set var="nodestep" value="${wfstep.nodeStepTargets}"/>
    <g:set var="substep" value="${wfstep.hasSubWorkflow()}"/>
    <div id="wfstep_${i+1}" class="wfstepstate">
    <div >
        Step ${subCtx? subCtx+'/':''}${i+1}: (${substep},${nodestep})
        <span class="execstate ${wfstep.stepState.executionState}">${wfstep.stepState.executionState}</span>
        <g:if test="${wfstep.stepState.errorMessage}">
            ${wfstep.stepState.errorMessage}
        </g:if>
    </div>

    <g:if test="${substep}">
        <g:set var="newsubctx" value="${(subCtx?subCtx+'/':'')+(i+1)}"/>
        <g:render template="wfstateModelDisplay" model="[workflowState:wfstep.subWorkflowState,subCtx: newsubctx]"/>
    </g:if>
    <g:elseif test="${nodestep}">
        <div class="stepnodes">
            <g:each in="${workflowState.nodeSet}" var="nodename">
                <g:set var="execState" value="${wfstep.nodeStateMap[nodename]?.executionState ?: ExecutionState.WAITING}"/>

                <div class="execstate ${execState}" data-node="${nodename.encodeAsHTML()}">
                ${nodename}:
                    <span class="stepstatus ${execState}">${execState}</span>

                </div>
            </g:each>
        </div>
    </g:elseif>
    <g:else>
        <g:set var="execState" value="${wfstep.stepState.executionState}"/>
    %{--workflow step        --}%
        <div class="execstate ${execState}" data-node="server">
            (server):
            <span class="stepstatus ${execState}">${execState}</span>

        </div>
    </g:else>
    </div>
</g:each>
