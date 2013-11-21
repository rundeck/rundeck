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



<div data-bind="foreach: nodes">
<div class=" wfnodestate " data-bind="css: { open: expanded() }">
    <div class="row wfnodeoverall action" data-bind="click: toggleExpand">
        <div class="col-sm-3  nodectx"
             data-bind="attr: { title: name }, css: { 'auto-caret-container': expanded() } ">
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

        <div class="col-sm-4 ">
            <div data-bind="with: currentStep(), visible: !expanded()">
                <span class="stepident execstate"
                      data-bind="attr: { 'data-execstate': executionState, title: stepctxdesc }">
                    <i class="rdicon icon-small" data-bind="css: type"></i>
                    <span data-bind="text: stepident"></span>
                </span>
            </div>
        </div>
        <div class="col-sm-3 ">
            <span class="execend  info time"
                  data-bind="text: durationSimple"
                  ></span>
        </div>

    </div>
    %{--step specific info for node--}%
    <div class="wfnodecollapse" data-bind="visible: expanded" >
        <div class="wfnodesteps" data-bind="foreach: steps">

            <div class="wfnodestep" data-bind="css: { open: followingOutput() }">
                <div class="row">
                    <div class="col-sm-2 col-sm-offset-3 action" data-bind="click: $root.toggleOutputForNodeStep, css: { 'auto-caret-container': followingOutput(), active: followingOutput() } ">
                        <span class="stepident execstate"
                              data-bind="attr: { 'data-execstate': executionState, title: stepctxdesc }">
                            <i class="auto-caret"></i>
                            <i class="rdicon icon-small" data-bind="css: type"></i>
                            <span data-bind="text: stepident"></span>
                        </span>
                    </div>

                    <div class=" col-sm-2">
                        <span class="execstate execstatedisplay isnode" data-bind="attr: { 'data-execstate': executionState }"></span>
                    </div>


                    <div class="col-sm-2">
                        <span class="execstart info time" data-bind="text: startTimeFormat('h:mm:ss a')"></span>
                    </div>

                    <div class="col-sm-3">
                        <span class="execend  info time" data-bind="text: durationSimple()"></span>
                    </div>

                </div>

                <div class="row">
                    <div class="col-sm-offset-3 col-sm-9 wfnodeoutput" data-bind="visible: followingOutput, attr: { 'data-node': $parent.name , 'data-stepctx': stepctx } ">
                        output here
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
</div>
</div>
