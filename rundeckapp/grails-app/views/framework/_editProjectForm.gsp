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
            <td>
                <label for="project" class="${projectNameError?'fieldError':''} required">
                <g:message code="domain.Project.field.name" default="Project Name"/>:
                </label>
            </td>
            <td>
                <g:textField name="project" size="50" autofocus="true" value="${project}"/>

                <g:if test="${projectNameError}">
                    <div class="warn note">${projectNameError.encodeAsHTML()}</div>
                </g:if>
            </td>
        </tr>
    </g:if>

    <tr>

        <td>
            <g:message code="domain.Project.field.resourcesUrl" default="Resource Model Source URL"/>:
        </td>
        <td>
            <g:textField name="resourcesUrl" size="50" value="${resourcesUrl?:params.resourcesUrl}"/>
            <div class="info note">
                <g:message code="domain.Project.field.resourcesUrl.description" />
            </div>
        </td>
    </tr>
    <tr>

        <td>
            <g:message code="domain.Project.field.sshKeyPath" default="Default SSH Key File"/>:
        </td>
        <td>
            <g:textField name="sshkeypath" size="50" value="${sshkeypath?:params.sshkeypath}"/>
            <div class="info note">
            <g:message code="domain.Project.field.sshKeyPath.description" />
            </div>
        </td>
    </tr>
</table>
<g:if test="${resourceModelConfigDescriptions}">

    <span class="section prompt">
        <g:message code="framework.service.ResourceModelSource.label" />
    </span>

    <div class="presentation">
        <g:message code="domain.Project.edit.ResourceModelSource.explanation" />
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
                            <span
                                class="warn note invalidProvider">Invalid Resurce Model Source configuration: Provider not found: ${config.type.encodeAsHTML()}</span>
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

<g:if test="${nodeExecDescriptions}">
    <span class="section prompt">Default <g:message code="framework.service.NodeExecutor.label" /></span>


    <div class="presentation">
        <span
            class="info note"><g:message code="domain.Project.edit.NodeExecutor.explanation" /></span>
        <g:each in="${nodeExecDescriptions}" var="description" status="nex">
            <g:set var="nkey" value="${g.rkey()}"/>
            <div>
                <label>
                    <g:radio
                        name="defaultNodeExec"
                        value="${nex}"
                        class="nexec"
                        id="${nkey+'_input'}"
                        checked="${defaultNodeExec?defaultNodeExec==description.name:false}"/>
                    <b>${description.title.encodeAsHTML()}</b> - ${description.description.encodeAsHTML()}
                </label>
                <g:hiddenField name="nodeexec.${nex}.type" value="${description.name}"/>
                <g:set var="nodeexecprefix" value="nodeexec.${nex}.config."/>
                <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                    <wdgt:action visible="false" targetSelector="table.nexecDetails"/>
                </wdgt:eventHandler>
                <g:if test="${description}">
                    <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                        <wdgt:action visible="true" target="${nkey+'_det'}"/>
                    </wdgt:eventHandler>
                    <table class="simpleForm nexecDetails" id="${nkey + '_det'}"
                           style="${wdgt.styleVisible(if: defaultNodeExec == description.name)}">
                        <g:each in="${description.properties}" var="prop">
                            <tr>
                                <g:render
                                    template="pluginConfigPropertyField"
                                    model="${[prop:prop,prefix:nodeexecprefix,error:nodeexecreport?.errors?nodeexecreport?.errors[prop.name]:null,values:nodeexecconfig,
                                fieldname:nodeexecprefix+prop.name,origfieldname:'orig.'+nodeexecprefix+prop.name]}"/>
                            </tr>
                        </g:each>
                    </table>
                </g:if>
            </div>
        </g:each>
    </div>

</g:if>
<g:if test="${fileCopyDescriptions}">
    <span class="section prompt">Default Node <g:message code="framework.service.FileCopier.label"/></span>


    <div class="presentation">
        <span
            class="info note"><g:message code="domain.Project.edit.FileCopier.explanation" /></span>
        <g:each in="${fileCopyDescriptions}" var="description" status="nex">
            <g:set var="nkey" value="${g.rkey()}"/>
            <div>
                <label>
                    <g:radio
                        name="defaultFileCopy"
                        value="${nex}"
                        class="fcopy"
                        id="${nkey+'_input'}"
                        checked="${defaultFileCopy?defaultFileCopy==description.name:false}"/>
                    <b>${description.title.encodeAsHTML()}</b> - ${description.description.encodeAsHTML()}
                </label>
                <g:hiddenField name="fcopy.${nex}.type" value="${description.name}"/>
                <g:set var="fcopyprefix" value="fcopy.${nex}.config."/>
                <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                    <wdgt:action visible="false" targetSelector="table.fcopyDetails"/>
                </wdgt:eventHandler>
                <g:if test="${description}">
                    <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                        <wdgt:action visible="true" target="${nkey+'_det'}"/>
                    </wdgt:eventHandler>
                    <table class="simpleForm fcopyDetails" id="${nkey + '_det'}"
                           style="${wdgt.styleVisible(if: defaultFileCopy == description.name)}">
                        <g:each in="${description.properties}" var="prop">
                            <tr>
                                <g:render
                                    template="pluginConfigPropertyField"
                                    model="${[prop:prop,prefix:fcopyprefix,error:fcopyreport?.errors?fcopyreport?.errors[prop.name]:null,values:fcopyconfig,
                                fieldname:fcopyprefix+prop.name,origfieldname:'orig.'+fcopyprefix+prop.name]}"/>
                            </tr>
                        </g:each>
                    </table>
                </g:if></div>
        </g:each>

    </div>

</g:if>
