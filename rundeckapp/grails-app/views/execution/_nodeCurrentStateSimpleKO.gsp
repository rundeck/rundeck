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

<i class="rdicon icon-small node"></i>
<span class="execstate isnode"
      data-bind="text: name, attr: { 'data-execstate': summaryState() } "></span>

<i class="glyphicon glyphicon-chevron-right text-muted"></i>
<span data-bind="with: currentStep()">
    <a class="stepident action textbtn"
          data-bind="attr: { 'data-execstate': executionState, title: stepctxdesc, href: '#'+stepctx+':'+node.name },  click: $root.scrollToOutput">
        <feature:disabled name="workflowDynamicStepSummaryGUI">
            <span data-bind="template: {name: 'step-info-simple', data:stepinfo(), as: 'stepinfo'}"></span>
        </feature:disabled>
        <feature:enabled name="workflowDynamicStepSummaryGUI">
            <span data-bind="template: {name: 'step-info', data:stepinfo(), as: 'stepinfo'}"></span>
        </feature:enabled>
    </a>
</span>
