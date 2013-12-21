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
    _nodeFilterField.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Oct 27, 2010 5:04:24 PM
 --%>
<g:set var="type" value="${include?'Include':'Exclude'}"/>
<g:set var="predefinedDefaults" value="${g.message(code: 'node.metadata.' + key + '.defaults', default: '')}"/>
<div id="nodeFilter${type}${key}"  style="${query?.('node'+type+key)?'':'display:none;'}"
     class="nodefilterfield form-group">
<span class="input">
    <label class="control-label col-sm-2"
           for="schedJobNodeInclude${key}">${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}:</label>
    <g:set var="filtvalue"
           value="${query?.('node' + type + key)?.encodeAsHTML()}"/>

    <div class="${predefinedDefaults ? 'col-sm-4' : 'col-sm-6'}">
        <input type='text' name="node${type}${key}" class="filterIncludeText form-control input-sm"
               value="${filtvalue}" id="schedJobNodeInclude${key}" onchange="_matchNodes();"/>
    </div>
    <g:if test="${predefinedDefaults}">
        <div class="col-sm-2">
            <g:select from="${predefinedDefaults.split(',').sort()}"
                      onchange="setFilter('${key}',true,this.value);_matchNodesKeyPress();"
                      class="form-control col-sm-2 input-sm"
                      name="_${key}defaults"/>
        </div>
    </g:if>

    <div class="col-sm-4">
        <span title="Remove filter for ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}"
              class="filterRemove action textbtn textbtn-danger form-control-static"
              onclick="_removeNodeFilterInput('${key}', ${include?true:false});">
            <i class="glyphicon glyphicon-remove"></i>
            remove
        </span>
    </div>
</span>
</div>
