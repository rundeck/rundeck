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


<ul>
    <g:each in="${validation.errors.keySet().sort()}" var="ident">

        <li>
            <g:if test="${documentPrefix && ident.startsWith(documentPrefix)}">
                <g:set var="policyIdent" value="${ident.substring((documentPrefix).size())}"/>
                <span class=""><g:message
                        code="acl.validation.policy.document.identity.title.0"
                        args="${[policyIdent]}"/></span>
            </g:if>
            <g:else>
                <code>${ident}</code>
                <g:helpTooltip css="text-info" code="acl.validation.error.sourceIdentity.help"/>
            </g:else>

            <ol>
                <g:each in="${validation.errors[ident]}" var="message">
                    <li><code>${message}</code></li>
                </g:each>
            </ol>
        </li>
    </g:each>
</ul>