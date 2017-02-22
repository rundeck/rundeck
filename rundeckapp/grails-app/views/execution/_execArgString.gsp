%{--
  Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%

<g:set var="parsed" value="${g.parseOptsFromString(args: argString)}"/>
<g:if test="${parsed}">
    <g:each in="${parsed}" var="entry">
        <span class="optkey"><g:enc>${entry.key}</g:enc></span>:
        <g:if test="${entry.value}">
            <g:if test="${inputFilesMap && inputFilesMap[entry.value]}">
                <span class="optvalue" title="${entry.value}">
                    <g:icon name="file"/>
                    ${inputFilesMap[entry.value].size} bytes
                    <g:if test="${inputFilesMap[entry.value].fileState == 'retained'}">
                        <g:icon name="ok"/>
                    </g:if>
                    <g:else>
                        <g:icon name="remove"/>
                    </g:else>
                </span>
            </g:if>
            <g:else>
                <span class="optvalue"><g:enc>${entry.value}</g:enc></span>
            </g:else>
        </g:if>
    </g:each>
</g:if>
<g:else>
    <span class="optvalue"><g:enc>${argString}</g:enc></span>
</g:else>
