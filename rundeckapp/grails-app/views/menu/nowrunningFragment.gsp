%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

<g:set var="rkey" value="${g.rkey()}" />
<g:render template="runningExecutions" model="[executions:nowrunning,jobs:jobs,nowrunning:true,idprefix:rkey,emptyText:'']"/>
<g:if test="${total>max}">
    <tr>
    <td colspan="5">
    <span class="paginate nowrunning"><g:paginate class="pagination-sm pagination-embed" action="nowrunning" total="${total}"  max="${max}"/></span>
        <g:if test="${total && max && total.toInteger() > max.toInteger()}">
            <span class="text-muted">Showing <g:enc>${nowrunning.size()} of ${total}</g:enc></span>
        </g:if>
    </td>
    </tr>
</g:if>
<g:render template="/common/boxinfo" model="${[name:'nowrunning',model:[title:'Now Running',total:total,lastExecId:lastExecId,count:nowrunning.size()]]}"/>
