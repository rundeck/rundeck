<div class=" wfnodestate" data-node="${node}">
    %{--overall node information--}%
    <div class="row">
        <div class="col-sm-12">
            <span class="nodectx">${node.encodeAsHTML()}</span>
            <span class="stepctx" data-node="${node}"></span>
            <span class="stepident" data-node="${node}"></span>
            <span class="overallexecstart" data-node="${node}"></span>
            <span class="overallexecend" data-node="${node}"></span>
            <span class="execstate isnode" data-node="${node}"></span>
        </div>
    </div>
    %{--step specific info for node--}%
    <div class="row" data-template="step" style="display: none">
        <div class="col-sm-12">
            <span class="nodectx"></span>
            <span class="stepctx" data-node="${node}"></span>
            <span class="stepident" data-node="${node}"></span>
            <span class="stepexecstart" data-node="${node}"></span>
            <span class="stepexecend" data-node="${node}"></span>
            <span class="execstate isnode" data-node="${node}"></span>
        </div>
    </div>
</div>
