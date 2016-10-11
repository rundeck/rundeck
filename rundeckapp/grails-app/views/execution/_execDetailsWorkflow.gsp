<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" %>
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
<%--
   _execDetailsWorkflow.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Apr 20, 2010 2:53:14 PM
   $Id$
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<g:unless test="${isAdhoc}">
<g:if test="${edit}">
<div>
    <span class=""><g:message code="Workflow.property.keepgoing.prompt" /></span>
    <label>
        <input type="radio" name="workflow.keepgoing" value="false" ${workflow?.keepgoing?'':'checked'}/>
        <g:message code="Workflow.property.keepgoing.false.description"/>
    </label>
    <label>
        <input type="radio" name="workflow.keepgoing" value="true" ${workflow?.keepgoing?'checked':''}/>
        <g:message code="Workflow.property.keepgoing.true.description"/>
    </label>
</div>
<div class="" id="workflowstrategyplugins">
    <g:set var="wfstrat" value="${params?.workflow?.strategy?:workflow?.strategy=='step-first'?'sequential':workflow?.strategy?:'node-first'}"/>
    <div class="form-inline">
        <div class="form-group ${hasErrors(bean: workflow, field: 'strategy', 'has-error')}">
            <label class="col-sm-12" title="Strategy for iteration">
                <g:message code="strategy"/>:
                <g:select name="workflow.strategy" from="${strategyPlugins}" optionKey="name" optionValue="title"
                          value="${wfstrat}"
                          class="form-control"/>
            </label>
            <g:hasErrors bean="${workflow}" field="strategy">

                <div class="text-warning col-sm-12">
                    %{--<i class="glyphicon glyphicon-warning-sign"></i>--}%
                    <g:renderErrors bean="${workflow}" as="list" field="strategy"/>

                </div>

            </g:hasErrors>
        </div>
    </div>
    <g:each in="${strategyPlugins}" var="pluginDescription">
        <g:set var="pluginName" value="${pluginDescription.name}"/>
        <g:set var="prefix" value="${('workflow.strategyPlugin.'+ pluginName + '.config.')}"/>
        <g:set var="definedConfig"
               value="${params.workflow?.strategyPlugin?.get(pluginName)?.config ?: wfstrat == pluginName?workflow?.getPluginConfigData('WorkflowStrategy',pluginName):null}"/>
        <div id="strategyPlugin${pluginName}" style="${wdgt.styleVisible(if: wfstrat == pluginName ? true : false)}"
              class="strategyPlugin">
            <span class="text-info">
                <g:render template="/scheduledExecution/description"
                          model="[description: pluginDescription.description,
                                  textCss: '',
                                  mode: 'collapsed',
                                  moreText:'More Information',
                                  rkey: g.rkey()]"/>
            </span>
            <div>

                <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                        properties:pluginDescription?.properties,
                        report:params?.strategyValidation?.get(pluginName)?:null,
                        prefix:prefix,
                        values:definedConfig,
                        fieldnamePrefix:prefix,
                        origfieldnamePrefix:'orig.' + prefix,
                        allowedScope:com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance
                ]}"/>

            </div>
        </div>
        <wdgt:eventHandler for="workflow.strategy" equals="${pluginName}"
                           target="strategyPlugin${pluginName}" visible="true"/>
    </g:each>
<g:javascript>
jQuery(function(){
    "use strict";
    jQuery('#workflowstrategyplugins').find('textarea.apply_ace').each(function(ndx,elem){_addAceTextarea(elem)});
})
</g:javascript>

</div>
</g:if>
</g:unless>
<div class="pflowlist ${edit?'edit':''} rounded ${isAdhoc?'adhoc':''}" style="">
    <g:if test="${edit}">
        <div id="wfundoredo" class="undoredocontrols">
            <g:render template="/common/undoRedoControls" model="[key:'workflow']"/>
        </div>
    </g:if>
    <ol id="wfilist_${rkey}" class="flowlist">
        <g:render template="/execution/wflistContent" model="${[workflow:workflow,edit:edit,noimgs:noimgs,project:project]}"/>
    </ol>
    <div id="workflowDropfinal" class="dragdropfinal" data-wfitemnum="${workflow?.commands? workflow.commands.size():0}" style="display:none"></div>
    <div class="empty note ${error?'error':''}" id="wfempty" style="${wdgt.styleVisible(unless:workflow && workflow?.commands)}">
        No Workflow ${g.message(code:'Workflow.step.label')}s
    </div>
    <g:if test="${edit}">
    <div >
    <div id="wfnewbutton" style="margin-top:5px;">
        <span class="btn btn-default btn-sm ready" onclick="$('wfnewtypes').show();$('wfnewbutton').hide();" title="Add a new Workflow ${g.message(code:'Workflow.step.label')} to the end">
            <b class="glyphicon glyphicon-plus"></b>
            Add a ${g.message(code:'Workflow.step.label')}
        </span>
    </div>
    <div id="wfnewtypes" style="display:none; margin-top:10px;" class="panel panel-success">
        <g:render template="/execution/wfAddStep"
            model="[addMessage:'Workflow.step.label.add',chooseMessage:'Workflow.step.label.choose.the.type']"
        />
    </div>

    <div id="wfnew_eh_types" style="display:none;  margin-top:10px;" class="panel panel-success">
        %{--This element is moved around to show the add error-handle buttons for a step--}%
        <g:render template="/execution/wfAddStep"
                model="[addMessage:'Workflow.stepErrorHandler.label.add',descriptionMessage:'Workflow.stepErrorHandler.description',chooseMessage:'Workflow.stepErrorHandler.label.choose.the.type']"
        />
    </div>
    </div>
        <script type="text/javascript">
            fireWhenReady('wfnew_eh_types',function(){
                $('wfnew_eh_types').select('.add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewEHChooseType);
                });
                $('wfnew_eh_types').select('.add_node_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewEHNodeStepType);
                });
                $('wfnew_eh_types').select('.cancel_add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewEHCancel);
                });
            })
            fireWhenReady('wfnewtypes', function () {
                $('wfnewtypes').select('.add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewStepChooseType);
                });
                $('wfnewtypes').select('.add_node_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewNodeStepChooseType);
                });
                $('wfnewtypes').select('.cancel_add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewStepCancel);
                });
            })
        </script>
</g:if>

</div>
<g:if test="${!edit && !isAdhoc}">
    <div>
    <span class="text-muted text-em">
        <g:message code="Workflow.property.keepgoing.prompt"/>
        <strong><g:message
            code="Workflow.property.keepgoing.${workflow?.keepgoing ? true : false}.description"/></strong>
    </span>
    </div>
    <div>
    <span class="text-muted text-em">
        <g:message code="strategy"/>:
        <div id="workflowstrategydetail" data-strategy="${workflow?.strategy}">
            <g:embedJSON id="workflowstrategyconfigdata"
                         data="${workflow?.getPluginConfigData('WorkflowStrategy', workflow?.strategy)}"/>
            <g:render template="/framework/renderPluginConfig"
                      model="[showPluginIcon: true,
                              type          : workflow?.strategy,
                              values        : workflow?.getPluginConfigData('WorkflowStrategy', workflow?.strategy),
                              description   : strategyPlugins.find { it.name == workflow?.strategy }
                      ]"/>
        </div>
    </span>

    </div>
</g:if>
<div class="clear"></div>




<div id="wfnewitem"></div>
