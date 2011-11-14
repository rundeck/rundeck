%{--
  - Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
 <%--
    _tagsummary.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 10/24/11 12:27 PM
 --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<g:if test="${tagsummary}">
        <g:set var="hidetop" value="${tagsummary.findAll {it.value>1}.size()>30}"/>
        <g:if test="${hidetop}">
            <span class="action button receiver" title="Show tag demographics" onclick="Element.show('tagdemo');
            Element.hide(this);">Show ${tagsummary.size()} tags&hellip;</span>
        </g:if>
        <span id="tagdemo" style="${wdgt.styleVisible(unless: hidetop)}">
            <span class="desc">${tagsummary.size()} tags:</span>
            <g:set var="singletag" value="${[]}"/>
            <g:each var="tag" in="${tagsummary.sort{a,b->a.value>b.value?-1:a.value<b.value?1:a.key<=>b.key}.keySet()}">
                <g:if test="${tagsummary[tag]>1 || tagsummary.size()<=30}">
                    <span class="summary">
                        <g:if test="${link}">
                            <g:link class=" action" action="${link.action}" controller="${link.controller}" params="${[(link.param):tag]}"
                                    title="Filter by tag: ${tag}">${tag.encodeAsHTML()}</g:link>:${tagsummary[tag]}
                        </g:if>
                        <g:elseif test="${action}">
                            <span class="${action.classnames}" onclick="${action.onclick}" tag="${tag.encodeAsHTML()}" title="Filter by tag: ${tag}">${tag.encodeAsHTML()}:${tagsummary[tag]}</span>
                        </g:elseif>
                        <g:else>
                            ${tag.encodeAsHTML()}:${tagsummary[tag]}
                        </g:else>
                        </span>
                </g:if>
                <g:else>
                    %{ singletag << tag }%
                </g:else>
            </g:each>
            <g:if test="${singletag}">
                <span class="action button receiver" title="See all tags" onclick="Element.show('singletags');
                Element.hide(this);">Show All&hellip;</span>
                <span style="display:none" id="singletags">
                    <g:each var="tag" in="${singletag}">
                        <span class="summary">
                            <g:if test="${link}">
                                <g:link class=" action" action="${link.action}" controller="${link.controller}"
                                        params="${[(link.param):tag]}"
                                        title="Filter by tag: ${tag}">${tag.encodeAsHTML()}</g:link>
                                :${tagsummary[tag]}
                            </g:if>
                            <g:elseif test="${action}">
                                <span class=" ${action.classnames}" onclick="${action.onclick}"
                                      tag="${tag.encodeAsHTML()}"
                                      title="Filter by tag: ${tag}">${tag.encodeAsHTML()}:${tagsummary[tag]}</span>
                            </g:elseif>
                            <g:else>
                                ${tag.encodeAsHTML()}
                                :${tagsummary[tag]}
                            </g:else></span>
                    </g:each>
                </span>
            </g:if>
        </span>
    
</g:if>