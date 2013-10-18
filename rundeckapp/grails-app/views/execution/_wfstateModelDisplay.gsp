<style>
    .stepstatus.SUCCEEDED{
        color: green;
    }
    .stepstatus.RUNNING{
        color: blue;
    }
    .stepstatus.FAILED{
        color: red;
    }
    .wfstepstate .stepnodes{
        margin-left:10px;
    }
</style>
<g:each in="${workflowState.stepStates}" var="wfstep" status="i">
    <div id="wfstep_${i+1}" class="wfstepstate">
    <div >
        Step ${i+1}:
        <span class="stepstatus ${wfstep.stepState.executionState}">${wfstep.stepState.executionState}</span>
    </div>
    <div class="stepnodes">
        <g:each in="${wfstep.nodeStateMap.keySet()}" var="nodename">
            <div class="nodestate" data-node="${nodename.encodeAsHTML()}">
            ${nodename}:
            <span class="stepstatus ${wfstep.nodeStateMap[nodename].executionState}">${wfstep.nodeStateMap[nodename].executionState}</span>
            </div>
        </g:each>
    </div>
    </div>
</g:each>
