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
<div id="nodeFilter${type}${key}"  style="${query?.('node'+type+key)?'':'display:none;'}" class="nfilteritem">
<span class="input">
    ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:
    <input type='text' name="node${type}${key}"
        value="${query?.('node'+type+key)?.encodeAsHTML()}" id="schedJobNode${type}${key}" onchange="_matchNodes();"/>
    <span title="Remove filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
        class="filterRemove action"
        onclick="_removeNodeFilterInput('${key}',${include?true:false});"
        ><img src="${resource( dir:'images',file:'icon-tiny-removex.png' )}" alt="remove" width="12px" height="12px"/></span>
</span>
    <g:if test="${g.message(code:'node.metadata.'+key+'.defaults',default:'')}">
        <g:select name="_${key}defaults" from="${g.message(code:'node.metadata.'+key+'.defaults').split(',').sort()}" onchange="_setNodeFilterDefault('${key}',${include?true:false},this.value);"/>
    </g:if>
</div>