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
                <g:set var="frkey" value="${g.rkey()}"/>
                <span class="optvalue optextra" title="File Input Option"
                      data-toggle="popover"
                      data-placement="bottom"
                      data-popover-content-ref="#f_${frkey}"
                      data-trigger="hover"
                      data-container="body">
                    ${inputFilesMap[entry.value].fileName ?: entry.value}
                </span>
                <span id="f_${frkey}" style="display: none">
                    <g:basicData classes="table-condensed table-bordered"
                                 fieldTitle="${[
                                         'fileName': 'Name',
                                         'size'    : 'Size',
                                         'sha'     : 'SHA256',
                                         'id'      : 'ID',
                                 ]}"
                                 fields="${[
                                         'fileName',
                                         'size',
                                         'sha',
                                         'id'
                                 ]}"
                                 data="${[
                                         fileName: inputFilesMap[entry.value].fileName ?: '(not set)',
                                         sha     : inputFilesMap[entry.value].sha?.substring(0, 15) + '...',
                                         size    : g.humanizeValue(
                                                 [value: inputFilesMap[entry.value].size, unit: 'byte']
                                         ),
                                         id      : entry.value,
                                 ]}"
                                 dataTitles="${[sha: inputFilesMap[entry.value].sha]}">
                    </g:basicData>
                </span>
            </g:if>
            <g:else>
                <code class="optvalue"><g:enc>${entry.value}</g:enc></code>
            </g:else>
        </g:if>
    </g:each>
</g:if>
<g:else>
    <code class="optvalue"><g:enc>${argString}</g:enc></code>
</g:else>
