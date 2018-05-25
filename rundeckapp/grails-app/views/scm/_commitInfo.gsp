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

<g:if test="${title}">
    <h3>${title}</h3>
</g:if>
<g:set var="rkey" value="${g.rkey()}"/>
<blockquote>
    ${commit.message}
    <footer>
        ${commit.author}
        <g:relativeDate elapsed="${commit.date}"/>
        in
        <g:expander key="commitInfo_${rkey}" classnames="textbtn-info">
            <cite>
                ${commit.commitId}
            </cite>
        </g:expander>
    </footer>
</blockquote>

<table class="table table-bordered table-condensed table-striped "
    id="commitInfo_${rkey}"
       style="display:none">
    <g:set var="map" value="${commit.asMap()}"/>
    <g:each in="${map.keySet().sort()}" var="key">
        <tr>
            <td>${key}</td>
            <td>${map[key]}</td>
        </tr>
    </g:each>
</table>