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

<div class="row jobstats">
    <div class="col-xs-12 col-sm-6 job-stats-item">
        <span class="text-table-header has_tooltip"
              title="${enc(attr: g.message(code: 'workflowState.summary.nodes.complete.description'))}"
              data-container="body"
              data-bind="bootstrapTooltip: true ">
            <g:message code="complete"/>

        </span>

        <div class="job-stats-value">
            <span
                  data-bind="text: percentageFixed(completedNodes().length,activeNodes().length) + '%'"></span>

            <span class="text-primary"
                  data-bind="text: completedNodes().length+'/'+activeNodes().length"></span>

        </div>

    </div>

    <div class="col-xs-12 col-sm-6 job-stats-item">
        <span class="text-table-header"
              title="${message(code: 'workflowState.summary.nodes.failed.description')}"
              data-container="body"
              data-bind="bootstrapTooltip: true">
            <g:message code="status.label.failed"/>
        </span>

        <div class="job-stats-value">
            <span
                    data-bind="css: {'text-danger': failedNodes().length > 0 , 'text-secondary': failedNodes().length < 1 } ">
                <span data-bind="text: failedNodes().length"></span>
            </span>
        </div>
    </div>

    <div class="col-xs-12 col-sm-6 job-stats-item">
        <span class="text-table-header"
              title="${message(code: 'workflowState.summary.nodes.incomplete.description')}"
              data-container="body"
              data-bind="bootstrapTooltip: true">
            <g:message code="incomplete"/>
        </span>

        <div class="job-stats-value">
            <span data-bind="css: {'text-warning': partialNodes().length > 0 , 'text-secondary': partialNodes().length < 1 } ">
                <span class="" data-bind="text: partialNodes().length"></span>
            </span>
        </div>
    </div>

    <div class="col-xs-12 col-sm-6 job-stats-item">
        <span class="text-table-header"
              title="${message(code: 'workflowState.summary.nodes.notstarted.description')}"
              data-container="body"
              data-bind="bootstrapTooltip: true">
            <g:message code="not.started"/>
        </span>

        <div class="job-stats-value">
            <span data-bind="css: {'text-warning': notstartedNodes().length > 0 , 'text-secondary': notstartedNodes().length < 1 } ">
                <span class="" data-bind="text: notstartedNodes().length"></span>
            </span>
        </div>
    </div>
</div>
