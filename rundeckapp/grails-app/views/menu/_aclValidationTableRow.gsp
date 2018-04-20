%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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



<div class="grid-row">
    <div class="grid-cell hover-action-holder">
        <g:if test="${editHref}">
            <a href="${editHref}"
               class=" btn btn-link textbtn-info "
               title="Edit">
                <g:if test="${validation.valid}">

                    <g:icon name="file"/>
                </g:if>
                <g:else>

                    <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
                       title="${message(code: "aclpolicy.format.validation.failed")}"></i>
                </g:else>

                ${policyFile}

                <span class="xhover-action text-info">
                    <g:icon name="edit"/>
                    <g:message code="edit"/>
                </span>
            </a>
        </g:if>
        <g:else>
            <span class="${validation.valid ? '' : 'text-warning'}">
                <g:icon name="file"/>
                ${policyFile}
            </span>
        </g:else>
        <g:if test="${flashMessage}">
            <span class="badge badge-default flash_info">
                ${flashMessage}
            </span>
        </g:if>
    </div>
    <div class="grid-cell">
        <g:set var="akey" value="${g.rkey()}"/>
        <g:if test="${!validation.valid}">
            <g:expander key="${akey}">
                <span class="text-warning">Validation Error</span>
            </g:expander>

        </g:if>
    </div>
</div>

<g:if test="${!validation.valid}">
    <div class="" id="${akey}" style="display: none">
        <div style="display: inline-block">
            <div class="well well-sm inline">
                <g:render template="aclValidationReport"
                          model="${[validation: validation, documentPrefix: (prefix ?: '') + policyFile]}"/>
            </div>
        </div>
    </div>
</g:if>