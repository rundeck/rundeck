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

<g:if test="${showClean && (exportStatus || importStatus) ||
        exportStatus && exportStatus?.toString() != 'CLEAN' ||
        importStatus && importStatus?.toString() != 'CLEAN'}">
    <g:set var="tooltips" value="${[]}"/>
    <g:if test="${exportStatus}">
        %{
            tooltips << message(
                    code: "scm.export.status.${exportStatus}.description",
                    default: exportStatus.toString()
            )
        }%
    </g:if>
    <g:if test="${importStatus}">
        %{
            tooltips << message(
                    code: "scm.import.status.${importStatus}.description",
                    default: importStatus.toString()
            )
        }%
    </g:if>
    <span title="${tooltips.join(", ")}" class="has_tooltip">
        <g:render template="/scm/statusIcon"
                  model="[exportStatus: exportStatus,
                          importStatus: importStatus,
                          showClean   : showClean,
                          text        : text,
                          icon        : icon,
                          notext      : notext,
                          exportCommit: exportCommit,
                          importCommit: importCommit]"/>
    </span>

</g:if>
