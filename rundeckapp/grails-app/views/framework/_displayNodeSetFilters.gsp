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
Display values of a NodeSet
 --%>
<g:set var="varStr" value=""/> <% varStr='${' %>
<g:each in="${com.dtolabs.rundeck.core.utils.NodeSet.FILTER_ENUM.values().sort{a,b->a.name<=>b.name}}" var="qparam">
<g:each in="${['include','exclude']}" var="clusion">
    <g:set var="value" value="${qparam.value(nodeset[clusion])}"/>
    <g:if test="${value}">
        <span class="querykey ${enc(attr:clusion)}"><g:message
                code="BaseNodeFilters.title.${qparam.name}" default="${qparam.name}"/></span>:
        <span class="queryvalue text ${enc(attr:clusion)} ${value.contains(varStr)?'variable':''}">
            <g:truncate max="50"><g:enc>${value}</g:enc></g:truncate></span>
    </g:if>
</g:each>
</g:each>
