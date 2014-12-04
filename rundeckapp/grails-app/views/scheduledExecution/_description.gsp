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

<g:set var="allowHTML"
       value="${!(grailsApplication.config.rundeck?.gui?.job?.description?.disableHTML in [true,'true'])}"/>
<g:set var="firstline" value="${g.textFirstLine(text: description)}"/>
<g:if test="${allowHTML && !firstLineOnly}">
    <g:set var="remainingLine" value="${g.textRemainingLines(text: description)}"/>
    <span class="${enc(attr: textCss ?: '')}"><g:enc>${firstline}</g:enc></span>
    <g:if test="${remainingLine}">
        <g:if test="${mode=='collapsed' || mode=='expanded'}">
            <span class="expandComponentHolder">
            <g:expander key="desc_${rkey}" open="${mode=='expanded'?'true':'false'}">More</g:expander>
            <span class="${enc(attr: markdownCss ?: '')}" style="${wdgt.styleVisible(if:mode=='expanded')}" id="desc_${enc(attr: rkey)}">
                <g:markdown>${remainingLine}</g:markdown>
            </span>
            </span>
        </g:if>
        <g:elseif test="${mode!='hidden'}">
            <span class="${enc(attr: markdownCss ?: '')}">
                <g:markdown>${remainingLine}</g:markdown>
            </span>
        </g:elseif>
    </g:if>
</g:if>
<g:else>
    <span class="${enc(attr:textCss?:'')}">
        <g:enc>${firstline}</g:enc>
    </span>
</g:else>
