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
    _nodeFilterInputs.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Apr 14, 2010 4:07:58 PM
    $Id$
 --%>
<g:set var="rkey" value="${rkey()}"/>
<g:javascript>
    function _addNodeFilterInput(name, isinclude, label) {
        var prefix = (isinclude ? 'Include' : 'Exclude');
        if ($('nodeFilter' + prefix + name)) {
            $('nodeFilter' + prefix + name).show();
            if ($('filterAdd' + prefix + name)) {
                $('filterAdd' + prefix + name).hide();
            }
            return;
        }
    }

    function _setNodeFilterDefault(name, isinclude, value) {
        var prefix = (isinclude ? 'Include' : 'Exclude');
        if ($('schedJobNode' + prefix + name)) {
            $('schedJobNode' + prefix + name).setValue(value);
            _matchNodes();
        }
    }
    function _removeNodeFilterInput(name, isinclude) {
        var prefix = (isinclude ? 'Include' : 'Exclude');
        if ($('nodeFilter' + prefix + name)) {
            $('nodeFilter' + prefix + name).hide();
            if ($('schedJobNode' + prefix + name)) {
                $('schedJobNode' + prefix + name).setValue('');
                if ($('filterAdd' + prefix + name)) {
                    $('filterAdd' + prefix + name).show();
                }
                _matchNodes();
                return;
            }
        }
    }
</g:javascript>

<g:set var="NODE_FILTERS_ALL" value="${['Name','Tags','','OsName','OsFamily','OsArch','OsVersion']}"/>
<g:set var="NODE_FILTERS" value="${['Name','Tags']}"/>
<g:set var="NODE_FILTERS_X" value="${['','OsName','OsFamily','OsArch','OsVersion']}"/>
<g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>
        <tr>
            <td>
                <span class=" ${hasErrors(bean:query,field:'nodeInclude','fieldError')}">
                    Include
                </span>
            </td>
            <td>
                <g:hiddenField name="formInput" value="true"/>

                <g:hasErrors bean="${query}" field="nodeInclude">
                    <div class="fieldError">
                        <g:renderErrors bean="${query}" as="list" field="nodeInclude"/>
                    </div>
                </g:hasErrors>
                <g:hasErrors bean="${query}" field="nodeInclude">
                    <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
                </g:hasErrors>
                <div id="nodeFilterDivInclude" style="">
                    <g:each var="key" in="${NODE_FILTERS_ALL}">
                        <g:render template="nodeFilterField" model="${[key:key,include:true,query:query,NODE_FILTER_MAP:NODE_FILTER_MAP]}"/>
                    </g:each>
                </div>
                <div class="filterSetButtons">
                    <g:each var="key" in="${NODE_FILTERS}">
                        <span
                            style="${query?.('nodeInclude'+key)?'display:none':''}"
                            title="Add Filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                            class="filterAdd button textbtn action"
                            id="filterAddInclude${key}"
                            onclick="_addNodeFilterInput('${key}',true,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                            >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                    </g:each>
                </div>
                <span class="filterAdd button textbtn action" onclick="Element.show('${rkey}moreIncludeFilters');Element.hide(this);">more&hellip;</span>
                <div class="filterSetButtons" id="${rkey}moreIncludeFilters" style="display:none">
                    <g:each var="key" in="${NODE_FILTERS_X}">
                        <span
                            style="${query?.('nodeInclude'+key)?'display:none':''}"
                            title="Add Filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                            class="filterAdd button textbtn action"
                            id="filterAddInclude${key}"
                            onclick="_addNodeFilterInput('${key}',true,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                            >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                    </g:each>
                </div>
            </td>
        </tr>
        <tr>
            <td ></td>
            <td >

                <g:render template="/common/nodefilterRegexSyntaxNote"/>
            </td>
        </tr>
        <tr>
            <td colspan="2" style="text-align:left">
                <g:expander key="${rkey}nodeXtraFilters">
                    Extended Filters...
                </g:expander>
            </td>
        </tr>
    <tbody id="${rkey}nodeXtraFilters" style="display:none">
        <tr>
            <td>
                <span class=" ${hasErrors(bean:query,field:'nodeExclude','fieldError')}" >
                    Exclude
                </span>
            </td>
            <td>

                <div>
                    <g:hasErrors bean="${query}" field="nodeExclude">
                    <div class="fieldError">
                        <g:renderErrors bean="${query}" as="list" field="nodeExclude"/>
                    </div>
                </g:hasErrors>
                    <g:hasErrors bean="${query}" field="nodeExclude">
                        <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
                    </g:hasErrors>
                </div>

                <div id="nodeFilterDivExclude" style="">
                    <g:each var="key" in="${NODE_FILTERS_ALL}">
                        <g:render template="nodeFilterField" model="${[key:key,include:false,query:query,NODE_FILTER_MAP:NODE_FILTER_MAP]}"/>
                    </g:each>

                </div>
                <div class="filterSetButtons">
                    <g:each var="key" in="${NODE_FILTERS_ALL}">
                            <span
                                style="${query?.('nodeExclude'+key)?'display:none':''}"
                            title="Add Filter: ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                            class="filterAdd button textbtn action"
                            id="filterAddExclude${key}"
                            onclick="_addNodeFilterInput('${key}',false,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                            >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                    </g:each>
                </div>
            </td>
        </tr>
        <g:if test="${filterErrors?.filter}">
            <tr>
                <td></td>
                <td>
                    <span class="error filter">${filterErrors?.filter}</span>
                </td>
            </tr>
        </g:if>
        <tr>
            <td>Exclude Filters have precedence?</td>
            <td>
                <g:radio
                    name="nodeExcludePrecedence"
                    value="false"
                    checked="${!query?.nodeExcludePrecedence}"
                    id="nodeExcludePrecedenceFalse" onchange="_matchNodes()"
                    />
                <label for="nodeExcludePrecedenceFalse">
                    <span class="action " id="nodeExcludePrecedenceFalseLabel" >No</span></label>

                <g:radio
                    name="nodeExcludePrecedence"
                    value="true"
                    checked="${query?.nodeExcludePrecedence}"
                    id="nodeExcludePrecedenceTrue" onchange="_matchNodes()"
                    />
                <label for="nodeExcludePrecedenceTrue">
                    <span class="action " id="nodeExcludePrecedenceTrueLabel" >Yes</span></label>
            </td>
        </tr>
    </tbody>