<%@ page import="rundeck.User; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; rundeck.PluginStep; rundeck.CommandExec; rundeck.JobExec" %>
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
<div class=" wfitemEditForm container">
<g:hasErrors bean="${item}">
    <div class="alert alert-danger">
    <g:renderErrors bean="${item}" as="list"/>
    </div>
</g:hasErrors>
<g:render template="/common/messages"/>
<div id="wfiedit_${rkey}">
    <g:if test="${isErrorHandler}">
        <span class="text-info"><g:message code="Workflow.stepErrorHandler.description" /></span>
    </g:if>
%{--Job Reference item--}%
<g:if test="${'job'==newitemtype || item instanceof JobExec || (item instanceof java.util.Map && item?.jobName)}">
<section >
    <div class="form-group">
       <label class="col-sm-2 control-label" for="jobNameField${rkey}">Job Name/Group</label>
        <div class="col-sm-4">

            <input id="jobNameField${rkey}" type="text" name="jobName" value="${enc(attr: item?.jobName)}"
                placeholder="Job Name"
                class="form-control"
                   size="100" autofocus/>
        </div>
        <div class="col-sm-4">
            <input id="jobGroupField${rkey}"  type="text" name="jobGroup" value="${enc(attr:item?.jobGroup)}" size="100"
                placeholder="Group Name"
                class="form-control"
            />
        </div>

        <div class="col-sm-2">
            <span class="btn btn-sm btn-default act_choose_job" onclick="loadJobChooser(this, 'jobChooser','jobNameField${rkey}','jobGroupField${rkey}');"
                  id="jobChooseBtn${rkey}"
                  title="Select an existing Job to use"
                  data-loading-text="Loading...">
                Choose A Job&hellip;
                <i class="caret"></i>
            </span>
            <span id="jobChooseSpinner"></span>
        </div>
    </div>
    <div class="form-group" >
        <label class="col-sm-2 control-label">Arguments</label>
        <div class="col-sm-10">
            <input type='text' name="argString" value="${enc(attr:item?.argString)}" size="100"
                placeholder="Enter arguments, e.g. -option1 value -option2 value"
                   id="jobArgStringField"
                   class="form-control"/>
        </div>
    </div>
    <div class="popout jobChooser" id="jobChooser" style="display:none; width:300px; padding: 5px; background:white; position:absolute;">
        <div style="margin-bottom:5px;">
            <span class="text-muted">Click on the name of the Job to use</span>
            <button type="button" class=" close" style="text-align:right" onclick="hideJobChooser();">
                &times;
            </button>
        </div>
        <div id="jobChooserContent" style="overflow-y:auto;">
        </div>
    </div>

    <div class="row">
    <div class="col-sm-2 control-label">
    <span class="btn   btn-link ${wdgt.css(if: item?.nodeFilter, then: 'active')}"
                data-toggle="collapse" data-target="#nodeFilterOverride${enc(attr: rkey)}">
        Override Node Filters?
        <i class="glyphicon ${wdgt.css(if: item?.nodeFilter, then: 'glyphicon-chevron-down', else: 'glyphicon-chevron-right')} "></i>
    </span>
    </div>
    </div>
    </section>

    <section id="nodeFilterOverride${enc(attr: rkey)}" class="collapse-expandable collapse ${wdgt.css(if: item?.nodeFilter, then: 'in')} node_filter_link_holder section-separator-solo">
    <div class="form-group">
        <div class="col-sm-12 ">
            <div class="text-info">
                <g:message code="JobExec.property.nodeFilter.help.description" />
            </div>
        </div>
    </div>
    <div class="form-group">

        <label class="col-sm-2 control-label" for="nodeFilterField${enc(attr: rkey)}">
            <g:message code="node.filter.prompt"/>
        </label>

        <div class="col-sm-10">
            <g:set var="filtvalue" value="${item?.nodeFilter}"/>

            <span class="input-group nodefilters">
                <g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
                    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
                </g:if>
                <g:render template="/framework/nodeFilterInputGroup"
                          model="[filterFieldName: 'nodeFilter',
                                  filterFieldId:'nodeFilterField'+rkey,
                                  queryFieldHelpId:'nodeFilterQueryFieldHelp'+rkey,
                                  queryFieldPlaceholderText: g.message(code:'enter.a.node.filter.override'),
                                  filterset: filterset,
                                  filtvalue: filtvalue,
                                  filterName: null]"/>
            </span>

            <div class=" collapse" id="nodeFilterQueryFieldHelp${enc(attr: rkey)}">
                <div class="help-block">
                    <g:render template="/common/nodefilterStringHelp"/>
                </div>
            </div>

        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-2 control-label">
            <g:message code="matched.nodes.prompt"/>
        </label>

        <div class=" col-sm-10  ">

            <div class="well well-sm embed matchednodes">
                <button type="button" class="pull-right btn btn-info btn-sm refresh_nodes"
                        data-loading-text="Loading..."
                        data-bind="click: $data.updateMatchedNodes"
                        title="click to refresh">
                    <g:message code="refresh"/>
                    <i class="glyphicon glyphicon-refresh"></i>
                </button>
                <span class="text-muted" data-bind="visible: total()>0">
                    <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="count.nodes.matched"/></span>
                </span>
                <span class="text-muted" data-bind="visible: !filter()">
                    <span data-bind="text: emptyMessage"></span>
                </span>

                <div id='matchednodes${rkey}' class="clearfix ">
                    <g:render template="/framework/nodesEmbedKO" model="[showLoading:true,showTruncated:true,showNone:true]"/>
                </div>
            </div>
        </div>
    </div>

    <div class="form-group">

        <label class="col-sm-2 control-label" for="nodeThreadcountField${rkey}">
            <g:message code="scheduledExecution.property.nodeThreadcount.label"/>
        </label>

        <div class="col-sm-2">
            <input
                    data-bind="enable: filter()"
                    type='number'
                    name="nodeThreadcount"
                    min="1"
                    value="${enc(attr: item?.nodeThreadcount)}"
                    size="3"
                    class="form-control"
                    id="nodeThreadcountField${rkey}"
            />
        </div>
        <div class="col-sm-8 help-block">
            <g:message code="JobExec.property.nodeThreadcount.null.description"/>
        </div>
    </div>

    <div class="form-group">

        <label class="col-sm-2 control-label ">
            <g:message code="scheduledExecution.property.nodeKeepgoing.prompt"/>
        </label>

        <div class="col-sm-10">
            <div class="radio">
                <label >
                    <g:radio name="nodeKeepgoing" value="" checked="${item?.nodeKeepgoing==null}"
                             data-bind="enable: filter()"/>
                    <g:message code="JobExec.property.nodeKeepgoing.null.description"/>
                </label>
            </div>

            <div class="radio">
                <label >
                    <g:radio name="nodeKeepgoing" value="true" checked="${item?.nodeKeepgoing!=null&&item?.nodeKeepgoing}"
                             data-bind="enable: filter()"/>
                    <g:message code="Workflow.property.keepgoing.true.description"/>
                </label>
            </div>

            <div class="radio">
                <label >
                    <g:radio name="nodeKeepgoing" value="false" checked="${item?.nodeKeepgoing!=null&&!item?.nodeKeepgoing}"
                             data-bind="enable: filter()"/>
                    <g:message  code="Workflow.property.keepgoing.false.description"/>
                </label>
            </div>
        </div>
    </div>

        <div class="form-group">

            <label class="col-sm-2 control-label" for="nodeRankAttributeField${rkey}">
                <g:message code="scheduledExecution.property.nodeRankAttribute.label"/>
            </label>

            <div class="col-sm-10">
                <input
                        data-bind="enable: filter()"
                        type='text'
                        name="nodeRankAttribute"
                        value="${enc(attr: item?.nodeRankAttribute)}"
                        class="form-control"
                        placeholder="${enc(code:'scheduledExecution.property.nodeRankAttribute.description',encodeAs:'HTMLAttribute')}"
                        id="nodeRankAttributeField${rkey}"/>
            </div>

        </div>
        <div class="form-group">

            <label class="col-sm-2 control-label" for="nodeRankOrderField${rkey}">
                <g:message code="scheduledExecution.property.nodeRankOrder.label"/>
            </label>

            <div class="col-sm-10">
                <div class="radio">
                    <label>
                        <g:radio name="nodeRankOrderAscending" value=""
                                 checked="${item?.nodeRankOrderAscending == null}"
                                 data-bind="enable: filter()"/>
                        <g:message code="JobExec.property.nodeRankOrder.null.description"/>
                    </label>
                </div>
                <div class="radio">
                    <label>
                        <g:radio name="nodeRankOrderAscending" value="true"
                                 checked="${item?.nodeRankOrderAscending == Boolean.TRUE}"
                                 data-bind="enable: filter()"/>
                        <g:message code="scheduledExecution.property.nodeRankOrder.ascending.label"/>
                    </label>
                </div>
                <div class="radio">
                    <label>
                        <g:radio name="nodeRankOrderAscending" value="false"
                                 checked="${item?.nodeRankOrderAscending == Boolean.FALSE}"
                                 data-bind="enable: filter()"/>
                        <g:message code="scheduledExecution.property.nodeRankOrder.descending.label"/>
                    </label>
                </div>
            </div>
        </div>
    </section>
    <section>
    <div class="form-group">
        <g:set var="isNodeStep" value="${item ? item.nodeStep : newitemnodestep == 'true'}"/>
        <label class="col-sm-2 control-label"><g:message code="JobExec.nodeStep.title" /></label>
        <div class="col-sm-10">
            <div class="radio">
                <label>
                    <g:radio id="jobNodeStepFieldTrue"  name="nodeStep" value="true"
                             checked="${!!isNodeStep}"/>

                    <g:message code="JobExec.nodeStep.true.label" />
                </label>
                <span class="text-muted"><g:message code="JobExec.nodeStep.true.description"/></span>
            </div>
            <div class="radio">
                <label>
                    <g:radio id="jobNodeStepFieldFalse"  name="nodeStep" value="false"
                             checked="${!isNodeStep}"/>

                    <g:message code="JobExec.nodeStep.false.label" />
                </label>
                <span class="text-muted"><g:message code="JobExec.nodeStep.false.description"/></span>
            </div>
        </div>
    </div>
    </section>
    <g:embedJSON id="jobrefFilterParamsJSON${rkey}" data="${[filter: item?.nodeFilter]}"/>
    <g:javascript>
            fireWhenReady("nodeFilterOverride${rkey}",function(){
                setupJobExecNodeFilterBinding('#nodeFilterOverride${rkey}','matchednodes${rkey}','jobrefFilterParamsJSON${rkey}');
            });
    </g:javascript>
</g:if>
%{--Script or Command item--}%
<g:elseif test="${(newitemtype in ['command','script','scriptfile']) || item instanceof CommandExec }">
    <g:set var="isAdhocRemote" value="${'command'==newitemtype || 'command'==origitemtype || item?.adhocRemoteString}"/>
    <g:set var="isAdhocLocal" value="${'script'==newitemtype ||'script'==origitemtype || item?.adhocLocalString}"/>
    <g:set var="isAdhocFileExecution" value="${'scriptfile'==newitemtype ||'scriptfile'==origitemtype || item?.adhocFilepath}"/>
    <g:hiddenField name="adhocExecution" value="true"/>
    <div id="scriptStep_${rkey}">
    <g:if test="${isAdhocLocal}">
        <div id="localScriptDiv" class="form-group ${hasErrors(bean:item,field:'adhocLocalString','has-error')}">
            <label class="col-sm-12 text-form-label" for="adhocLocalStringField${rkey}">
                <g:message code="Workflow.Step.adhocLocalString.description" />
            </label>
            <div class="col-sm-12">
                <textarea rows="10" cols="60" name="adhocLocalString" id="adhocLocalStringField${rkey}" class="form-control code apply_ace" data-ace-autofocus='true' ><g:enc>${item?.adhocLocalString}</g:enc></textarea>
            </div>
        </div>
    </g:if>
    <g:elseif test="${isAdhocFileExecution}">
    <div id="filepathDiv" class="form-group ${hasErrors(bean:item,field:'adhocFilepath','has-error')}">
        <label class="col-sm-2 control-label"><g:message code="Workflow.Step.adhocFilepath.label" /></label>
        <div class="col-sm-10">
            <input
                    type='text'
                    name="adhocFilepath"
                    value="${enc(attr:item?.adhocFilepath)}"
                    class="form-control"
                    id="adhocFilepathField"
                    placeholder="${g.enc(code:'Workflow.Step.adhocFilepath.description',encodeAs:'HTMLAttribute')}"
                    autofocus
            />
        </div>
    </div>
    </g:elseif>
    <g:elseif test="${isAdhocRemote}">
    <div id="remoteScriptDiv"  class="form-group  ${hasErrors(bean:item,field:'adhocRemoteString','has-error')}">
        <label class="col-sm-2 control-label"><g:message code="Workflow.Step.adhocRemoteString.label" /></label>
        <div class="col-sm-10">
            <input
                    type='text'
                    name="adhocRemoteString"
                    value="${enc(attr:item?.adhocRemoteString)}"
                    class="form-control"
                    placeholder="${g.enc(code:'Workflow.Step.adhocRemoteString.description',encodeAs:'HTMLAttribute')}"
                    id="adhocRemoteStringField"
                    autofocus/>
        </div>
    </div>
    </g:elseif>
    <g:if test="${!isAdhocRemote||isAdhocFileExecution}">
    <div id="adhocScriptArgs" class="form-group" >
        <label class="control-label col-sm-2">
            <g:message code="Workflow.Step.argString.label" />
        </label>
        <div class="col-sm-10">
            <input type='text'
                   name="argString"
                   value="${enc(attr:item?.argString)}"
                   class="form-control"
                   id="argStringField"
                   placeholder="${enc(code:'Workflow.Step.argString.description',encodeAs:'HTMLAttribute')}"
                   data-bind="value: args, valueUpdate: 'keyup'"
            />
        </div>
    </div>
    </g:if>
    <g:if test="${!isAdhocRemote}">
        <g:set var="hasAdvanced" value="${item?.scriptInterpreter || item?.interpreterArgsQuoted || item?.fileExtension}"/>
        <div class="row">
            <div class="col-sm-2 control-label">
                <span class="btn btn-link ${wdgt.css(if: hasAdvanced, then:'active')}" data-toggle="collapse" data-target="#scriptInterpreter${rkey}">
                    Advanced
                    <i class="glyphicon ${wdgt.css(if: hasAdvanced, then: 'glyphicon-chevron-down', else:'glyphicon-chevron-right')} "></i>
                </span>
            </div>
        </div>
        <div id="scriptInterpreter${enc(attr:rkey)}" class="collapse-expandable collapse ${wdgt.css(if: hasAdvanced, then: 'in')}">
            <div class="form-group">

                <label class="col-sm-2 control-label"
                       for="scriptInterpreterField${rkey}"><g:message
                        code="Workflow.Step.scriptInterpreter.label"/></label>
                <div class="col-sm-10">
                    <div class=""
                         id="interpreterHelp${enc(attr: rkey)}_tooltip"
                         style="display:none;">
                        <div class="help-block"><g:message
                                code="Workflow.Step.scriptInterpreter.help"/></div>
                    </div>
                    <span class="input-group">

                        <input type='text' name="scriptInterpreter"
                               placeholder="${enc(attr:g.message(code: 'Workflow.Step.scriptInterpreter.prompt'))}"
                               value="${enc(attr:item?.scriptInterpreter)}" size="100"
                            class="form-control"
                            data-bind="value: invocationString, valueUpdate: 'keyup'"
                               id="scriptInterpreterField${rkey}" autofocus/>

                        <div class="input-group-addon">
                            <span class="action "
                                data-toggle="popover"
                                data-placement="left"
                                data-trigger="hover"
                                data-popover-content-ref="#interpreterHelp${enc(attr: rkey)}_tooltip"
                                  id="interpreterHelp${enc(attr: rkey)}"><i
                                    class="glyphicon glyphicon-question-sign  text-info"></i></span>

                        </div>


                    </span>
                </div>
            </div>

            <div class="form-group">
                  <div class="col-sm-offset-2 col-sm-10">
                      <div class="checkbox">
                        <label>
                            <g:checkBox name="interpreterArgsQuoted"
                                        checked="${item?.interpreterArgsQuoted}"
                                        id="interpreterArgsQuotedField" value="true"
                                        data-bind="checked: argsQuoted"/>
                                <g:message code="Workflow.Step.interpreterArgsQuoted.label"/>
                        </label>
                          <span class="action"

                                data-toggle="popover"
                                data-placement="bottom"
                                data-trigger="hover"
                                data-popover-content-ref="#interpreterArgsQuotedHelp${enc(attr: rkey)}_tooltip"
                                id="interpreterArgsQuotedHelp${enc(attr: rkey)}"><i
                                  class="glyphicon glyphicon-question-sign  text-info"></i>
                          </span>
                        <div class=""
                             id="interpreterArgsQuotedHelp${enc(attr: rkey)}_tooltip"
                             style="display:none; ">
                            <div class="help-block"><g:message
                                    code="Workflow.Step.interpreterArgsQuoted.help"/></div>
                        </div>

                    </div>
                </div>
            </div>

            <div class="form-group">

                <label class="col-sm-2 control-label"
                       for="fileExtensionField${rkey}"><g:message
                        code="Workflow.Step.fileExtension.label"/></label>

                <div class="col-sm-10">
                    <div class="popout tooltipcontent helptooltip"
                         id="fileExtensionHelp${enc(attr: rkey)}_tooltip"
                         style="display: none;">
                        <div class="panel-body">
                        <div class="help-block"><g:message
                                code="Workflow.Step.fileExtension.help"/></div>
                        </div>
                    </div>
                    <span class="input-group">

                        <input type='text' name="fileExtension"
                               placeholder="${enc(attr: g.message(code: 'Workflow.Step.fileExtension.prompt'))}"
                               value="${enc(attr: item?.fileExtension)}" size="100"
                               class="form-control"
                               data-bind="value: fileExtension, valueUpdate: 'keyup'"
                               id="fileExtensionField${rkey}" />

                        <div class="input-group-addon">
                            <span class="action"
                                  data-toggle="popover"
                                  data-placement="left"
                                  data-trigger="hover"
                                  data-popover-content-ref="#fileExtensionHelp${enc(attr: rkey)}_tooltip"
                                  id="fileExtensionHelp${enc(attr: rkey)}"><i
                                    class="glyphicon glyphicon-question-sign  text-info"></i></span>

                        </div>

                    </span>
                </div>
            </div>
        </div>
        <div>
        <div class="form-group">
            <div class="col-sm-2 control-label">Execution Preview</div>

                <div id='interpreterArgsQuotedHelp${rkey}_preview' class="col-sm-10 form-control-static">
                    <code>$ <span data-bind="html: invocationPreviewHtml"></span></code>
                </div>

                <g:embedJSON id="scriptStepData_${rkey}" data="${[invocationString: item?.scriptInterpreter?:'',fileExtension: item?.fileExtension?:'',args: item?.argString?:'',argsQuoted: item?.interpreterArgsQuoted?true:false]}"/>
                <g:javascript>
                fireWhenReady("scriptStep_${rkey}",function(){
                    workflowEditor.bindKey('${rkey}','scriptStep_${rkey}',loadJsonData('scriptStepData_${rkey}'));
                    if (typeof(_initPopoverContentRef) == 'function') {
                        _initPopoverContentRef("#scriptStep_${rkey}");
                    }
                });
                </g:javascript>
            </div>
        </div>
    </g:if>
    </div>
</g:elseif>
<g:elseif test="${( newitemtype || item && item.instanceOf(PluginStep) ) && newitemDescription}">
    <div>
        <div>
            <span class="h4"><g:enc>${newitemDescription.title}</g:enc></span>
            <span class="help-block">
                <g:render template="/scheduledExecution/description"
                          model="[description: newitemDescription.description,
                                  textCss: '',
                                  mode: 'collapsed',
                                  moreText:'More Information',
                                  rkey: g.rkey()]"/>
            </span>
        </div>
        <g:hiddenField name="pluginItem" value="true"/>
        <g:hiddenField name="newitemnodestep" value="${item?!!item.nodeStep:newitemnodestep=='true'}"/>


        <div>
            <g:set var="pluginprefix" value="pluginConfig."/>
            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                    properties:newitemDescription.properties,
                    report:report,
                    prefix:pluginprefix,
                    values:item?.configuration,
                    fieldnamePrefix:pluginprefix,
                    origfieldnamePrefix:'orig.' + pluginprefix,
                    allowedScope:PropertyScope.Instance
            ]}"/>

        </div>
    </div>
</g:elseif>
<g:elseif test="${( newitemtype || item && item.instanceOf(PluginStep) ) && pluginNotFound}">
    <div>
        <div>
            <span class="text-danger invalidProvider">Provider not found</span>
            <span class="help-block text-danger">
                The step plugin provider "${newitemtype?:item?.type}" is not installed, or could not be loaded.
            </span>
        </div>
        <g:hiddenField name="pluginItem" value="true"/>
        <g:hiddenField name="newitemnodestep" value="${item?!!item.nodeStep:newitemnodestep=='true'}"/>

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
    <div class="form-group">
        <label class="col-sm-2 control-label" for="description${rkey}">Step Description</label>
        <div class="col-sm-10">
        <input id="description${rkey}" type="text" name="description" value="${enc(attr:item?.description)}"
            class="form-control"
            placeholder="Description of this step"
               size="100"/>
        </div>
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
            <g:hiddenField name="origitemtype" value="${origitemtype}"/>
            <span class="btn btn-default btn-sm" onclick="_wfiview('${key}',${num},${isErrorHandler?true:false});" title="Discard changes to the ${g.message(code:'Workflow.'+ msgItem+'.label')}">Discard</span>
            <span class="btn btn-primary btn-sm" onclick="_wfisave('${key}',${num}, 'wfiedit_${rkey}', ${ isErrorHandler?true:false});"
                  title="Save changes to the ${g.message(code:'Workflow.'+ msgItem+'.label')}">Save</span>
        </g:else>
    </div>
    <div class="clear"></div>
</div>
</div>
