%{--
  Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

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

%{--Filter navigation/selection dropdown--}%
<div class="input-group-btn">
  <button type="button" class="btn btn-default dropdown-toggle" data-bind="css: { 'btn-success': filterExcludeName(), 'btn-default': !filterExcludeName() }" data-toggle="dropdown">
    <span data-bind="text: filterExcludeNameDisplay() || ''"><g:message code="filter.title" /></span> <span class="caret"></span>
  </button>
  <ul class="dropdown-menu">
    <li class="dropdown-header"><i class="glyphicon glyphicon-filter"></i> <g:message code="saved.filters" /></li>
    <g:render template="/common/selectFilter" model="[className: 'nodeexcludefilterlink',filterList: true, filterset: filterset, filterName: filterName, prefName: 'nodes', noSelection: filterName ? message(code:'all.nodes.menu.item') : null]"/>
  </ul>
</div>

<input type='search' name="${filterFieldName?enc(attr:filterFieldName):'filterExclude'}" class="schedJobNodeFilter form-control"
       data-bind="textInput: filterExcludeWithoutAll,  executeOnEnter: newFilterText"
       placeholder="${queryFieldPlaceholderText?:g.message(code:'enter.a.node.filter')}"
       data-toggle='popover'
       data-popover-content-ref="#${queryFieldHelpId?enc(attr:queryFieldHelpId):'queryFilterHelp'}"
       data-placement="bottom"
       data-trigger="manual"
       data-container="body"
       value="${enc(attr:filtvalue)}" id="schedJobNodeFilter"/>
<div class="input-group-btn">
  <a class="btn btn-warning btn-fill" data-bind="click: $data.newFilterText, css: {disabled: !filter()}" href="#">
    <g:message code="search" />
  </a>
</div>
