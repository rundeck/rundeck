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
<span class="input-group-btn">
    <button type="button"
            class="btn btn-default dropdown-toggle"
        data-bind="css: { 'btn-success': filterName(), 'btn-default': !filterName() }"
            data-toggle="dropdown">
        <span data-bind="text: filterNameDisplay() || ''"><g:message code="filter.title" /></span> <span class="caret"></span></button>
    <ul class="dropdown-menu">

        <li>
            <g:link class="nodefilterlink"
                    action="nodes" controller="framework"
                    data-node-filter-name=".*"
                    data-node-filter=".*"
                    data-bind="css: { active: '.*'== filterName() }"
                    params="[filterName: '.*']">
                <g:message code="show.all.nodes" />
            </g:link>
        </li>
        <li class="divider"></li>
        <li class="dropdown-header"><i class="glyphicon glyphicon-filter"></i> <g:message code="saved.filters" /></li>
        <g:render template="/common/selectFilter"
                  model="[filterList: true, filterset: filterset, filterName: filterName, prefName: 'nodes', noSelection: filterName ? message(code:'all.nodes.menu.item') : null]"/>
    </ul>
</span>
<input type='search' name="${filterFieldName?enc(attr:filterFieldName):'filter'}" class="schedJobNodeFilter form-control"
       data-bind="textInput: filterWithoutAll,  executeOnEnter: newFilterText"
       placeholder="${queryFieldPlaceholderText?:g.message(code:'enter.a.node.filter')}"
       data-toggle='popover'
       data-popover-content-ref="#${queryFieldHelpId?enc(attr:queryFieldHelpId):'queryFilterHelp'}"
       data-placement="bottom"
       data-trigger="manual"
       data-container="body"
       value="${enc(attr:filtvalue)}" id="${filterFieldId ? enc(attr: filterFieldId) : 'schedJobNodeFilter'}"/>


<span class="input-group-btn">
    <a class="btn btn-default" data-toggle='popover-for' data-target="#${filterFieldId ? enc(attr: filterFieldId) : 'schedJobNodeFilter'}" onclick="jQuery('#${filterFieldId ? enc(attr: filterFieldId) : 'schedJobNodeFilter'}').popover('toggle')">
        <i class="glyphicon glyphicon-question-sign"></i>
    </a>
    <a class="btn btn-default" data-bind="click: $data.newFilterText, css: {disabled: !filter()}" href="#">
        <g:message code="search" />
    </a>
</span>
