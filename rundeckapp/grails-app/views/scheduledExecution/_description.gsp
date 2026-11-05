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
<%@ page import="org.rundeck.util.HtmlTableToMarkdownConverter" %>

<g:set var="allowHTML"
       value="${!(cfg.getString(config: "gui.job.description.disableHTML") in [true,'true'])}"/>
%{-- A description with a table on its very first line -- whether or not
     prose shares that line with it -- has no separate first line that can
     safely become a short summary: splitting one off would either leave the
     table nothing to convert (a single-line table) or strip its own opening
     tag onto the discarded line (a multi-line table starting on line one).
     The summary line is always plain, escaped text anyway, so it could never
     render a table either way. In that case the entire description goes
     through as "remainingLine" and there is no separate first line. Every
     other description keeps its existing first-line/remaining-lines split
     unchanged -- see HtmlTableToMarkdownConverter javadoc. --}%
%{-- The check must see the very same text the converter will get: only
     what precedes the cutoff marker. Otherwise an unsupported table in the
     cut-off section after the marker would make the check fail for a
     perfectly good table on line one. --}%
<g:set var="renderableDescription"
       value="${cutoffMarker ? g.textBeforeLine(text: description, marker: cutoffMarker) : description}"/>
<g:set var="descriptionFirstLineHasTable"
       value="${allowHTML && !firstLineOnly && HtmlTableToMarkdownConverter.firstLineContainsTable(renderableDescription?.toString())}"/>
<g:set var="firstline" value="${descriptionFirstLineHasTable ? '' : g.textFirstLine(text: description)}"/>
<g:if test="${allowHTML && !firstLineOnly}">
    <g:set var="remainingLine"
           value="${descriptionFirstLineHasTable ? renderableDescription : g.textRemainingLines(text: description)}"/>
    <g:if test="${cutoffMarker}">
        <g:set var="remainingLine" value="${g.textBeforeLine(text: remainingLine, marker:cutoffMarker)}"/>
    </g:if>
    %{-- Convert hand-written HTML <table> blocks to Markdown before they
         reach <g:markdown> -- see HtmlTableToMarkdownConverter javadoc. --}%
    <g:set var="remainingLine" value="${HtmlTableToMarkdownConverter.convert(remainingLine?.toString())}"/>

    <g:if test="${remainingLine?.trim()}">
        <g:set var="replTokens" value="${[:]}"/>
        <g:if test="${service && name}">
            <g:set var="pluginBaseUrl" value="${g.createLink(
                    controller: 'plugin',
                    action: 'pluginFile',
                    params: [service: service, name: name]
            )}"/>
            <g:set var="replTokens" value="${['plugin.url':pluginBaseUrl]}"/>
        </g:if>
        <g:if test="${mode=='collapsed' || mode=='expanded'}">
            <details class="more-info" ${mode=='expanded'?'open':''}>
                <summary>
                    <span class="${enc(attr: textCss ?: '')}"><g:enc>${firstline}</g:enc></span>
                    <span class="btn-link btn-xs more-indicator-verbiage">
                        ${moreText?:message(code: "more", default: "More")}
                    </span>
                    <span class="btn-link btn-xs less-indicator-verbiage">
                        <g:message code="less"  default="Less"/>
                    </span>
                </summary>
                <div class="${enc(attr: markdownCss ?: '')} more-info-content" >
                    <g:markdown><g:autoLink jobLinkId="${jobLinkId}" tokens="${replTokens}">${remainingLine}</g:autoLink></g:markdown>
                </div>
            </details>

        </g:if>
        <g:elseif test="${mode!='hidden'}">
            <span class="${enc(attr: markdownCss ?: '')}">
                <g:markdown><g:autoLink jobLinkId="${jobLinkId}" tokens="${replTokens}">${remainingLine}</g:autoLink></g:markdown>
            </span>
        </g:elseif>
    </g:if>
    <g:else>
        <span class="${enc(attr: textCss ?: '')}"><g:enc>${firstline}</g:enc></span>
    </g:else>
</g:if>
<g:else>
    <span class="${enc(attr:textCss?:'')}">
        <g:enc>${firstline}</g:enc>
    </span>
</g:else>
