%{--
  Copyright 2018 Rundeck, Inc. (http://rundeck.com)

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

<input type='search' name="${filterFieldName?enc(attr:filterFieldName):'filter'}" class="schedJobStepFilter form-control"
       data-bind="textInput: stepFilterValue,  executeOnEnter: filterStepDescriptions"
       placeholder="${queryFieldPlaceholderText?:g.message(code:'enter.a.node.filter')}"
       data-toggle='popover'
       data-popover-content-ref="#${queryFieldHelpId?enc(attr:queryFieldHelpId):'queryFilterHelp'}"
       data-placement="bottom"
       data-trigger="manual"
       data-container="body"
       value="${enc(attr:filtvalue)}" id="${filterFieldId ? enc(attr: filterFieldId) : 'schedJobStepFilter'}"/>


<span class="input-group-btn">
    <a class="btn btn-default" data-toggle='popover-for' data-target="#${filterFieldId ? enc(attr: filterFieldId) : 'schedJobStepFilter'}" onclick="jQuery('#${filterFieldId ? enc(attr: filterFieldId) : 'schedJobStepFilter'}').popover('toggle')">
        <i class="glyphicon glyphicon-question-sign"></i>
    </a>
    <a class="btn btn-default" data-bind="click: filterStepDescriptions" href="#">
        <g:message code="search" />
    </a>
</span>
