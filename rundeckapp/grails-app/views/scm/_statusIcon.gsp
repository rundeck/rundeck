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

<g:set var="textColor" value="text-info"/>
<g:set var="iconName" value="${iscommit ? 'glyphicon-plus' : 'glyphicon-question-sign'}"/>
<g:set var="defaultExportText"
       value="${message(code: "scm.export.status.${exportStatus}.display.text", default: exportStatus.toString())}"/>
<g:set var="defaultImportText"
       value="${message(code: "scm.import.status.${importStatus}.display.text", default: importStatus.toString())}"/>

<g:set var="showStatus" value="${ exportStatus && (showClean || exportStatus.toString() != 'CLEAN') ? exportStatus : importStatus}"/>
<g:set var="defaultText" value="${ exportStatus && (showClean || exportStatus.toString() != 'CLEAN') ? defaultExportText : defaultImportText}"/>
<g:if test="${exportStatus && importStatus &&
        (exportStatus.toString() != 'CLEAN' && importStatus.toString() != 'CLEAN')}">
%{--combined status--}%
    <g:set var="textColor" value="text-warning"/>
    <g:set var="iconName" value="glyphicon-exclamation-sign"/>
</g:if>
<g:else>

    <g:if test="${showStatus.toString() == 'EXPORT_NEEDED'}">
        <g:set var="textColor" value="text-info"/>
        <g:set var="iconName" value="glyphicon-exclamation-sign"/>
    </g:if>
    <g:elseif test="${showStatus.toString() == 'CREATE_NEEDED'}">
        <g:set var="textColor" value="text-success"/>
        <g:set var="iconName" value="glyphicon-exclamation-sign"/>
    </g:elseif>
    <g:elseif test="${showStatus.toString() == 'UNKNOWN'}">
        <g:set var="textColor" value="text-muted"/>
        <g:set var="iconName" value="glyphicon-question-sign"/>
    </g:elseif>
    <g:elseif test="${showStatus.toString() == 'IMPORT_NEEDED'}">
        <g:set var="textColor" value="text-warning"/>
        <g:set var="iconName" value="glyphicon-exclamation-sign"/>
    </g:elseif>
    <g:elseif test="${showStatus.toString() == 'REFRESH_NEEDED'}">
        <g:set var="textColor" value="text-warning"/>
        <g:set var="iconName" value="glyphicon-exclamation-sign"/>
    </g:elseif>
    <g:elseif test="${showStatus.toString() == 'DELETED'}">
        <g:set var="textColor" value="text-danger"/>
        <g:set var="iconName" value="glyphicon-minus-sign"/>

    </g:elseif>
    <g:elseif test="${showStatus.toString() == 'CLEAN'}">
        <g:set var="textColor" value="text-muted"/>
        <g:set var="iconName" value="glyphicon-ok"/>
    </g:elseif>
</g:else>

<span class="${textColor}">
    <g:if test="${!noicon}">
        <g:icon name="${icon ?: iconName}"/>
    </g:if>
    <g:if test="${!notext}">
        ${text ?: defaultText}
    </g:if>
</span>