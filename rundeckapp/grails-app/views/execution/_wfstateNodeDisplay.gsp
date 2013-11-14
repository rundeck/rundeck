<div class=" wfnodestate" data-node="${node}">
    <g:set var="nodestate" value="${workflowState.nodeStates[node]}"/>
    <g:set var="lastident" value="${nodestate?.lastIdentifier}"/>
    <g:set var="laststate" value="${nodestate?.stepStateMap[lastident]}"/>
    %{--overall node information--}%
    <div class="row">
        <div class="col-sm-12" data-template="overall" data-node="${node}">
            <span class="nodectx">${node.encodeAsHTML()}</span>
            <span class="stepctx">${lastident?.context.collect{it.step}.join("/")}</span>
            <span class="stepident">${lastident?.context.collect { it.step }.join("/")}</span>
            <span class="execstart"><g:formatDate date="${laststate?.startTime}"/></span>
            <span class="execend"><g:formatDate date="${laststate?.endTime}"/></span>
            <span class="execstate isnode" data-execstate="${laststate?.executionState}">${laststate?.executionState}</span>
        </div>
    </div>
    %{--step specific info for node--}%
    <g:each in="${nodestate.stepStateMap.keySet().sort()}" var="ident">
        <g:set var="state" value="${nodestate.stepStateMap[ident]}"/>
        <div class="row" data-template="step" data-stepctx="${ident.context.collect { it.step }.join("/")}">
            <div class="col-sm-12">
                <span class="nodectx">${node.encodeAsHTML()}</span>
                <span class="stepctx" >${ident.context.collect{it.step}.join("/")}</span>
                <span class="stepident">${ident.context.collect { it.step }.join("/")}</span>
                <span class="execstart"><g:formatDate date="${state.startTime}"/></span>
                <span class="execend"><g:formatDate date="${state.endTime}"/></span>
                <span class="execstate isnode" data-execstate="${state.executionState}">${state.executionState}</span>
            </div>
        </div>
    </g:each>
</div>
