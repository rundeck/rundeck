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
    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">Filter <span
            class="caret"></span></button>
    <ul class="dropdown-menu">

        <li>
            <g:link class="nodefilterlink"
                    action="nodes" controller="framework"
                    data-node-filter=".*"
                    data-node-filter-all="true"
                    params="[showall: 'true']">
                Show all nodes
            </g:link>
        </li>
        <li class="divider"></li>
        <li class="dropdown-header"><i class="glyphicon glyphicon-filter"></i> Saved Filters</li>
        <g:if test="${filterset}">
            <g:render template="/common/selectFilter"
                      model="[filterList: true, filterset: filterset, filterName: filterName, prefName: 'nodes', noSelection: filterName ? '-All Nodes-' : null]"/>
        </g:if>
    </ul>
</span>
<input type='search' name="filter" class="schedJobNodeFilter form-control"
       data-bind="value: filterWithoutAll"
       placeholder="Enter a node filter"
       data-toggle='popover'
       data-popover-content-ref="#queryFilterHelp"
       data-placement="bottom"
       data-trigger="manual"
       data-container="body"
       value="${filtvalue}" id="schedJobNodeFilter"/>


<span class="input-group-btn">
    <a class="btn btn-info" data-toggle='popover-for' data-target="#schedJobNodeFilter">
        <i class="glyphicon glyphicon-question-sign"></i>
    </a>
    <button class="btn btn-default" type="submit">
        <g:message code="set.filter"/>
    </button>
</span>
