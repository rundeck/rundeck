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

  <div class="row">
    <div class="col-xs-12 col-sm-4">
      <g:message code="complete" />:
      <strong><span class="text-primary" data-bind="text: percentageFixed(completedNodes().length,activeNodes().length) + '%'"></span></strong>
      <strong><span class="text-primary"data-bind="text: completedNodes().length+'/'+activeNodes().length"></span></strong>
      <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.complete.description']"/>
    </div>
    <div class="col-xs-12 col-sm-2">
      <g:message code="status.label.failed" />:
      <span data-bind="css: {'text-danger': failedNodes().length > 0 , 'text-primary': failedNodes().length < 1 } ">
          <strong><span data-bind="text: failedNodes().length"></span></strong>
      </span>
      <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.failed.description']"/>
    </div>
    <div class="col-xs-12 col-sm-3">
      <g:message code="incomplete" />:
      <span data-bind="css: {'text-warning': partialNodes().length > 0 , 'text-primary': partialNodes().length < 1 } ">
        <strong><span class="" data-bind="text: partialNodes().length"></span></strong>
      </span>
      <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.incomplete.description']"/>
    </div>
    <div class="col-xs-12 col-sm-3">
      <g:message code="not.started" />:
      <span data-bind="css: {'text-warning': notstartedNodes().length > 0 , 'text-primary': notstartedNodes().length < 1 } ">
        <strong><span class="" data-bind="text: notstartedNodes().length"></span></strong>
      </span>
      <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.notstarted.description']"/>
    </div>
  </div>
