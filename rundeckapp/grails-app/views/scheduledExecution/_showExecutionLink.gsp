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

<g:link
        controller="execution"
        action="show"
        class="primary ${linkCss?:''}"
        id="${execution.id}"
        absolute="${absolute ? 'true' : 'false'}"
        params="${(followparams?.findAll { it.value }?:[:]) + [project: execution.project]}">
    <g:if test="${execution}">
        <i class="exec-status icon "
           data-bind="attr: { 'data-execstate': executionState, 'data-statusstring':executionStatusString }">
        </i>
    </g:if>
    <g:if test="${scheduledExecution}">
        <span class="primary"><g:message code="scheduledExecution.identity"
                                         args="[scheduledExecution.jobName, execution.id]"/></span>
    </g:if>
    <g:else>
        <span class="primary"><g:message code="execution.identity" args="[execution.id]"/></span>
    </g:else>

</g:link>
