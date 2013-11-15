<div class=" wfnodestate" data-node="${node}">
    <g:set var="nodestate" value="${workflowState?.nodeStates[node]}"/>
    <g:set var="lastident" value="${nodestate?.lastIdentifier}"/>
    <g:set var="laststate" value="${nodestate?.stepStateMap[lastident]}"/>
    %{--overall node information--}%
    <div class="row  row-space wfnodeoverall" data-node="${node}" data-bind-attr="data-node:nodename">
        <g:render template="wfstateNodeStepTemplate" model="[node:node,state:laststate,ident:lastident]"/>
    </div>
    %{--step specific info for node--}%
    <g:each in="${nodestate?.stepStateMap.keySet().sort()}" var="ident">
        <g:set var="state" value="${nodestate.stepStateMap[ident]}"/>
        <div class="row wfnodestep" data-stepctx="${ident.context.collect { it.step }.join("/")}">
            <g:render template="wfstateNodeStepTemplate" model="[node: node, state: state, ident: ident]"/>
        </div>
    </g:each>
</div>
