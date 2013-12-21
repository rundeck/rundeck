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

        <div class="form-group  ${hasErrors(bean: query, field: 'nodeInclude', 'has-error')}">
            <div class="col-sm-12">
            <span class="h4 ">
                Include Nodes Matching:
            </span>

            <g:render template="/common/nodefilterRegexSyntaxNote"/>
            </div>


            <g:hiddenField name="formInput" value="true"/>

            <g:hasErrors bean="${query}" field="nodeInclude">
                <div class="col-sm-12">
                <div class="text-warning">
                    <g:renderErrors bean="${query}" as="list" field="nodeInclude"/>
                    <i class="glyphicon glyphicon-warning-sign"></i>
                </div>
                </div>
            </g:hasErrors>
            <div id="nodeFilterDivInclude" style="">
                <g:each var="key" in="${NODE_FILTERS_ALL}">
                    <g:render template="nodeFilterField" model="${[key:key,include:true,query:query,NODE_FILTER_MAP:NODE_FILTER_MAP]}"/>
                </g:each>
            </div>
            <div class="row">
                <div class="col-sm-10 col-sm-offset-2">
                <g:each var="key" in="${NODE_FILTERS}">
                    <span
                        style="${query?.('nodeInclude'+key)?'display:none':''}"
                        title="Add Filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                        class="filterAdd btn btn-default btn-sm"
                        id="filterAddInclude${key}"
                        onclick="_addNodeFilterInput('${key}',true,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                        >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                </g:each>
                    <span class="filterAdd btn btn-default btn-sm" onclick="Element.show('${rkey}moreIncludeFilters');
                    Element.hide(this);">more&hellip;</span>
            </div>

            <div class=" col-sm-10 col-sm-offset-2" id="${rkey}moreIncludeFilters" style="display:none">
                <g:each var="key" in="${NODE_FILTERS_X}">
                    <span
                        style="${query?.('nodeInclude'+key)?'display:none':''}"
                        title="Add Filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                        class="filterAdd btn btn-default btn-sm"
                        id="filterAddInclude${key}"
                        onclick="_addNodeFilterInput('${key}',true,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                        >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                </g:each>
            </div>
            </div>
        </div>

<div class="row">
    <div class="col-sm-12">
        <g:expander key="${rkey}nodeXtraFilters">Extended Filters&hellip;</g:expander>
    </div>
</div>%{--//extended filters toggle--}%
    <div id="${rkey}nodeXtraFilters" style="display:none" class="subfields">
        <div class="form-group">
            <div class="col-sm-12">
            <span class="h5 ${hasErrors(bean: query, field: 'nodeExclude', 'has-error')}">
                Exclude Nodes Matching:
            </span>
            </div>


                <g:hasErrors bean="${query}" field="nodeExclude">
                    <div class="col-sm-12">
                    <div class="has-error">
                        <g:renderErrors bean="${query}" as="list" field="nodeExclude"/>
                        <i class="glyphicon glyphicon-warning-sign"></i>
                    </div>
                    </div>
                </g:hasErrors>


            <div id="nodeFilterDivExclude" style="">
                <g:each var="key" in="${NODE_FILTERS_ALL}">
                    <g:render template="nodeFilterField" model="${[key:key,include:false,query:query,NODE_FILTER_MAP:NODE_FILTER_MAP]}"/>
                </g:each>
            </div>
            <div class="row">
            <div class="  col-sm-10 col-sm-offset-2">
                <g:each var="key" in="${NODE_FILTERS_ALL}">
                        <span
                            style="${query?.('nodeExclude'+key)?'display:none':''}"
                        title="Add Filter: ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                        class="filterAdd btn btn-default btn-sm"
                        id="filterAddExclude${key}"
                        onclick="_addNodeFilterInput('${key}',false,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                        >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                </g:each>
            </div>
            </div>
        </div>
        <g:if test="${filterErrors?.filter}">
            <div class="row">
                <div class="col-sm-12">
                    <span class="error filter">${filterErrors?.filter}</span>
                </div>
            </div>
        </g:if>
        <div class="form-group">
            <label class="col-sm-2  control-label">Precedence to:</label>

            <div class="col-sm-10">
                <label title="Include more nodes" class="radio-inline">
                    <g:radio name="nodeExcludePrecedence" value="false"
                             checked="${!query?.nodeExcludePrecedence}"
                             id="nodeExcludePrecedenceFalse" onchange="_matchNodes()"/>
                    Included</label>

                <label title="Exclude more nodes" class="radio-inline">
                    <g:radio name="nodeExcludePrecedence" value="true"
                             checked="${query?.nodeExcludePrecedence}"
                             id="nodeExcludePrecedenceTrue" onchange="_matchNodes()"/>
                    Excluded</label>
            </div>
        </div>
    </div>
