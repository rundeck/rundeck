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

        <div class="form-group  ${hasErrors(bean: query, field: 'filter', 'has-error')} ${filterErrors?'has-error':''}">

            <g:hiddenField name="formInput" value="true"/>


            <div id="nodeFilterDivFilter" style="">
                <g:set var="filtvalue"
                       value="${enc(html: query?.('filter'))}"/>
                <div class="col-sm-12 nfilteritem">

                    <div class="input-group">
                        <input type='text' name="filter" class="form-control schedJobNodeFilter"
                            placeholder="Enter a node filter"
                               value="${enc(attr:filtvalue)}" id="schedJobNodeFilter" onchange="_matchNodes();"/>

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

