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
<div data-bind="if: !stateLoaded()" style="margin-top:1em;">
    <div class="">
        <div class="row-space-lg row">
            <div class="col-sm-12">

                <div data-bind="if: errorMessage()">
                    <div class="well well-lg" data-bind="visible: errorMessage()" style="display: none">
                        <div class="text-warning" data-bind="text: errorMessage()">
                        </div>

                        <div style="margin-top:1em;">
                            <a class="btn btn-default btn-sm" href="#output"
                               data-bind="click: showTab.curry('tab_link_output') ">View Log Output &raquo;</a>
                        </div>
                    </div>
                </div>

                <div data-bind="if: !errorMessage() && !statusMessage()">
                    <div class="well well-lg text-primary">
                        Loading…
                    </div>
                </div>


                <div data-bind="if: statusMessage()">
                    <div class="well well-lg text-primary" data-bind="text: statusMessage()">
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>
<div data-bind="if: stateLoaded()">
  <div data-bind="if: activeTab()=='flow'">
    <div class="row row-space" data-bind="if: completed()">
      <div class="col-sm-12">
        <tmpl:wfstateSummaryScore/>
      </div>
    </div>
    <div class="row text-primary row-space">
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
  <div data-bind="foreach: activeNodes()">
    <div class="wfnodestate" data-bind="css: { open: expanded() }, attr: { 'data-node': name } ">
      <div class="row wfnodeoverall action" data-bind="click: toggleExpand">
          <div class="col-sm-3  nodectx"
               data-bind="attr: { title: name }, css: { 'auto-caret-container': expanded() } ">
              <div class="execstate nodename action isnode" data-bind="attr: { 'data-execstate': summaryState }, css: { active: expanded() }">
                  <i class="auto-caret"></i>
                  <i class="fas fa-hdd"></i>
                  <span data-bind="text: name"></span>
              </div>
          </div>

          <div class="col-sm-3 " data-bind-action="stepoutput" data-bind-attr="data-node:nodename,data-stepctx:stepctx">
                  <span class="execstate " data-bind="attr: {'data-execstate': summaryState } ">
                      <span data-bind="text: summary"></span>
                  </span>
          </div>

          <div class="col-sm-4 ">
              <div data-bind="with: currentStep(), visible: !expanded()">
                  <span class="stepident "
                        data-bind="attr: { 'data-execstate': executionState }">

                      <feature:disabled name="workflowDynamicStepSummaryGUI">
                          <i class="rdicon icon-small" data-bind="css: stepinfo().type"></i>
                          <span data-bind="text: stepinfo().stepident"></span>
                      </feature:disabled>
                      <feature:enabled name="workflowDynamicStepSummaryGUI">
                          <span data-bind="template: {name: 'step-info-simple-link', data:stepinfo(), as: 'stepinfo'}"></span>
                      </feature:enabled>
                  </span>
                  <span data-bind="if: ( executionState() == 'WAITING' ) " class="text-primary">
                      (Next up)
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
      <div data-bind="if: expanded">
      <div  data-bind="visible: expanded" >
          <div data-bind="foreach: steps">
              <div data-bind="if: !$data.parameterizedStep()">
              <div class="wfnodesteps" data-bind="attr: { 'data-node': node.name }">
              <div class=" wfnodestep" data-bind="css: { open: followingOutput() }, attr: { 'data-node': node.name, 'data-stepctx': $data.stepctx }">
                  <div class="row action" data-bind="click: $root.toggleOutputForNodeStep,
                                 event: { mouseover: function(){hovering(true);}, mouseout: function(){hovering(false);} } ">
                      <div class="col-sm-3 " >
                          <div class="stepident action col-inset"
                                data-bind="
                                attr: { 'data-execstate': executionState },
                                css: { 'auto-caret-container': followingOutput(), active: followingOutput() }
                                ">
                              <i class="auto-caret"></i>

                              <feature:disabled name="workflowDynamicStepSummaryGUI">
                                  <i class="rdicon icon-small" data-bind="css: stepinfo().type"></i>
                                  <span data-bind="text: stepinfo().stepident"></span>
                              </feature:disabled>
                              <feature:enabled name="workflowDynamicStepSummaryGUI">
                                  <span data-bind="visible: hovering() || followingOutput() ">
                                      %{--<span data-bind="if: followingOutput()">--}%
                                      <span data-bind="template: { name: 'step-info-parent-path-links', data:stepinfo, as: 'stepinfo' }"></span>
                                      %{--</span>--}%
                                      %{--<span data-bind="if: !followingOutput()">--}%
                                      %{--<span data-bind="template: { name: 'step-info-parent-path', data:stepinfo, as: 'stepinfo' }"></span>--}%
                                      %{--</span>--}%
                                  </span>

                                  <span data-bind="template: { name: 'step-info-simple-link', data:stepinfo, as: 'stepinfo' }"></span>
                              </feature:enabled>

                          </div>
                      </div>

                      <div class=" col-sm-2">
                          <span class="execstate execstatedisplay " data-bind="attr: {
                               'data-execstate': executionState,
                               'data-next': ( node.currentStep()==$data && executionState() == 'WAITING' )
                           }"></span>
                      </div>


                      <div class="col-sm-2 col-sm-offset-3">
                          <span class="execstart info time" data-bind="text: startTimeFormat('h:mm:ss a')"></span>
                      </div>

                      <div class="col-sm-2">
                          <span class="execend  info time" data-bind="text: durationSimple()"></span>
                      </div>

                  </div>

                  <div data-bind="if: followingOutput">
                  <div class="row " data-bind="visible: followingOutput">
                      <div class="col-sm-12 wfnodeoutput" data-bind="attr: { 'data-node': $parent.name , 'data-stepctx': stepctx } ">

                      </div>
                  </div>
                  <div data-bind="visible: followingOutput() && outputLineCount() < 0 " class="row row-space ">
                      <div class="col-sm-12">
                          <div class="well well-sm well-nobg inline">
                          <p class="text-primary">
                              <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                                   alt="Spinner"/>
                              <em><g:message code="loading" /></em>
                          </p>
                          </div>
                      </div>
                  </div>
                  <div data-bind="visible: followingOutput() && outputLineCount() == 0 " class="row row-space ">
                      <div class="col-sm-12">
                          <div class="well well-sm well-nobg inline">
                          <p class="text-primary">
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

      </div>
  </div>
      %{--endif--}%
  </div>
  </div>
</div>
