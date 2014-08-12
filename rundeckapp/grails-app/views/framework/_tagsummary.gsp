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
<g:set var="urkey" value="${g.rkey()}"/>
<%@ page contentType="text/html;charset=UTF-8" %>
<g:if test="${tagsummary}">
        <g:set var="hidetop" value="${hidetop?:tagsummary.findAll {it.value>1}.size()>30}"/>
        <g:if test="${hidetop}">
            <span class="textbtn textbtn-secondary tag"
                  title="Show tag demographics" onclick="Element.toggle('tagdemo'); Element.toggleClassName(this,'active');">
                <i class="glyphicon glyphicon-tags text-muted "></i>
                <g:enc>${tagsummary.size()}</g:enc> tags
                <i class="glyphicon glyphicon-chevron-right"></i></span>
        </g:if>
        <span id="tagdemo" style="${wdgt.styleVisible(unless: hidetop)}">
            <g:if test="${!hidetop}">
                <i class="glyphicon glyphicon-tags text-muted"></i>
            </g:if>
            <g:set var="singletag" value="${[]}"/>
            <g:each var="tag" in="${tagsummary.sort{a,b->a.value>b.value?-1:a.value<b.value?1:a.key<=>b.key}.keySet()}">
                <g:if test="${tagsummary[tag]>1 || tagsummary.size()<=30}">
                    <span class="summary nodetags">
                        <g:if test="${link}">
                            <g:render template="nodeFilterLink"
                                model="[key:'tags',value:tag,linktext:tag+' ('+tagsummary[tag]+')',css:'tag textbtn']"
                                      />
                        </g:if>
                        <g:elseif test="${action}">
                            <span class="${enc(attr:action.classnames)}" onclick="${enc(attr:action.onclick)}"
                                  data-tag="${enc(attr:tag)}" title="Filter by tag: ${enc(attr:tag)}">
                                <g:enc>${tag}:${tagsummary[tag]}</g:enc>
                            </span>
                        </g:elseif>
                        <g:else>
                            <g:enc>${tag}:${tagsummary[tag]}</g:enc>
                        </g:else>
                        </span>
                </g:if>
                <g:else>
                    %{ singletag << tag }%
                </g:else>
            </g:each>
            <g:if test="${singletag}">
                <span class="btn btn-sm btn-default receiver" title="See all tags"
                      onclick="Element.show('${enc(attr:urkey)}singletags');
                Element.hide(this);">Show All&hellip;</span>
                <span style="display:none" id="${enc(attr:urkey)}singletags">
                    <g:each var="tag" in="${singletag}">
                        <span class="summary">
                            <g:if test="${link}">
                                <g:render template="nodeFilterLink"
                                          model="[key: 'tags', value: tag, linktext: tag + ' (' + tagsummary[tag]+')', css: 'tag textbtn']"/>
                            </g:if>
                            <g:elseif test="${action}">
                                <span class=" ${enc(attr:action.classnames)}" onclick="${enc(attr:action.onclick)}"
                                      data-tag="${enc(attr:tag)}"
                                      title="Filter by tag: ${enc(attr:tag)}">
                                    <g:enc>${tag}:${tagsummary[tag]}</g:enc>
                                </span>
                            </g:elseif>
                            <g:else>
                                <g:enc>${tag}
                                (${tagsummary[tag]})</g:enc>
                            </g:else></span>
                    </g:each>
                </span>
            </g:if>
        </span>
    
</g:if>
