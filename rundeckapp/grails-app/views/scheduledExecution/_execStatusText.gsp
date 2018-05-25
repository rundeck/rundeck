    by %{--
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

<g:username user="${execution.user}"/>
    <g:if test="${execution.dateCompleted == null}">
        <span class="isnode execstate execstatedisplay overall" data-bind="attr: { 'data-execstate': executionState() } ">
        </span>
    </g:if>
    <g:else>
        <span class="exec-status ${execution.statusSucceeded() ? 'succeed' : execution.cancelled?'warn': execution.customStatusString?'other':'fail'}">

            <g:if test="${execution.customStatusString}">
                "${execution.customStatusString}"
            </g:if>
            <g:else>
                <g:message code="status.label.${execution.executionState}"/>
                <g:if test="${execution.cancelled}">
                    <g:if test="${execution.abortedby}"> by <g:username user="${execution.abortedby}"/></g:if>
                </g:if>
            </g:else>

        </span>
    </g:else>
