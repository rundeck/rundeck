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

<%@ page import="com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState" %>

<div class="container">
<div class="row">
    <div class="col-sm-3">
        Node
    </div>

    <div class="col-sm-2">
        Step
    </div>

    <div class="col-sm-2">
        State
    </div>

    <div class="col-sm-2">
        Start time
    </div>

    <div class="col-sm-3">
        Duration
    </div>
</div>

%{--<g:render template="wfstateNodeDisplay2" model="[node: '', template: 'node']"/>--}%
%{--<g:each in="${workflowState.allNodes}" var="node" status="i">--}%
    %{--<g:render template="wfstateNodeDisplay2" model="[node: node, workflowState: workflowState, i: i]"/>--}%
%{--</g:each>--}%


<div data-bind="foreach: nodes">
<div class=" wfnodestate " data-bind="css: { open: expanded() }">
    <div class="row wfnodeoverall">
        <div class="col-sm-3  nodectx"
             data-bind="attr: { title: name }, click: toggleExpand, css: { 'auto-caret-container': expanded() } ">
            <div class="textbtn isnode execstate nodename action" data-bind="attr: { 'data-execstate': summaryState }, css: { active: expanded() }">
                <i class="auto-caret"></i>
                <span data-bind="text: name"></span>
            </div>
        </div>

        <div class="col-sm-2 " data-bind-action="stepoutput" data-bind-attr="data-node:nodename,data-stepctx:stepctx">
                <span class="execstate" data-bind="attr: {'data-execstate': summaryState } ">
                    <span data-bind="text: summary"></span>
                </span>
        </div>

        <div class="col-sm-3 col-sm-offset-4">
            <span class="execend  info time"
                  data-bind="text: durationSimple"
                  ></span>
        </div>

    </div>
    %{--step specific info for node--}%
    <div class="wfnodecollapse" data-bind="visible: expanded" >
        <div class="wfnodesteps" data-bind="foreach: steps">

            <div class="wfnodestep" >
                <div class="row">


                    <div class="col-sm-2 col-sm-offset-3 action" data-bind-action="stepoutput"
                         data-bind-attr="data-node:nodename,data-stepctx:stepctx">
                        <span class="stepident execstate"
                              xdata-bind="title:stepctxdesc"
                              data-bind="attr: { 'data-execstate': executionState }">
                            <i class="auto-caret"></i>
                            <i class="rdicon icon-small" data-bind="css: type"></i>
                            <span data-bind="text: stepident"></span>
                        </span>
                    </div>

                    <div class=" col-sm-2">
                        <span class="execstate execstatedisplay isnode" data-bind="attr: { 'data-execstate': executionState }"></span>
                    </div>


                    <div class="col-sm-2">
                        <span class="execstart info time" data-bind="text: startTimeSimple()"
                              xdata-bind-format="moment:h:mm:ss a"></span>
                    </div>

                    <div class="col-sm-3">
                        %{--<g:unless test="${overall}">--}%
                        <span class="">
                            <span class="execend  info time" data-bind="text: durationSimple()"></span>
                    </span>
                        %{--</g:unless>--}%
                    </div>

                </div>

                <div class="row">
                    <div class="col-sm-offset-3 col-sm-9 wfnodeoutput"
                         data-node="${node}"
                         data-stepctx="${stepctx}" style="display: none">
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
</div>
</div>
