<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
    _displayNodeFilters.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Apr 15, 2010 12:45:18 PM
    $Id$
 --%>
<g:set var="varStr" value=""/> <% varStr = '${' %>
<g:if test="${displayParams.filter}">
    <g:set var="filters" value="${com.dtolabs.rundeck.core.utils.NodeSet.parseFilter(displayParams.filter)}"/>
    <g:set var="filtersInc" value="${filters.include}"/>
    <g:set var="filtersExc" value="${filters.exclude}"/>
    <g:each in="${filtersInc.keySet().sort()}" var="qparam">
        <g:if test="${filtersInc[qparam]}">
            <span class="querykey include"><g:enc>${qparam}</g:enc></span>:
            <span class="queryvalue text include ${filtersInc[qparam].contains(varStr) ? 'variable' : ''}">
                <g:truncate max="50"><g:enc>${filtersInc[qparam]}</g:enc></g:truncate></span>
        </g:if>
    </g:each>
    <g:each in="${filtersExc.keySet().sort()}" var="qparam">
        <g:if test="${filtersExc[qparam]}">
            <span class="querykey exclude"><g:enc>${qparam}</g:enc></span>:
            <span class="queryvalue text exclude ${filtersExc[qparam].contains(varStr) ? 'variable' : ''}">
                <g:truncate max="50"><g:enc>${ filtersExc[qparam]}</g:enc></g:truncate></span>
        </g:if>
    </g:each>
</g:if>
<g:else>
<g:each in="${displayParams.properties.keySet().grep{it=~/^(node(Include|Exclude)(?!Precedence).*)$/}.sort()}" var="qparam">
    <g:if test="${displayParams[qparam]}">
    <span class="querykey ${qparam=~/Exclude/?'exclude':'include'}"><g:message code="BaseNodeFilters.title.${qparam}"/></span>:
    <span class="queryvalue text ${qparam=~/Exclude/?'exclude':'include'} ${displayParams[qparam].contains(varStr) ? 'variable' : ''}">
        <g:truncate max="50"><g:message code="${'BaseNodeFilters.title.'+qparam+'.'+displayParams[qparam]}" default="${enc(html: displayParams[qparam]).toString()}"/></g:truncate></span>
    </g:if>
</g:each>
</g:else>
