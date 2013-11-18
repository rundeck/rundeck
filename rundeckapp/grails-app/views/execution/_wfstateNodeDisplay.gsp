<div class=" wfnodestate" data-node="${node}" data-template="${template}" style="${wdgt.styleVisible(unless:template)}"
     data-bind-attr="${template?'data-node:nodename':''}" data-template-parent="step">
    <g:set var="nodestate" value="${workflowState?.nodeStates[node]}"/>
    <g:set var="lastident" value="${nodestate?.lastIdentifier}"/>
    <g:set var="laststate" value="${nodestate?.stepStateMap? nodestate?.stepStateMap[lastident]:null}"/>
    %{--overall node information--}%
    <div class="row  row-space wfnodeoverall  action" data-node="${node}" data-bind-attr="data-node:nodename">
        <g:render template="wfstateNodeStepTemplate" model="[node:node,state:laststate,ident:lastident,overall:true]"/>
    </div>
    %{--step specific info for node--}%
    <div class="wfnodesteps wfnodecollapse collapse">
        <g:if test="${template}">
            <div data-template="step" style="display: none;" class="wfnodestep">
                <div class="row action" data-bind-attr="data-stepctx:stepctx">
                    <g:render template="wfstateNodeStepTemplate" model="[node: '']"/>
                </div>

                <div class="row">
                    <div class="col-sm-12 wfnodeoutput " data-bind-attr="data-node:nodename,data-stepctx:stepctx">
                    </div>
                </div>
            </div>
        </g:if>
        <g:else>
            <g:each in="${nodestate?.stepStateMap.keySet().sort()}" var="ident" status="i">
                <g:set var="state" value="${nodestate.stepStateMap[ident]}"/>
                <g:set var="stepctx" value="${ident.context.collect { it.step }.join("/")}"/>
                <div class="row wfnodestep action " data-stepctx="${stepctx}">
                    <g:render template="wfstateNodeStepTemplate" model="[node: node, state: state, ident: ident]"/>
                </div>
                <div class="row">
                    <div class="col-sm-12 wfnodeoutput "  data-node="${node}" data-stepctx="${stepctx}">
                    </div>
                </div>
            </g:each>
        </g:else>
    </div>

    <div class="wfnodeoutput " data-bind-attr="data-node:nodename" data-node="${node}" style="display: none" data-stepctx="">
    </div>
</div>
