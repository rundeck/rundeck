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

<g:if test="${flash.message || request.message}">
    <div class="alert alert-info alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:autoLink><g:enc>${flash.message}${request.message}</g:enc></g:autoLink>
        <g:if test="${flash.extendedMessage||request.extendedMessage}">
            <g:set var="ekey" value="${g.rkey()}"/>
            <g:expander key="${ekey}"><g:message code="more.info" /></g:expander>
            <div id="${ekey}" style="display:none">
                <g:autoLink><g:enc>${flash.extendedMessage}${request.extendedMessage}</g:enc></g:autoLink>
            </div>
        </g:if>
    </div>
</g:if>
<g:if test="${flash.invalidToken||flash.error||flash.errors!=null||request.error||( ((request.errors instanceof org.springframework.validation.Errors && request.errors.hasErrors())|| request.errors instanceof java.util.Collection))||flash.errorCode||request.errorCode}">
    <div class="alert alert-danger alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:autoLink><g:enc>${flash.error?.toString()}${request.error?.toString()}</g:enc></g:autoLink>
        <g:if test="${flash.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${flash.errors}" as="list"/>
        </g:if>
        <g:if test="${request.errors instanceof org.springframework.validation.Errors}">
            <g:renderErrors bean="${request.errors}" as="list"/>
        </g:if>
        <g:if test="${request.errors instanceof java.util.Collection}">
            <ul>
                <g:each in="${request.errors}" var="err">
                    <li><g:enc>${err}</g:enc></li>
                </g:each>
            </ul>
        </g:if>
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}"
                       args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
        <g:if test="${flash.invalidToken}">
            <g:message code="request.error.invalidtoken.message"/>
        </g:if>
    </div>
    <g:if test="${request.errorHelp || flash.errorHelp}">
        <div class="alert alert-info alert-dismissable">
                <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>

            <g:icon name="question-sign"/>
            <g:autoLink><g:enc>${flash.errorHelp?.toString()}${request.errorHelp?.toString()}</g:enc></g:autoLink>
        </div>
    </g:if>
</g:if>
<g:if test="${flash.warn || request.warn}">
    <div class="alert alert-warning alert-dismissable">
        <g:unless test="${notDismissable}">
            <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        </g:unless>
        <g:autoLink><g:enc>${flash.warn}${request.warn}</g:enc></g:autoLink>
    </div>
</g:if>
