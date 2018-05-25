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
<div class="container">
<script type="text/html" id="node-current-state-simple">
    <g:render template="nodeCurrentStateSimpleKO"/>
</script>

<div data-bind="if: !stateLoaded()">
        <div class="row-space-lg row">
            <div class="col-sm-12">

                <div data-bind="if: errorMessage()">
                    <div class="well well-lg" data-bind="visible: errorMessage()" style="display: none">
                        <span class="text-warning" data-bind="text: errorMessage()">
                        </span>
                        <div>
                            <a class="btn btn-default btn-sm" href="#output" data-bind="click: showTab.curry('tab_link_output') "><g:message code="button.action.view.log.output" /></a>
                        </div>
                    </div>
                </div>

                <div data-bind="if: !errorMessage() && !statusMessage()">
                    <div class="well well-lg text-muted">
                        <g:message code="waiting.for.state.info" />
                    </div>
                </div>
                <div data-bind="if: statusMessage()">
                    <div class="well well-lg text-muted" data-bind="text: statusMessage()">

                    </div>
                </div>

            </div>
        </div>
</div>


<div data-bind="if: stateLoaded()">

        <div class="row row-space" data-bind="if: completed()">
            <div class="col-sm-12">
                <tmpl:wfstateSummaryScore />
            </div>
        </div>

        <div class="row row-space" data-bind="if: !completed()">
            <div class="col-sm-12" >
                <table class="table table-bordered">

                    <tr>
                        <th colspan="3" class="text-muted table-footer text-small">
                            <g:message code="node.summary" />
                        </th>
                    </tr>
                    <tr>
                       <th style="width: 33%" class="text-muted text-center h5 text-header">
                           <g:message code="waiting" />
                           <g:render template="/common/helpTooltipIconKO"
                                   model="[messageCode:'workflowState.summary.nodes.waiting.description']"/>
                       </th>
                       <th style="width: 33%" class="text-muted text-center h5 text-header">
                           <g:message code="running" />
                           <g:render template="/common/helpTooltipIconKO"
                                     model="[messageCode: 'workflowState.summary.nodes.running.description']"/>
                       </th>
                       <th style="width: 33%" class="text-muted text-center h5 text-header">
                           <g:message code="done" />
                           <g:render template="/common/helpTooltipIconKO"
                                     model="[messageCode: 'workflowState.summary.nodes.complete.description']"/>
                       </th>
                    </tr>
                    <tr>
                        <td>
                                <div class="text-center">
                                    <span class="h3 text-muted" data-bind="text: waitingNodes().length"></span>
                                </div>
                        </td>
                        <td>

                            <div class="text-center">
                                <span class=" h3"
                                      data-bind="css: {'text-info': runningNodes().length > 0 , 'text-muted': runningNodes().length < 1 } ">
                                    <span class=" " data-bind="text: runningNodes().length"></span>
                                </span>
                            </div>


                        </td>
                        <td >

                            <div class="text-center">
                                <span class=" h3"
                                      data-bind="css: {'text-info': completedNodes().length > 0 , 'text-muted': completedNodes().length < 1 } ">
                                    <span data-bind="text: completedNodes().length"></span>
                                </span>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>
        </div>


        <div class="row " data-bind="if: !completed()">
            <div class="col-sm-3 text-muted h4 text-right">
                Now Running
            </div>
            <div class="col-sm-9">
                <section data-bind="visible: runningNodes().length > 0, if: runningNodes().length > 0" >
                    <div data-bind="foreach: runningNodes()">
                        <div data-bind="template: {name:'node-current-state-simple',data:$data}">
                        </div>
                    </div>
                </section>
            </div>
        </div>

        <div class="row " data-bind="if: failedNodes().length > 0 ">
            <div class="col-sm-3 text-muted h4 text-right">
                <span data-bind="text: failedNodes().length"></span>
                Failed Nodes
            </div>
            <div class="col-sm-9">
                <div data-bind="if: failedNodes().length > 0" >
                    <div data-bind="foreach: failedNodes()">
                        <div data-bind="template: {name:'node-current-state-simple',data:$data}">
                        </div>
                    </div>
                </div>
            </div>
            </div>

    <div class="row " data-bind="if: partialNodes().length > 0">
        <div class="col-sm-3 text-muted h4 text-right">
            <span data-bind="text: partialNodes().length"></span>
            Incomplete Nodes
        </div>

        <div class="col-sm-9">
                %{--display up to 5 partial nodes nodes--}%
                <div data-bind="if:  partialNodes().length > 0" >
                    <div data-bind="foreach: partialNodes()">
                        <div data-bind="template: {name:'node-current-state-simple',data:$data}">
                        </div>
                    </div>
                </div>
            </div>
        </div>
</div>
</div>
