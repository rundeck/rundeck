%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
<%--
    _selectFilter.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Apr 16, 2010 11:17:06 AM
    $Id$
 --%>

<g:if test="${filterList && filterset}">
    <g:set var="projectName" value="${project?:params.project}"/>
    <g:set var="projectfilters" value="${filterset.findAll{it.project==projectName}.sort({ a, b -> a.name.compareTo(b.name) })}"/>
    <g:set var="otherfilters" value="${filterset.findAll{it.project!=projectName}.sort({ a, b -> a.name.compareTo(b.name) })}"/>
    <g:set var="lastproject" value="${null}"/>
    %{--<g:each in="${projectfilters + otherfilters }" var="filter">--}%
        %{--<g:set var="isActive" value="${filter.name == filterName}"/>--}%
        %{--<g:if test="${lastproject && lastproject!=filter.project}">--}%
            %{--<li class="divider"></li>--}%
        %{--</g:if>--}%
        %{--<g:set var="lastproject" value="${filter.project}"/>--}%
        %{--<li>--}%
        %{--<g:link action="nodes" controller="framework" params="[filterName: filter.name, project: projectName]"--}%
                %{--class="${isActive ? 'active' : ''} textbtn textbtn-primary nodefilterlink "--}%
            %{--data-node-filter-name="${enc(attr:filter.name)}"--}%
            %{--data-node-filter="${enc(attr:filter.asFilter())}"--}%
                %{--title="Apply filter: ${enc(attr:filter.name)}">--}%
            %{--${filter.name}<g:if test="${filter.project!=projectName}"> [${filter.project}]</g:if></g:link>--}%
        %{--</li>--}%
    %{--</g:each>--}%
    <!-- ko if: nodeSummary() -->
    <!-- ko foreach: nodeSummary().filters -->
    <li>
        <a class=" nodefilterlink"
        data-bind="text: name(), attr: { 'data-node-filter-name': name(), 'data-node-filter': filter() }, css: { active: name()==$root.filterName() }"
        ></a>
    </li>
    <!-- /ko -->
    <!-- /ko -->
</g:if>
<g:elseif test="${filterLinks && filterset}">
    <i class="glyphicon glyphicon-filter"></i>
    Filters:
    <span class="nav-links">
    <g:each in="${filterset.sort({ a, b -> a.name.compareTo(b.name) })}" var="filter">
        <g:set var="isActive" value="${filter.name == filterName}"/>
        <g:link action="nodes" controller="framework" params="[filterName: filter.name,project:project?:params.project]"
                class="${isActive ? 'active' : ''} textbtn textbtn-primary has_tooltip nodefilterlink "
            data-node-filter-name="${enc(attr:filter.name)}"
            data-node-filter="${enc(attr:filter.asFilter())}"
            data-placement="bottom"
                title="Apply filter: ${enc(attr:filter.name)}">
            <g:enc>${filter.name}</g:enc></g:link>
    </g:each>
    </span>
</g:elseif>
<g:elseif test="${filterset}">
    <g:select name="filterName" optionKey="name" optionValue="name" from="${filterset?filterset.sort({a,b->a.name.compareTo(b.name)}):filterset}" value="${filterName}"
        noSelection="${['':noSelection?noSelection:'-select a filter-']}" onchange="setFilter('${enc(attr:prefName)}',this.value);"/>
</g:elseif>
<g:jsonToken id="filter_select_tokens" url="${request.forwardURI}"/>
