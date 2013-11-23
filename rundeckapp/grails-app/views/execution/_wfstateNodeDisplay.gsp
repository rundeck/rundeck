%{--
  Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%

<div class=" wfnodestate container" data-node="${node}" data-template="${template}" style="${wdgt.styleVisible(unless:template)}"
     data-bind-attr="${template?'data-node:nodename':''}" data-template-parent="step">
    <g:set var="nodestate" value="${workflowState?.nodeStates[node]}"/>
    <g:set var="lastident" value="${nodestate?.lastIdentifier}"/>
    <g:set var="laststate" value="${nodestate?.stepStateMap? nodestate?.stepStateMap[lastident]:null}"/>
    %{--overall node information--}%
    <div class="row wfnodeoverall  " data-node="${node}" data-bind-attr="data-node:nodename">
        <g:render template="wfstateNodeStepTemplate" model="[node:node,state:laststate,ident:lastident,overall:true]"/>
    </div>
    %{--step specific info for node--}%
    <div class="wfnodecollapse" style="display: none">
    <div class="wfnodesteps" >
        <g:if test="${template}">
            <div data-template="step" style="display: none;" class="wfnodestep" data-bind-attr="data-stepctx:stepctx">
                <div class="row"
                     data-bind-attr="data-node:nodename,data-stepctx:stepctx"
                     data-node="${node}"
                >
                    <g:render template="wfstateNodeStepTemplate" model="[node: '']"/>
                </div>

                <div class="row">
                    <div class="col-sm-offset-3 col-sm-9 wfnodeoutput" data-bind-attr="data-node:nodename,data-stepctx:stepctx"
                         style="display: none">
                    </div>
                </div>
            </div>
        </g:if>
        <g:else>
            <g:each in="${nodestate?.stepStateMap.keySet().sort()}" var="ident" status="i">
                <g:set var="state" value="${nodestate.stepStateMap[ident]}"/>
                <g:set var="stepctx" value="${ident.context.collect { it.step }.join("/")}"/>

                <div class="wfnodestep" data-node="${node}" data-stepctx="${stepctx}">
                    <div class="row"
                         data-stepctx="${stepctx}"
                         data-node="${node}">
                        <g:render template="wfstateNodeStepTemplate" model="[node: node, state: state, ident: ident]"/>
                    </div>
                    <div class="row">
                        <div class="col-sm-offset-3 col-sm-9 wfnodeoutput"  data-node="${node}" data-stepctx="${stepctx}" style="display: none">
                        </div>
                    </div>
                </div>
            </g:each>
        </g:else>
    </div>

    %{--all output for the node--}%
    %{--<div class="row">
        <div class="wfnodeoutput" data-bind-attr="data-node:nodename" data-node="${node}"
             style="display: none" data-stepctx="">
        </div>
    </div>--}%
    </div>
</div>
