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

        <div class="form-group  ${hasErrors(bean: query, field: 'filter', 'has-error')} ${filterErrors?'has-error':''}">

            <g:hiddenField name="formInput" value="true"/>

            <g:hasErrors bean="${query}" field="filter">
                <div class="col-sm-12">
                    <div class="text-warning">
                        <g:renderErrors bean="${query}" as="list" field="filter"/>
                        <i class="glyphicon glyphicon-warning-sign"></i>
                    </div>
                </div>
            </g:hasErrors>
            <div id="nodeFilterDivFilter" style="">
                %{--<label class="control-label col-sm-2"--}%
                       %{--for="schedJobNodeFilter">Filter:</label>--}%
                <g:set var="filtvalue"
                       value="${query?.('filter')?.encodeAsHTML()}"/>
                <div class="col-sm-12 nfilteritem">

                    <div class="input-group">
                        <input type='text' name="filter" class="filterIncludeText form-control"
                            placeholder="Enter a node filter"
                               value="${filtvalue}" id="schedJobNodeFilter" onchange="_matchNodes();"/>

                        <span class="input-group-btn">
                            <a class="btn btn-info" data-toggle='collapse' href="#queryFilterHelp">
                                <i class="glyphicon glyphicon-question-sign"></i>
                            </a>
                        </span>
                    </div>
                </div>
                <div class="col-sm-12 collapse" id="queryFilterHelp">
                    <div class="help-block">
                    <g:render template="/common/nodefilterStringHelp"/>
                    </div>
                </div>
            </div>
        </div>

