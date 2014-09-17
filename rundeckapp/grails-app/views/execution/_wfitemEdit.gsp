<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; rundeck.PluginStep; rundeck.CommandExec; rundeck.JobExec" %>
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
    <g:if test="${isErrorHandler}">
        <span class="text-info"><g:message code="Workflow.stepErrorHandler.description" /></span>
    </g:if>
<g:if test="${'job'==newitemtype || item instanceof JobExec || (item instanceof java.util.Map && item?.jobName)}">
    <div >
       <div class="text-muted">Job Name</div>
       <input id="jobNameField" type="text" name="jobName" value="${enc(attr:item?.jobName)}" size="100" autofocus/>
    </div>
    <div >
        <div class="text-muted">Job Group</div>
        <input id="jobGroupField"  type="text" name="jobGroup" value="${enc(attr:item?.jobGroup)}" size="100"/>
    </div>
    <div  >
        <div class="text-muted">Enter the commandline arguments for the Job:</div>
        <input type='text' name="argString" value="${enc(attr:item?.argString)}" size="100" id="jobArgStringField"/>
    </div>
    <div style="margin-top:5px;">
        <span class="btn btn-sm btn-default" onclick="loadJobChooser(this,'jobChooser');" id="jobChooseBtn"
              title="Select an existing Job to use"
              data-loading-text="Loading...">
            Choose A Job&hellip;
            <i class="caret"></i>
        </span>
        <span id="jobChooseSpinner"></span>
    </div>
    <div class="popout" id="jobChooser" style="display:none; width:300px; padding: 5px; background:white; position:absolute;">
        <div style="margin-bottom:5px;">
            <span class="text-muted">Click on the name of the Job to use</span>
            <button type="button" class=" close" style="text-align:right" onclick="hideJobChooser();">
                &times;
            </button>
        </div>
        <div id="jobChooserContent" style="overflow-y:auto;">
        </div>
    </div>

    <div style="margin-top:5px;">
        <g:set var="isNodeStep" value="${item ? item.nodeStep : newitemnodestep == 'true'}"/>
        <div class="prompt"><g:message code="JobExec.nodeStep.title" /></div>
        <div class="presentation">
            <div>
                <g:radio id="jobNodeStepFieldTrue" type="checkbox" name="nodeStep" value="true"
                         checked="${!!isNodeStep}"/>
                <label for="jobNodeStepFieldTrue">
                    <g:message code="JobExec.nodeStep.true.label" />
                </label>
                <span class="text-muted"><g:message code="JobExec.nodeStep.true.description"/></span>
            </div>
            <div>
                <g:radio id="jobNodeStepFieldFalse" type="checkbox" name="nodeStep" value="false"
                         checked="${!isNodeStep}"/>
                <label for="jobNodeStepFieldFalse">
                    <g:message code="JobExec.nodeStep.false.label" />
                </label>
                <span class="text-muted"><g:message code="JobExec.nodeStep.false.description"/></span>
            </div>
        </div>
    </div>
</g:if>
<g:elseif test="${'script'==newitemtype || 'scriptfile'==newitemtype || 'command'==newitemtype || item instanceof CommandExec }">
    <g:set var="isAdhocRemote" value="${'command'==newitemtype || item?.adhocRemoteString}"/>
    <g:set var="isAdhocLocal" value="${'script'==newitemtype || item?.adhocLocalString}"/>
    <g:set var="isAdhocFileExecution" value="${'scriptfile'==newitemtype || item?.adhocFilepath}"/>
    <g:hiddenField name="adhocExecution" value="true"/>
    <div id="scriptStep_${rkey}">
    <g:if test="${isAdhocLocal}">
        <div id="localScriptDiv" class="${hasErrors(bean:item,field:'adhocExecution','fieldError')}">
            <div class="text-muted"><g:message code="Workflow.Step.adhocLocalString.description" />:</div>
            <textarea rows="10" cols="60" name="adhocLocalString" id="adhocLocalStringField" class="code apply_ace" autofocus><g:enc>${item?.adhocLocalString}</g:enc></textarea>
        </div>
    </g:if>
    <g:elseif test="${isAdhocFileExecution}">
    <div id="filepathDiv" >
        <div class="text-muted"><g:message code="Workflow.Step.adhocFilepath.description" />:</div>
        <input type='text' name="adhocFilepath" value="${enc(attr:item?.adhocFilepath)}" size="100" id="adhocFilepathField" autofocus/>
    </div>
    </g:elseif>
    <g:elseif test="${isAdhocRemote}">
    <div id="remoteScriptDiv"  class="${hasErrors(bean:item,field:'adhocExecution','fieldError')}">
        <div class="text-muted"><g:message code="Workflow.Step.adhocRemoteString.description" />:</div>
        <input type='text' name="adhocRemoteString" value="${enc(attr:item?.adhocRemoteString)}" size="100" id="adhocRemoteStringField" autofocus/>
    </div>
    </g:elseif>
    <g:if test="${!isAdhocRemote||isAdhocFileExecution}">
    <div id="adhocScriptArgs" >
        <div class="text-muted"><g:message code="Workflow.Step.argString.description" />:</div>
        <input type='text' name="argString" value="${enc(attr:item?.argString)}" size="100" id="argStringField"
               data-bind="value: args, valueUpdate: 'keyup'"/>
    </div>
    </g:if>
    <g:if test="${!isAdhocRemote}">
        <g:expander key="scriptInterpreter${rkey}" open="${item?.scriptInterpreter?'true':'false'}">Advanced </g:expander>
        <div id="scriptInterpreter${enc(attr:rkey)}" style="${wdgt.styleVisible(if: item?.scriptInterpreter)}" class="presentation">
            <div class="">

                <span class="text-muted"><g:message
                        code="Workflow.Step.scriptInterpreter.label"/>:</span>
                <span class="action obs_tooltip"
                      id="interpreterHelp${enc(attr: rkey)}"><i
                        class="glyphicon glyphicon-question-sign  text-info"></i> Explain</span>

                <div class="popout tooltipcontent"
                     id="interpreterHelp${enc(attr: rkey)}_tooltip"
                     style="display:none; background:white; position:absolute; max-width: 500px; width:500px;">
                    <div class="help-block"><g:message code="Workflow.Step.scriptInterpreter.help"/></div>
                </div>
            </div>
            <input type='text' name="scriptInterpreter"
                   placeholder="${enc(attr:g.message(code: 'Workflow.Step.scriptInterpreter.prompt'))}"
                   value="${enc(attr:item?.scriptInterpreter)}" size="100"
                data-bind="value: invocationString, valueUpdate: 'keyup'"
                   id="scriptInterpreterField" autofocus/>
            <div>
                <label>
                    <g:checkBox name="interpreterArgsQuoted" checked="${item?.interpreterArgsQuoted}"
                                id="interpreterArgsQuotedField" value="true" data-bind="checked: argsQuoted"/>
                    <g:message code="Workflow.Step.interpreterArgsQuoted.label"/>
                </label>

                <span class="action obs_tooltip"
                      id="interpreterArgsQuotedHelp${enc(attr:rkey)}"><i class="glyphicon glyphicon-question-sign  text-info"></i> Explain</span>

                <div class="popout tooltipcontent" id="interpreterArgsQuotedHelp${enc(attr:rkey)}_tooltip"
                     style="display:none; background:white; position:absolute; max-width: 500px; width:500px;">
                    <div class="help-block"><g:message code="Workflow.Step.interpreterArgsQuoted.help"/></div>
                </div>
            </div>
        </div>
        <div>
        <span class="prompt">Execution Preview:</span>

        <div id='interpreterArgsQuotedHelp${rkey}_preview' class="presentation">
            <code>$ <span data-bind="html: invocationPreviewHtml"></span></code>
        </div>
            <g:embedJSON id="scriptStepData_${rkey}" data="${[invocationString: item?.scriptInterpreter?:'',args: item?.argString?:'',argsQuoted: item?.interpreterArgsQuoted?true:false]}"/>
            <g:javascript>
            fireWhenReady("scriptStep_${rkey}",function(){
                workflowEditor.bindKey('${rkey}','scriptStep_${rkey}',loadJsonData('scriptStepData_${rkey}'));
            });
            </g:javascript>
        </div>
    </g:if>
    </div>
</g:elseif>
<g:elseif test="${( newitemtype || item && item.instanceOf(PluginStep) ) && newitemDescription}">
    <div>
        <div>
            <span class="prompt"><g:enc>${newitemDescription.title}</g:enc></span>
            <span class="text-muted"><g:enc>${newitemDescription.description}</g:enc></span>
        </div>
        <g:hiddenField name="pluginItem" value="true"/>
        <g:hiddenField name="newitemnodestep" value="${item?!!item.nodeStep:newitemnodestep=='true'}"/>
        <div>
            <table class="simpleForm nexecDetails">
                <g:set var="pluginprefix" value="pluginConfig."/>
                <g:each in="${newitemDescription.properties}" var="prop">
                    <g:if test="${!prop.scope || prop.scope.isInstanceLevel() || prop.scope.isUnspecified()}">
                    <tr>
                        <g:render
                                template="/framework/pluginConfigPropertyField"
                                model="${[prop: prop, prefix: pluginprefix, values: item?.configuration,
                                        fieldname: pluginprefix + prop.name, origfieldname: 'orig.' + pluginprefix + prop.name, error: report?.errors ? report?.errors[prop.name] : null]}"/>
                    </tr>
                    </g:if>
                </g:each>
            </table>
        </div>
    </div>
</g:elseif>
<g:if test="${isErrorHandler}">
    <div class="presentation">
        <label>
        <g:checkBox name="keepgoingOnSuccess" value="true" checked="${item?.keepgoingOnSuccess}"/>
        <g:message code="Workflow.stepErrorHandler.keepgoingOnSuccess.label" />
        </label>
        <span class="text-muted"><g:message code="Workflow.stepErrorHandler.keepgoingOnSuccess.description" /></span>
    </div>
</g:if>
<g:else>
    <div>
        <div class="info note">Step Description</div>
        <input id="description" type="text" name="description" value="${enc(attr:item?.description)}" size="100"/>
    </div>
</g:else>

<g:hiddenField name="key" value="${key}"/>
<g:hiddenField name="isErrorHandler" value="${isErrorHandler ? true : false}"/>
<g:hiddenField name="scheduledExecutionId" value="${scheduledExecutionId}"/>
    <div class="floatr" style="margin:10px 0;">
        <g:set var="msgItem" value="${isErrorHandler ? 'stepErrorHandler' : 'step'}"/>
        <span class="warn note cancelsavemsg" style="display:none;">
            <g:message code="scheduledExecution.workflow.${msgItem}.Item.unsaved.warning"
                       default="Discard or save changes to this Workflow Step before completing changes to the job"/>
        </span>
        <g:if test="${newitemtype||newitem}">
            <g:hiddenField name="newitem" value="true"/>
            <g:hiddenField name="newitemtype" value="${newitemtype}"/>

            <g:if test="${isErrorHandler}">
                <g:hiddenField name="num" value="${num}"/>
                <span class="btn btn-default btn-sm" onclick="_wficancelnewEH(this);"
                      title="Cancel adding new ${g.message(code: 'Workflow.'+ msgItem+'.label')}">Cancel</span>
                <span class="btn btn-primary btn-sm" onclick="_wfisave('${key}', ${num}, 'wfiedit_${rkey}',${ isErrorHandler?true:false});" title="Save the new ${g.message(code:'Workflow.'+ msgItem+'.label')}">Save</span>
            </g:if>
            <g:else>

                <span class="btn btn-default btn-sm" onclick="_wficancelnew(${num});"
                      title="Cancel adding new ${g.message(code: 'Workflow.step.label')}">Cancel</span>
                <span class="btn btn-primary btn-sm" onclick="_wfisavenew('wfiedit_${rkey}');" title="Save the new ${g.message(code:'Workflow.step.label')}">Save</span>
            </g:else>
        </g:if>
        <g:else>
            <g:hiddenField name="num" value="${num}"/>
            <span class="btn btn-default btn-sm" onclick="_wfiview('${key}',${num},${isErrorHandler?true:false});" title="Discard changes to the ${g.message(code:'Workflow.'+ msgItem+'.label')}">Discard</span>
            <span class="btn btn-primary btn-sm" onclick="_wfisave('${key}',${num}, 'wfiedit_${rkey}', ${ isErrorHandler?true:false});"
                  title="Save changes to the ${g.message(code:'Workflow.'+ msgItem+'.label')}">Save</span>
        </g:else>
    </div>
    <div class="clear"></div>
</div>
</div>
