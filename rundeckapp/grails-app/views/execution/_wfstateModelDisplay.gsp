<%@ page import="com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState" %>
<style>
    .execstate[data-execstate=SUCCEEDED]{
        color: green;
    }
    .execstate[data-execstate=RUNNING]{
        color: blue;
    }
    .execstate[data-execstate=FAILED]{
        color: red;
    }
    .execstate[data-execstate=WAITING]{
        color: gray;
    }
    .wfstepstate .stepnodes{
        margin-left:10px;
    }
    .errmsg{
        color: gray;
    }
</style>
<g:each in="${workflowState.stepStates}" var="wfstep" status="i">
    <g:set var="nodestep" value="${wfstep.nodeStepTargets}"/>
    <g:set var="substep" value="${wfstep.hasSubWorkflow()}"/>
    <g:set var="myctx" value="${subCtx ? subCtx + '/' : ''}${i + 1}"/>
    <div id="wfstep_${i+1}" class="wfstepstate" data-stepctx="${myctx}">
    <div >
        Step ${subCtx? subCtx+'/':''}${i+1}:
        <span class="execstate ${wfstep.stepState.executionState} step" data-stepctx="${myctx}"
              data-execstate="${wfstep.stepState.executionState}">${wfstep.stepState.executionState}</span>
        <span class="errmsg step" data-stepctx="${myctx}" style="${wdgt.styleVisible(if: wfstep.stepState.errorMessage)}">
        <g:if test="${wfstep.stepState.errorMessage}">
            %{--${wfstep.stepState.errorMessage.encodeAsHTML()}--}%
        </g:if>
        </span>
    </div>

    <g:if test="${substep}">
        <g:set var="newsubctx" value="${(subCtx?subCtx+'/':'')+(i+1)}"/>
        <div class="sub">
        <g:render template="wfstateModelDisplay" model="[workflowState:wfstep.subWorkflowState,subCtx: newsubctx]"/>
        </div>
    </g:if>
    <g:elseif test="${nodestep}">
        <div class="stepnodes">
            <g:each in="${workflowState.nodeSet}" var="nodename">
                <g:set var="execState" value="${wfstep.nodeStateMap[nodename]?.executionState ?: ExecutionState.WAITING}"/>

                <div class="execstate ${execState} node"
                     data-node="${nodename.encodeAsHTML()}"
                     data-stepctx="${myctx}"
                     data-execstate="${execState}"
                >
                ${nodename}:
                    <span class="stepstatus ${execState}">${execState}</span>

                </div>
                <span class="errmsg isnode"
                      data-node="${nodename.encodeAsHTML()}"
                      data-stepctx="${myctx}"
                      style="${wdgt.styleVisible(if: wfstep.nodeStateMap[nodename]?.errorMessage)}">
                    <g:if test="${wfstep.nodeStateMap[nodename]?.errorMessage}">
                        %{--${wfstep.nodeStateMap[nodename].errorMessage.encodeAsHTML()}--}%
                    </g:if>
                </span>
            </g:each>
        </div>
    </g:elseif>
    <g:else>
        <g:set var="execState" value="${wfstep.stepState.executionState}"/>
    %{--workflow step        --}%
        <div class="execstate ${execState}"
             data-server="true"
             data-stepctx="${myctx}"
             data-execstate="${execState}">
            (server):
            <span class="stepstatus ${execState}">${execState}</span>

        </div>
    </g:else>
    </div>
</g:each>
