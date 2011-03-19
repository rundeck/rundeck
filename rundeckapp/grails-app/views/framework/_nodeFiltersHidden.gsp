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

 --%> <%--
    _nodeFilterHidden.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Apr 14, 2010 4:07:58 PM
    $Id$
 --%>
<g:set var="NODE_FILTERS" value="${['','Name','Tags','OsName','OsFamily','OsArch','OsVersion']}"/>
<g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>
<g:hiddenField name="formInput" value="true"/>


<g:each var="key" in="${NODE_FILTERS}">
    <g:if test="${query?.('nodeInclude'+key)}">
        <input type='hidden' name="nodeInclude${key}"
            value="${query?.('nodeInclude'+key)?.encodeAsHTML()}" />
    </g:if>
    <g:if test="${query?.('nodeExclude'+key)}">
            <input type='hidden' name="nodeExclude${key}"
                value="${query?.('nodeExclude'+key)?.encodeAsHTML()}" />
    </g:if>
</g:each>
<input type="hidden" name="nodeExcludePrecedence" value="${query?.nodeExcludePrecedence}" />
