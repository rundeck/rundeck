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
  <div class="col-xs-12">
    <div class="card card-plain">
      <div class="card-header">
        <h3 class="card-title">
          <g:message code="node.summary" />
        </h3>
      </div>
    </div>
  </div>
  <div class="col-xs-12 col-sm-3">
    <div class="card text-center">
      <div class="card-header">
        <h4 class="card-title">
          <g:message code="complete" />
          <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.complete.description']"/>
        </h4>
      </div>
      <div class="card-content">
        <span class="h3 text-primary" data-bind="text: percentageFixed(completedNodes().length,activeNodes().length) + '%'"></span>
        <span class="text-primary"data-bind="text: completedNodes().length+'/'+activeNodes().length"></span>
      </div>
    </div>
  </div>

  <div class="col-xs-12 col-sm-3">
    <div class="card text-center">
      <div class="card-header">
        <h4 class="card-title">
          <g:message code="status.label.failed" />
          <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.failed.description']"/>
        </h4>
      </div>
      <div class="card-content">
        <span class=" h3" data-bind="css: {'text-danger': failedNodes().length > 0 , 'text-primary': failedNodes().length < 1 } ">
            <span data-bind="text: failedNodes().length"></span>
        </span>
      </div>
    </div>
  </div>


  <div class="col-xs-12 col-sm-3">
    <div class="card text-center">
      <div class="card-header">
        <h4 class="card-title">
          <g:message code="incomplete" />
          <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.incomplete.description']"/>
        </h4>
      </div>
      <div class="card-content">
        <span class=" h3" data-bind="css: {'text-warning': partialNodes().length > 0 , 'text-primary': partialNodes().length < 1 } ">
            <span class="" data-bind="text: partialNodes().length"></span>
        </span>
      </div>
    </div>
  </div>

  <div class="col-xs-12 col-sm-3">
    <div class="card text-center">
      <div class="card-header">
        <h4 class="card-title">
          <g:message code="not.started" />
          <g:render template="/common/helpTooltipIconKO" model="[messageCode: 'workflowState.summary.nodes.notstarted.description']"/>
        </h4>
      </div>
      <div class="card-content">
        <span class=" h3" data-bind="css: {'text-warning': notstartedNodes().length > 0 , 'text-primary': notstartedNodes().length < 1 } ">
            <span class="" data-bind="text: notstartedNodes().length"></span>
        </span>
      </div>
    </div>
  </div>
</div>
