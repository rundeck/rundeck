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
<li>
    <g:if test="${validation.valid}">
        <i class="glyphicon glyphicon-ok text-success has_tooltip"
           title="${message(code: "aclpolicy.format.validation.succeeded")}"></i>
    </g:if>
    <g:else>
        <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
           title="${message(code: "aclpolicy.format.validation.failed")}"></i>
    </g:else>
    <span class="${validation.valid ? '' : 'text-warning'}">${policyFile}</span>
    <g:if test="${editHref}">
        <a href="${editHref}" class="btn btn-simple btn-sm">
            <g:icon name="edit"/>
            <g:message code="edit.file"/>
        </a>
    </g:if>
    <g:if test="${flashMessage}">
        <span class="badge badge-default flash_info">
            ${flashMessage}
        </span>
    </g:if>
    <g:set var="akey" value="${g.rkey()}"/>
    <g:if test="${!validation.valid}">
        <g:expander key="${akey}"><g:message code="more"/></g:expander>
        <div class="well well-sm well-embed" id="${akey}" style="display: none">

            <g:render template="aclValidationReport"
                      model="${[validation: validation, documentPrefix: (prefix?:'')+policyFile]}"/>
        </div>
    </g:if>
</li>