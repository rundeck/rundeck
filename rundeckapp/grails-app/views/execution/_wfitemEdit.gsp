<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
    _wfitemEdit.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jul 26, 2010 5:15:53 PM
    $Id$
 --%>
<g:set var="rkey" value="${g.rkey()}"/>
<div class="popout wfitemEditForm" style="background:white">
<g:hasErrors bean="${item}">
    <div class="error message">
    <g:renderErrors bean="${item}" as="list"/>
    </div>
</g:hasErrors>
<g:render template="/common/messages"/>
<div id="wfiedit_${rkey}">
<g:if test="${'job'==newitemtype || item instanceof JobExec || (item instanceof java.util.Map && item?.jobName)}">
    <div >
       <div class="info note">Job Name</div>
       <input id="jobNameField" type="text" name="jobName" value="${item?.jobName}" size="40" autofocus/>
    </div>
    <div >
        <div class="info note">Job Group</div>
        <input id="jobGroupField"  type="text" name="jobGroup" value="${item?.jobGroup}" size="40"/>
    </div>
    <div  >
        <div class="info note">Enter the commandline arguments for the Job:</div>
        <input type='text' name="argString" value="${item?.argString?.encodeAsHTML()}" size="80" id="jobArgStringField"/>
    </div>
    <div style="margin-top:5px;">
        <span class="action button" onclick="loadJobChooser(this,'jobChooser');" id="jobChooseBtn" title="Select an existing Job to use">Choose A Job&hellip; <g:img file="icon-tiny-disclosure.png" width="12px" height="12px"/></span>
        <span id="jobChooseSpinner"></span>
    </div>
    <div class="popout" id="jobChooser" style="display:none; width:300px; padding: 5px; background:white; position:absolute;">
        <div style="margin-bottom:5px;">
            <span class="info note">Click on the name of the Job to use</span>
            <span class=" floatr action textbtn" style="text-align:right" onclick="hideJobChooser();">
                Close
                <g:img file="icon-tiny-removex-gray.png" width="12px" height="12px"/>
            </span>
        </div>
        <div id="jobChooserContent" style="overflow-y:auto;">
        </div>
    </div>
</g:if>
<g:elseif test="${'script'==newitemtype || 'scriptfile'==newitemtype || 'command'==newitemtype || item?.adhocExecution}">
    <g:set var="isAdhocRemote" value="${'command'==newitemtype || item?.adhocRemoteString}"/>
    <g:set var="isAdhocLocal" value="${'script'==newitemtype || item?.adhocLocalString}"/>
    <g:set var="isAdhocFileExecution" value="${'scriptfile'==newitemtype || item?.adhocFilepath}"/>
    <g:hiddenField name="adhocExecution" value="true"/>
    <g:if test="${isAdhocLocal}">
        <div id="localScriptDiv" class="${hasErrors(bean:item,field:'adhocExecution','fieldError')}">
            <div class="info note"><g:message code="Workflow.Step.adhocLocalString.description" />:</div>
            <textarea rows="10" cols="60" name="adhocLocalString" id="adhocLocalStringField" class="code" autofocus>${item?.adhocLocalString?.encodeAsHTML()}</textarea>
        </div>
    </g:if>
    <g:elseif test="${isAdhocFileExecution}">
    <div id="filepathDiv" >
        <div class="info note"><g:message code="Workflow.Step.adhocFilepath.description" />:</div>
        <input type='text' name="adhocFilepath" value="${item?.adhocFilepath?.encodeAsHTML()}" size="80" id="adhocFilepathField" autofocus/>
    </div>
    </g:elseif>
    <g:elseif test="${isAdhocRemote}">
    <div id="remoteScriptDiv"  class="${hasErrors(bean:item,field:'adhocExecution','fieldError')}">
        <div class="info note"><g:message code="Workflow.Step.adhocRemoteString.description" />:</div>
        <input type='text' name="adhocRemoteString" value="${item?.adhocRemoteString?.encodeAsHTML()}" size="80" id="adhocRemoteStringField" autofocus/>
    </div>
    </g:elseif>
    <g:if test="${!isAdhocRemote||isAdhocFileExecution}">
    <div id="adhocScriptArgs" >
        <div class="info note"><g:message code="Workflow.Step.argString.description" />:</div>
        <input type='text' name="argString" value="${item?.argString?.encodeAsHTML()}" size="80" id="argStringField"/>
    </div>
    </g:if>
</g:elseif>

<g:hiddenField name="num" value="${num}"/>
<g:hiddenField name="scheduledExecutionId" value="${scheduledExecutionId}"/>
    <div class="floatr" style="margin:10px 0;">
        <span class="warn note cancelsavemsg" style="display:none;">
            <g:message code="scheduledExecution.workflow.step.unsaved.warning"
                       default="Discard or save changes to this Workflow Step before completing changes to the job"/>
        </span>
        <g:if test="${newitemtype||newitem}">
            <g:hiddenField name="newitem" value="true"/>
            <g:hiddenField name="newitemtype" value="${newitemtype}"/>
            <span class="action button small textbtn" onclick="_wficancelnew(${num});" title="Cancel adding new ${g.message(code:'Workflow.step.label')}">Cancel</span>
            <span class="action button small textbtn" onclick="_wfisavenew('wfiedit_${rkey}');" title="Save the new ${g.message(code:'Workflow.step.label')}">Save</span>
        </g:if>
        <g:else>
            <span class="action button small textbtn" onclick="_wfiview(${num});" title="Discard changes to the ${g.message(code:'Workflow.step.label')}">Discard</span>
            <span class="action button small textbtn" onclick="_wfisave(${num}, 'wfiedit_${rkey}');" title="Save changes to the ${g.message(code:'Workflow.step.label')}">Save</span>
        </g:else>
    </div>
    <div class="clear"></div>
</div>
</div>