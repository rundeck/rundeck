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
<g:if test="${showInputTitle}">
    <span class="input-group-addon input-group-addon-title"><g:message code="nodes" /></span>
</g:if>
<div class="input-group-btn">
    <button type="button" class="btn btn-default dropdown-toggle" data-bind="css: { 'btn-success': filterName(), 'btn-default': !filterName() }" data-toggle="dropdown">
        <span data-bind="text: filterNameDisplay() || ''"><g:message code="filter.title" /></span> <span class="caret"></span>
    </button>
    <ul class="dropdown-menu">
        <li>
            <g:link class="nodefilterlink"
                    action="nodes" controller="framework"
                    data-node-filter-name=".*"
                    data-node-filter=".*"
                    data-bind="css: { active: '.*'== filterName() }"
                    params="[filterName: '.*']">
                <i class="fas fa-asterisk"></i>
                <g:message code="show.all.nodes" />
            </g:link>
        </li>

        <li class="divider"></li>

        <li class="dropdown-header" data-bind="visible: filterName()">
            <g:message code="filter" /><span data-bind="text: filterNameDisplay()"></span>
        </li>
        <li data-bind="visible: canSaveFilter">
            <a href="#"
               data-toggle="modal"
               data-target="#saveFilterModal">
                <i class="glyphicon glyphicon-plus"></i>
                <g:message code="save.filter.ellipsis" />
            </a>
        </li>
        <li data-bind="visible: canDeleteFilter">
            <a href="#"
               class=""
               data-bind="click: deleteFilter">
                <i class="glyphicon glyphicon-remove"></i>
                <g:message code="delete.this.filter.ellipsis" />
            </a>
        </li>
        <li data-bind="visible: canSetDefaultFilter">
            <a href="#"
               class=""
               data-bind="click: setDefaultFilter">
                <i class="glyphicon glyphicon-filter"></i>
                <g:message code="set.as.default.filter" />
            </a>
        </li>
        <li data-bind="visible: canRemoveDefaultFilter">
            <a href="#"
               class=""
               data-bind="click: nodeSummary().removeDefault">
                <i class="glyphicon glyphicon-ban-circle"></i>
                <g:message code="remove.default.filter" />
            </a>
        </li>

        <!-- ko if: nodeSummary() -->
        <!-- ko if: nodeSummary().filters().length > 0 -->
        <li class="divider"></li>
        <li class="dropdown-header"> <g:message code="saved.filters" /></li>
        %{--    <g:render template="/common/selectFilter" model="[className: 'nodefilterlink',filterList: true, filterset: filterset, filterName: filterName, prefName: 'nodes', noSelection: filterName ? message(code:'all.nodes.menu.item') : null]"/>--}%
        <!-- /ko -->
        <!-- ko foreach: nodeSummary().filters -->
        <li>
            <a  class="nodefilterlink"
                data-bind="text: name(), attr: { 'data-node-filter-name': name(), 'data-node-filter': filter() }, css: { active: name()==$root.filterName() }"
            ></a>
        </li>
        <!-- /ko -->
        <!-- /ko -->
    </ul>
</div>

<g:jsonToken id="filter_select_tokens" url="${request.forwardURI}"/>
<input type='search'
       name="${filterFieldName?enc(attr:filterFieldName):'filter'}"
       class="schedJobNodeFilter form-control"
    ${autofocus?'autofocus':''}
       data-bind="textInput: filterWithoutAll,  executeOnEnter: newFilterText"
       placeholder="${queryFieldPlaceholderText?:g.message(code:'enter.a.node.filter')}"
       data-toggle='popover'
       data-popover-content-ref="#${queryFieldHelpId?enc(attr:queryFieldHelpId):'queryFilterHelp'}"
       data-placement="bottom"
       data-trigger="manual"
       data-container="body"
       value="${enc(attr:filtvalue)}" id="${filterFieldId ? enc(attr: filterFieldId) : 'schedJobNodeFilter'}"/>
<div class="input-group-btn">
    <a class="btn btn-default" data-toggle='popover-for' data-target="#${filterFieldId ? enc(attr: filterFieldId) : 'schedJobNodeFilter'}" onclick="jQuery('#${filterFieldId ? enc(attr: filterFieldId) : 'schedJobNodeFilter'}').popover('toggle')">
        <i class="glyphicon glyphicon-question-sign"></i>
    </a>
    <a class="btn btn-primary btn-fill" data-bind="click: $data.newFilterText, css: {disabled: !filter()}" href="#">
        <g:message code="search" />
    </a>
</div>
