<%@ page import="com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState" %>
<style>
    .execstate[data-execstate=NODE_MIXED]{
        color: orange;
    }
    .execstate[data-execstate=NODE_MIXED]:after {
        content: ' ~';
    }
    .execstate[data-execstate=SUCCEEDED]{
        color: gray;
    }
    .execstate[data-execstate=SUCCEEDED]:after{
        content: ' √';
    }
    .execstate[data-execstate=NODE_PARTIAL_SUCCEEDED]{
        color: gray;
    }
    .execstate[data-execstate=NODE_PARTIAL_SUCCEEDED]:after{
        content: ' ';
    }
    .execstate[data-execstate=RUNNING]{
        color: blue;
        background-image: url(${g.resource(dir: 'images',file: 'icon-tiny-disclosure-waiting.gif')});
        padding-right: 16px;
        background-repeat: no-repeat;
        background-position: right 2px;
    }
    .execstate[data-execstate=FAILED]{
        color: red;
    }
    .execstate[data-execstate=FAILED]:after{
        content: ' -';
    }
    .execstate[data-execstate=WAITING], .execstate[data-execstate=NOT_STARTED]{
        color: lightgray;
    }
    .execstate[data-execstate=WAITING]:after{
        content: ' zzz';
    }
    .execstate[data-execstate=NOT_STARTED]:after{
        content: ' …';
    }
    .wfstepstate .stepnodes{
        margin-left:10px;
    }
    .errmsg{
        color: gray;
    }
</style>
<g:each in="${workflowState.stepStates}" var="wfstep" status="i">
    <g:set var="nodestep" value="${wfstep.nodeStep}"/>
    <g:set var="substep" value="${wfstep.hasSubWorkflow()}"/>
    <g:set var="myctx" value="${subCtx ? subCtx + '/' : ''}${i + 1}"/>
    <div id="wfstep_${i+1}" class="wfstepstate" data-stepctx="${myctx}">
    <div >
        Step ${subCtx? subCtx+'/':''}${i+1}:
        <span class="execstate step"
              data-stepctx="${myctx}"
              data-execstate="${wfstep.stepState.executionState}">

          </span>
        <span class="errmsg step"
              data-stepctx="${myctx}"
              style="${wdgt.styleVisible(if: wfstep.stepState.errorMessage)}">
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
        <div class="nodestates">
            <g:each in="${wfstep.nodeStepTargets?: workflowState.nodeSet?.sort()?:[]}" var="nodename">
                <g:set var="execState" value="${wfstep.nodeStateMap[nodename]?.executionState ?: ExecutionState.WAITING}"/>
                <div>
                    <span class="execstate isnode"
                         data-node="${nodename.encodeAsHTML()}"
                         data-stepctx="${myctx}"
                         data-execstate="${execState}"
                    >
                    ${nodename}

                    </span>
                    <span class="errmsg isnode"
                          data-node="${nodename.encodeAsHTML()}"
                          data-stepctx="${myctx}"
                          style="${wdgt.styleVisible(if: wfstep.nodeStateMap[nodename]?.errorMessage)}">
                        <g:if test="${wfstep.nodeStateMap[nodename]?.errorMessage}">
                            %{--${wfstep.nodeStateMap[nodename].errorMessage.encodeAsHTML()}--}%
                        </g:if>
                    </span>
                </div>
            </g:each>
        </div>
    </g:elseif>
    <g:else>
        <div class="nodestates">

        </div>
    </g:else>
    </div>
</g:each>
