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

<g:set var="NODE_FILTERS" value="${['','Name','Type','Tags','OsName','OsFamily','OsArch','OsVersion']}"/>
<g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>
        <%--<tr>
            <td>
               Project
            </td>
            <td>
                <g:textField name="project" size="30" value="${query?.project?.encodeAsHTML()}"/>
                <g:if test="${filterErrors?.project}">
                    <span class="error filter">${filterErrors?.project}</span>
                </g:if>
                --><!-- show popup of projects list --><!--
                <g:if test="${session.projects}">
                    <g:select name="project_select" value="${query?.project}" from="${session.projects}" noSelection="${['':'Select a Project...']}"/>
                    <wdgt:eventHandler for="project_select" state="unempty" copy="value" target="project"/>
                </g:if>
            </td>
        </tr>--%>
        <tr>
            <td>
                <span class=" ${hasErrors(bean:query,field:'nodeInclude','fieldError')}">
                    Node Include Filters
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
                    <g:each var="key" in="${NODE_FILTERS}">
                            <div id="nodeFilterInclude${key}"  style="${query?.('nodeInclude'+key)?'':'display:none;'}">
                            <span class="input">
                                ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:
                                <input type='text' name="nodeInclude${key}"
                                    value="${query?.('nodeInclude'+key)?.encodeAsHTML()}" id="schedJobNodeInclude${key}" onchange="_matchNodes()"/>
                                <span title="Remove filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                                    class="filterRemove action"
                                    onclick="_removeNodeFilterInput('${key}',true);"
                                    ><img src="${resource( dir:'images',file:'icon-tiny-removex.png' )}" alt="remove" width="12px" height="12px"/></span>
                            </span>
                                <g:if test="${g.message(code:'node.metadata.'+key+'.defaults',default:'')}">
                                    <g:select from="${g.message(code:'node.metadata.'+key+'.defaults').split(',').sort()}" onchange="_setNodeFilterDefault('${key}',true,this.value);"/>
                                </g:if>
                            </div>
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
            </td>
        </tr>
        <tr>
            <td>
                <span for="nodeExclude" class=" ${hasErrors(bean:query,field:'nodeExclude','fieldError')}">
                    Node Exclude Filters
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
                    <g:each var="key" in="${NODE_FILTERS}">
                            <div id="nodeFilterExclude${key}" style="${query?.('nodeExclude'+key)?'':'display:none;'}">
                            <span class="input">
                                ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:
                                <input type='text' name="nodeExclude${key}"
                                    value="${query?.('nodeExclude'+key)?.encodeAsHTML()}" id="schedJobNodeExclude${key}" onchange="_matchNodes()"/>
                                <span class="filterRemove action"
                                    onclick="_removeNodeFilterInput('${key}',false);"
                                    ><img src="${resource( dir:'images',file:'icon-tiny-removex.png' )}" alt="remove" width="12px" height="12px"/></span>
                            </span>
                                <g:if test="${g.message(code:'node.metadata.'+key+'.defaults',default:'')}">
                                    <g:select from="${g.message(code:'node.metadata.'+key+'.defaults').split(',').sort()}" onchange="_setNodeFilterDefault('${key}',false,this.value);"/>
                                </g:if>
                            </div>
                    </g:each>

                </div>
                <div class="filterSetButtons">
                    <g:each var="key" in="${NODE_FILTERS}">
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
        <tr>
            <td ></td>
            <td >

                <g:render template="/common/nodefilterRegexSyntaxNote"/>
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
    
