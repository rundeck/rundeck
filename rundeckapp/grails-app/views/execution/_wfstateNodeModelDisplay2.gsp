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
<div data-bind="if: !stateLoaded()">
    <div class="container">
        <div class="row-space-lg row">
            <div class="col-sm-12">

                <div data-bind="if: errorMessage()">
                    <div class="well well-lg" data-bind="visible: errorMessage()" style="display: none">
                        <div class="text-warning" data-bind="text: errorMessage()">
                        </div>
                    </div>
                </div>

                <div data-bind="if: !errorMessage()">
                    <div class="well well-lg text-muted">
                        Waiting for state info…
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>
<div data-bind="if: stateLoaded()">
<div class="container">
<div class="row text-muted row-space">
    <div class="col-sm-3">
        Node
    </div>



    <div class="col-sm-2 col-sm-offset-5">
        Start time
    </div>

    <div class="col-sm-2">
        Duration
    </div>
</div>
</div>



<div data-bind="foreach: nodes">
<div class="wfnodestate container" data-bind="css: { open: expanded() }">
    <div class="row wfnodeoverall action" data-bind="click: toggleExpand">
        <div class="col-sm-3  nodectx"
             data-bind="attr: { title: name }, css: { 'auto-caret-container': expanded() } ">
            <div class="execstate nodename action" data-bind="attr: { 'data-execstate': summaryState }, css: { active: expanded() }">
                <i class="auto-caret"></i>
                <i class="rdicon icon-small node"></i>
                <span data-bind="text: name"></span>
            </div>
        </div>

        <div class="col-sm-3 " data-bind-action="stepoutput" data-bind-attr="data-node:nodename,data-stepctx:stepctx">
                <span class="execstate isnode" data-bind="attr: {'data-execstate': summaryState } ">
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
        <div class="col-sm-2 ">
            <span class="execend  info time"
                  data-bind="text: durationSimple"
                  ></span>
        </div>

    </div>
    %{--step specific info for node--}%
    <div class="row" data-bind="visible: expanded" >
        <div class="col-sm-12 wfnodesteps" data-bind="foreach: steps">

            <div class="container wfnodestep" data-bind="css: { open: followingOutput() }">
                <div class="row action" data-bind="click: $root.toggleOutputForNodeStep">
                    <div class="col-sm-3  action" data-bind="css: { 'auto-caret-container': followingOutput(), active: followingOutput() } ">
                        <span class="stepident execstate"
                              data-bind="attr: { 'data-execstate': executionState, title: stepctxdesc }">
                            <i class="auto-caret"></i>
                            <i class="rdicon icon-small" data-bind="css: type"></i>
                            <span data-bind="text: stepident"></span>
                        </span>
                    </div>

                    <div class=" col-sm-2">
                        <span class="execstate execstatedisplay " data-bind="attr: { 'data-execstate': executionState }"></span>
                    </div>


                    <div class="col-sm-2 col-sm-offset-3">
                        <span class="execstart info time" data-bind="text: startTimeFormat('h:mm:ss a')"></span>
                    </div>

                    <div class="col-sm-2">
                        <span class="execend  info time" data-bind="text: durationSimple()"></span>
                    </div>

                </div>

                <div class="row wfnodeoutput" data-bind="visible: followingOutput, attr: { 'data-node': $parent.name , 'data-stepctx': stepctx } ">

                </div>
                <div data-bind="visible: followingOutput() && outputLineCount() < 0 " class="row row-space ">
                    <div class="col-sm-12">
                        <p class="text-muted">
                            <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                                 alt="Spinner"/>
                            <em><g:message code="loading" /></em>
                        </p>
                    </div>
                </div>
                <div data-bind="visible: followingOutput() && outputLineCount() == 0 " class="row row-space ">
                    <div class="col-sm-12">
                        <p class="text-muted">
                            <i class="glyphicon glyphicon-info-sign"></i>
                            <em><g:message code="no.output" /></em>
                        </p>
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
</div>
</div>
