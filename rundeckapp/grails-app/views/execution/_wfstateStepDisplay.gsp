<%@ page import="com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState" %>
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

<g:if test="${wfstep.hasSubWorkflow()}">
    <g:set var="newsubctx" value="${(subCtx ? subCtx + '/' : '') + (i + 1)}"/>
    <g:render template="wfstateModelDisplay"
                  model="[workflowState: wfstep.subWorkflowState, subCtx: newsubctx]"/>
</g:if>
<g:else>
<g:set var="nodestep" value="${wfstep.nodeStep}"/>
<g:set var="substep" value="${wfstep.hasSubWorkflow()}"/>
<g:set var="myctx" value="${subCtx ? subCtx + '/' : ''}${i + 1}"/>
<div id="wfstep_${i + 1}" class="row wfstepstate" data-stepctx="${myctx}">
    <div class="col-sm-4">
        <span class="execstate step"
              data-stepctx="${myctx}"
              data-execstate="${wfstep.stepState.executionState}">

        </span>
        <span class="stepctx">Step ${subCtx ? subCtx + '/' : ''}${i + 1}</span>
        <span class=" stepident" data-stepctx="${myctx}"></span>
    </div>

    <div class="col-sm-8 ">
        <div class="row">
            <div class="col-sm-12">
                <span class="errmsg step"
                      data-stepctx="${myctx}"
                      style="${wdgt.styleVisible(if: wfstep.stepState.errorMessage)}">
                    <g:if test="${wfstep.stepState.errorMessage}">
                    %{--${wfstep.stepState.errorMessage.encodeAsHTML()}--}%
                    </g:if>
                </span>
            </div>
        </div>

        <div class="row">
            <g:if test="${substep}">
            </g:if>
            <g:elseif test="${false}">
                <div class="nodestates col-sm-12">
                    <g:each in="${wfstep.nodeStepTargets ?: workflowState.nodeSet?.sort() ?: []}" var="nodename">
                        <g:set var="execState"
                               value="${wfstep.nodeStateMap[nodename]?.executionState ?: ExecutionState.WAITING}"/>
                        <div>
                            <span class="execstate isnode"
                                  data-node="${nodename.encodeAsHTML()}"
                                  data-stepctx="${myctx}"
                                  data-execstate="${execState}">
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
                <div class="nodestates col-sm-12">

                </div>
            </g:else>

        </div>
    </div>
</div>
</g:else>
