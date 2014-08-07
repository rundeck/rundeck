<table cellpadding="0" cellspacing="0" width="100%">

        %{--
  - Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<% def seen=false %>
        <% def i =0 %>
        <g:each in="${nodes}" var="node">
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>

            <tr class="${i%2==1?'alternateRow':''} node_entry ">
                <td class="objIdent" colspan="3">
                <span class="node_ident" id="${enc(attr:node.nodename)}_key">
                    <i class="rdicon node icon-small"></i>
                    ${enc(html:resName)}
                </span>
                    <g:if test="${totalexecs[node.nodename]}">
                        (${enc(html:totalexecs[node.nodename])})
                    </g:if>
                    
                    <span  class="objdesc">
                        ${enc(html:resHost)}
                    </span>
                    <g:render template="nodeTooltipView" model="[node:node,key:node.nodename+'_key']"/>
                </td>
                
            </tr>
            <% i++ %>
        </g:each>
</table>

<g:javascript>
    if(typeof(initTooltipForElements)=='function'){
        initTooltipForElements('tr.node_entry span.node_ident');
    }
</g:javascript>
