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
<g:each in="${displayParams.properties.keySet().grep{it=~/^(project|node(Include|Exclude)(?!Precedence).*)$/}.sort()}" var="qparam">
    <g:if test="${displayParams[qparam]}">
    <span class="querykey ${qparam=~/Exclude/?'exclude':'include'}"><g:message code="BaseNodeFilters.title.${qparam}"/></span>:
    <span class="queryvalue text ${qparam=~/Exclude/?'exclude':'include'}">
        <g:truncate max="50" title="${displayParams[qparam].toString().encodeAsHTML()}"><g:message code="${'BaseNodeFilters.title.'+qparam+'.'+displayParams[qparam]}" default="${displayParams[qparam].toString().encodeAsHTML()}"/></g:truncate></span>
    </g:if>
</g:each>