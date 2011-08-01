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
   _editProjectForm.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 8/1/11 11:38 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<g:if test="${editOnly}">
    <g:hiddenField name="project" value="${project}"/>
</g:if>
<table class="simpleForm" cellspacing="0">
    <g:if test="${!editOnly}">
    <tr>
        <td><g:message code="domain.Project.field.name" default="Project Name"/>:</td>
        <td>
            <g:textField name="project" size="50" autofocus="true"/>
        </td>
    </tr>
    </g:if>

    <tr>

        <td>
            <g:message code="domain.Project.field.resourcesUrl" default="Resources Provider URL"/>:
        </td>
        <td>
            <g:textField name="resourcesUrl" size="50" value="${resourcesUrl?:params.resourcesUrl}"/>
            <div class="info note">
                An optional URL to a remote Resource Model Provider.
            </div>
        </td>
    </tr>
</table>
<g:if test="${resourceModelConfigDescriptions}">

    <span class="prompt">
        Resource Model Sources:
    </span>

    <div class="presentation">
        You can add additional custom sources, and their results will be used with the ordering shown.
        Later sources will override earlier sources.
    </div>

    <div class="error note" id="errors" style="display:none;">

    </div>
    <ol id="configs">
        <g:if test="${configs}">
            <g:each var="config" in="${configs}" status="n">
                <li>
                    <div class="inpageconfig">
                        <g:set var="desc" value="${resourceModelConfigDescriptions.find {it.name==config.type}}"/>
                        <g:if test="${!desc}">
                            <span class="warn note invalidProvider">Invalid Resurce Model Source configuration: Provider not found: ${config.type.encodeAsHTML()}</span>
                        </g:if>
                        <g:render template="viewResourceModelConfig"
                                  model="${[prefix: prefixKey+'.'+(n+1)+'.', values: config.props, includeFormFields: true, description: desc, saved:true,type:config.type]}"/>
                    </div>
                </li>
            </g:each>
        </g:if>
    </ol>

    <div id="sourcebutton" class="sourcechrome presentation"><button>Add Source</button></div>

    <div id="sourcepicker" class="popout sourcechrome" style="display:none;">
        <span class="prompt">
            Choose the type of Source to add:
        </span>
        <ul>
            <g:each in="${resourceModelConfigDescriptions}" var="description">
                <li>
                    <button onclick="configControl.addConfig('${description.name.encodeAsJavaScript()}');
                    return false;">Add</button>
                    <b>${description.title.encodeAsHTML()}</b> - ${description.description.encodeAsHTML()}
                </li>
            </g:each>
        </ul>

        <div id="sourcecancel" class="sourcechrome presentation"><button>Cancel</button></div>
    </div>
</g:if>