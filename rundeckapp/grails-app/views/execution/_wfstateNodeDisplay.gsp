<div class=" wfnodestate" data-node="${node}">
    <g:set var="nodestate" value="${workflowState.nodeStates[node]}"/>
    <g:set var="lastident" value="${nodestate?.lastIdentifier}"/>
    <g:set var="laststate" value="${nodestate?.stepStateMap[lastident]}"/>
    %{--overall node information--}%
    <div class="row">
        <div data-template="overall" data-node="${node}">
            <div class="col-sm-3 ">
                <span class="nodectx pull-right">${node.encodeAsHTML()}</span>
            </div>

            <div class="col-sm-3">
                <span class="stepctx">${lastident?.context.collect{it.step}.join("/")}.</span>
                <span class="stepident"></span>
            </div>

            <div class="col-sm-2">
                <span class="execstart"><g:formatDate date="${laststate?.startTime}"/></span>
            </div>

            <div class="col-sm-2">
                <span class="execend"><g:formatDate date="${laststate?.endTime}"/></span>
            </div>

            <div class="col-sm-2">
                <span class="execstate isnode" data-execstate="${laststate?.executionState}">${laststate?.executionState}</span>
            </div>
        </div>
    </div>
    %{--step specific info for node--}%
    <g:each in="${nodestate.stepStateMap.keySet().sort()}" var="ident">
        <g:set var="state" value="${nodestate.stepStateMap[ident]}"/>
        <div class="row" data-template="step" data-stepctx="${ident.context.collect { it.step }.join("/")}">
            <div>
                <div class="col-sm-3">
                <span class="nodectx" style="visibility: hidden">${node.encodeAsHTML()}</span>
                </div>

                <div class="col-sm-3">
                <span class="stepctx" >${ident.context.collect{it.step}.join("/")}.</span>
                <span class="stepident"></span>
                </div>

                <div class="col-sm-2">
                <span class="execstart"><g:formatDate date="${state.startTime}"/></span>
                </div>

                <div class="col-sm-2">
                <span class="execend"><g:formatDate date="${state.endTime}"/></span>
                </div>

                <div class="col-sm-2">
                <span class="execstate isnode" data-execstate="${state.executionState}">${state.executionState}</span>
                </div>
            </div>
        </div>
    </g:each>
</div>
