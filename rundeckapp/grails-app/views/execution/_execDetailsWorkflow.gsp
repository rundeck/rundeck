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
<g:set var="scheduledExecutionUuid" value="${(context === null) ? (context instanceof rundeck.Workflow ? null: null) : (context instanceof rundeck.ScheduledExecution ? context?.uuid : null) }" />
<g:unless test="${isAdhoc}">
<g:if test="${edit}">
<div>
    <div class=""><g:message code="Workflow.property.keepgoing.prompt" /></div>
    <div class="radio radio-inline">
      <input type="radio" id="workflowKeepGoingFail" name="workflow.keepgoing" value="false" ${workflow?.keepgoing?'':'checked'}/>
      <label for="workflowKeepGoingFail">
          <g:message code="Workflow.property.keepgoing.false.description"/>
      </label>
    </div>
    <div class="radio radio-inline">
      <input type="radio" id="workflowKeepGoingRemainingSteps" name="workflow.keepgoing" value="true" ${workflow?.keepgoing?'checked':''}/>
      <label for="workflowKeepGoingRemainingSteps">
          <g:message code="Workflow.property.keepgoing.true.description"/>
      </label>
    </div>
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
               value="${((params.workflow instanceof Map)?(params.workflow?.strategyPlugin?.get(pluginName)?.config):null) ?: wfstrat == pluginName?workflow?.getPluginConfigData('WorkflowStrategy',pluginName):null}"/>
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
<hr>
    <g:set var="workflowLogFilterPluginConfigs" value="${workflow?.getPluginConfigDataList('LogFilter')}"/>
    <g:if test="${logFilterPlugins}">
        <div id="logfilterplugins_wf" style="margin: 10px 0;">
            <div class="">
                <div class="${hasErrors(bean: workflow, field: 'strategy', 'has-error')}">
                    <label class="">
                      <g:message code="global.log.filters" />
                      <span class="btn btn-default btn-xs" data-bind="click: addFilterPopup">
                          <g:icon name="plus"/>
                          <g:message code="add" />
                      </span>
                    </label>

                    <div>
                        <!-- ko foreach: {data: filters, as: 'filter' } -->
                        <div class="btn-group" style="margin-top:15px;">
                          <span class="btn btn-sm btn-default autohilite"
                                style="border-right:0;"
                                data-bind="click: $root.editFilter"
                                title="${message(code:"click.to.edit.filter")}">
                              <!-- ko if: plugin() -->
                              <!-- ko with: plugin() -->
                              <!-- ko if: iconSrc -->
                              <img width="16px" height="16px" data-bind="attr: {src: iconSrc}"/>
                              <!-- /ko -->
                              <!-- ko if: glyphicon -->
                              <i data-bind="css: 'glyphicon glyphicon-'+glyphicon()"></i>
                              <!-- /ko -->
                              <!-- ko if: faicon -->
                              <i data-bind="css: 'fas fa-'+faicon()"></i>
                              <!-- /ko -->
                              <!-- ko if: fabicon -->
                              <i data-bind="css: 'fab fa-'+fabicon()"></i>
                              <!-- /ko -->
                              <!-- ko if: !iconSrc() && !glyphicon() && !faicon() && !fabicon() -->
                              <i class="rdicon icon-small plugin"></i>
                              <!-- /ko -->
                              <!-- /ko -->
                              <!-- /ko -->
                              <span data-bind="text: title"></span>
                          </span>
                          <!--define hidden inputs for the configured filter -->
                          <input type="hidden"
                                 data-bind="attr: { name: 'workflow.globalLogFilters.'+index()+'.type', value: type}"/>
                          <!--config values-->
                          <span data-bind="foreachprop: config">
                              <input type="hidden"
                                     data-bind="attr: { name: 'workflow.globalLogFilters.'+filter.index()+'.config.'+key, value: value}"/>
                          </span>
                          <span class="btn btn-danger btn-sm"
                                data-bind="click: $root.removeFilter"
                                title="${message(code:"remove.filter")}">
                              <g:icon name="remove"/></span>
                        </div>
                        <!-- /ko -->

                        <g:embedJSON id="logFilterData_wf" data="${[
                                global: true,
                                description: "All workflow steps",
                                filters: workflowLogFilterPluginConfigs ?: []
                        ]
                        }"/>
                        <script type="text/javascript">
                            fireWhenReady("logfilterplugins_wf", function () {
                                var step = workflowEditor.bindStepFilters('logfilterplugins_wf', 'logfilterplugins_wf', loadJsonData('logFilterData_wf'), {
                                    editor: function (x) {
                                        return new WorkflowGlobalLogFilterEditor(x);
                                    }
                                });
                            });
                        </script>
                    </div>
                </div>
            </div>
        </div>
    </g:if>
    <hr>
</g:if>
</g:unless>
<div class="pflowlist ${edit?'edit':''} rounded ${isAdhoc?'adhoc':''}" style="">
    <g:if test="${edit}">
        <div id="wfundoredo" class="undoredocontrols">
            <g:render template="/common/undoRedoControls" model="[key:'workflow']"/>
        </div>
    </g:if>
    <g:if test="${true}">
        <div id="stepsDashboard_container" style="width: 100%; height: 150px; margin-bottom: 2rem;">
            <g:render template="/execution/stepsDashboard" model="${[workflow: workflow]}" />
        </div>
        <script>
            fireWhenReady('stepsDashboard_container', function (){
                if( '${scheduledExecutionUuid}'.length > 1 ){
                    jQuery('#stepsDashboard_container').data('SeUuid', '${scheduledExecutionUuid}')
                }
                _loadDashboard('${scheduledExecutionUuid}');
            })
        </script>
    </g:if>
    <ol id="wfilist_${rkey}" class="flowlist">
        <g:render template="/execution/wflistContent" model="${[workflow:workflow,edit:edit,noimgs:noimgs,project:project]}"/>
    </ol>

    <div class="empty note ${error?'error':''}" id="wfempty" style="${wdgt.styleVisible(unless:workflow && workflow?.commands)}">
        No Workflow ${g.message(code:'Workflow.step.label')}s
    </div>
    <g:if test="${edit}">
    <div >
    <div id="wfnewbutton" style="margin-top:5px;">
        <span class="btn btn-default btn-sm ready" onclick="jQuery('#wfnewtypes').show();jQuery('#wfnewbutton').hide();" title="Add a new Workflow ${g.message(code:'Workflow.step.label')} to the end">
            <b class="glyphicon glyphicon-plus"></b>
            Add a ${g.message(code:'Workflow.step.label')}
        </span>
    </div>
    <div id="wfnewtypes" style="display:none; margin-top:10px; margin-left:20px;" class="panel panel-default">
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

                jQuery('#wfnew_eh_types').find( '.add_step_type' ).each(function (indx,e) {
                    e.addEventListener('click', _evtNewEHChooseType, false);
                });
                jQuery('#wfnew_eh_types').find( '.add_node_step_type' ).each(function (indx, e) {
                    e.addEventListener('click', _evtNewEHNodeStepType, false);
                });
                jQuery('#wfnew_eh_types').find( '.cancel_add_step_type' ).each(function ( indx, e) {
                    e.addEventListener('click', _evtNewEHCancel, false);
                });
            })
            fireWhenReady('wfnewtypes', function () {
                jQuery('#wfnewtypes').find( '.add_step_type' ).each(function (indx, e) {
                    e.addEventListener('click', _evtNewStepChooseType, false);
                });
                jQuery('#wfnewtypes').find( '.add_node_step_type' ).each(function (indx, e) {
                    e.addEventListener('click', _evtNewNodeStepChooseType, false);
                });
                jQuery('#wfnewtypes').find( '.cancel_add_step_type' ).each(function (indx, e) {
                    e.addEventListener('click', _evtNewStepCancel, false);
                });
            })
        </script>
</g:if>

</div>
<g:if test="${!edit && !isAdhoc}">
    <div>
    <span class="text-strong text-em">
        <g:message code="Workflow.property.keepgoing.prompt"/>
        <strong><g:message
            code="Workflow.property.keepgoing.${workflow?.keepgoing ? true : false}.description"/></strong>
    </span>
    </div>
    <div>
    <span class="text-strong text-em">
        <g:message code="strategy"/>:
        <div id="workflowstrategydetail" data-strategy="${workflow?.strategy}">
            <g:embedJSON id="workflowstrategyconfigdata"
                         data="${workflow?.getPluginConfigData('WorkflowStrategy', workflow?.strategy)}"/>
            <g:render template="/framework/renderPluginConfig"
                      model="[showPluginIcon: true,
                              serviceName   : 'WorkflowStrategy',
                              type          : workflow?.strategy,
                              values        : workflow?.getPluginConfigData('WorkflowStrategy', workflow?.strategy),
                              description   : strategyPlugins.find { it.name == workflow?.strategy }
                      ]"/>
        </div>
    </span>

    </div>
    <g:set var="workflowLogFilterPluginConfigs" value="${workflow?.getPluginConfigDataList('LogFilter')}"/>
    <g:if test="${workflowLogFilterPluginConfigs}">
        <div>

            <span class="text-strong text-em">
                Log Filters:
                <div id="workflowlogfilterdetail">
                    <g:embedJSON id="workflowlogfilterconfigdata"
                                 data="${[filters: workflowLogFilterPluginConfigs]}"/>
                    <g:each in="${workflowLogFilterPluginConfigs}" var="config">

                        <g:render template="/framework/renderPluginConfig"
                                  model="[showPluginIcon: true,
                                          serviceName   : 'LogFilter',
                                          type          : config.type,
                                          values        : config.config,
                                          description   : logFilterPlugins?.values()?.
                                                  find { it.name == config.type }?.description
                                  ]"/>
                    </g:each>
                </div>
            </span>
        </div>
    </g:if>
</g:if>
<div class="clear"></div>




<div id="wfnewitem"></div>
