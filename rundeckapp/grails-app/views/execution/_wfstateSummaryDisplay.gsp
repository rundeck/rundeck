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
<div>
    <script type="text/html" id="node-current-state-simple">
    <g:render template="nodeCurrentStateSimpleKO"/>
    </script>

    <div data-bind="if: !stateLoaded()">
        <div data-bind="if: errorMessage()">
            <div class="well well-lg" data-bind="visible: errorMessage()" style="display: none">
                <span class="text-warning" data-bind="text: errorMessage()">
                </span>

                <div>
                    <a class="btn btn-default btn-sm" href="#output"
                       data-bind="click: activeTab('output')"><g:message
                            code="button.action.view.log.output"/></a>
                </div>
            </div>
        </div>

        <div data-bind="if: !errorMessage() && !statusMessage()">
            <div class="well well-lg text-strong">
                <g:message code="waiting.for.state.info"/>
            </div>
        </div>

        <div data-bind="if: statusMessage()">
            <div class="well well-lg text-strong" data-bind="text: statusMessage()"></div>
        </div>
    </div>


    <div data-bind="if: stateLoaded()">
        <div data-bind="if: completed()">
            <tmpl:wfstateSummaryScore/>
        </div>

        <div class="row jobstats" data-bind="if: !completed()">
            <div class="col-xs-12 col-sm-4 job-stats-item">

                <span class="job-stats-value">
                    <span class="text-strong" data-bind="text: waitingNodes().length"></span>
                </span>

                <span class="text-table-header has_tooltip"
                      title="${enc(attr: g.message(code: 'workflowState.summary.nodes.waiting.description'))}"
                      data-container="body"
                      data-bind="bootstrapTooltip: true ">
                    <g:message code="waiting"/>

                </span>

            </div>

            <div class="col-xs-12 col-sm-4 job-stats-item">

                <span class="job-stats-value">
                    <span
                            data-bind="css: {'text-info': runningNodes().length > 0 , 'text-strong': runningNodes().length < 1 } ">
                        <span class=" " data-bind="text: runningNodes().length"></span>
                    </span>
                </span>

                <span class="text-table-header has_tooltip"
                      title="${enc(attr: g.message(code: 'workflowState.summary.nodes.running.description'))}"
                      data-container="body"
                      data-bind="bootstrapTooltip: true ">
                    <g:message code="running"/>
                </span>
                
            </div>

            <div class="col-xs-12 col-sm-4 job-stats-item">

                <span class="job-stats-value">
                    <span
                            data-bind="css: {'text-info': completedNodes().length > 0 , 'text-strong': completedNodes().length < 1 } ">
                        <span data-bind="text: completedNodes().length"></span>
                    </span>
                </span>

                <span class="text-table-header has_tooltip"
                      title="${enc(attr: g.message(code: 'workflowState.summary.nodes.complete.description'))}"
                      data-container="body"
                      data-bind="bootstrapTooltip: true ">
                    <g:message code="done"/>

                </span>

            </div>
        </div>
    </div>
</div>
