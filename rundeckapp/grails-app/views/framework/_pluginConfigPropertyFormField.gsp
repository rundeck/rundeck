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
    _pluginConfigProperty.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 7/28/11 12:01 PM
 --%>

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; com.dtolabs.rundeck.plugins.ServiceNameConstants; com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants" contentType="text/html;charset=UTF-8" %>
%{--<g:set var="fieldname" value="${}"/>--}%
%{--<g:set var="origfieldname" value="${}"/>--}%
<g:set var="labelColType" value="col-sm-2 control-label input-sm"/>
<g:set var="valueColType" value="col-sm-10"/>
<g:set var="valueColTypeSplitA" value="col-sm-5"/>
<g:set var="valueColTypeSplitB" value="col-sm-5"/>
<g:set var="valueColTypeSplit80" value="col-sm-8"/>
<g:set var="valueColTypeSplit20" value="col-sm-2"/>
<g:set var="offsetColType" value="col-sm-10 col-sm-offset-2"/>
<g:set var="formControlType" value="form-control input-sm"/>
<g:set var="formControlCodeType" value="form-control code apply_ace"/>
<g:set var="hasError" value="${error ? 'has-error' : ''}"/>
<g:set var="required" value="${prop.required ? 'required' : ''}"/>
<g:set var="propScope"
       value="${prop.scope != null && prop.scope != PropertyScope.Unspecified ? prop.scope : defaultScope}"/>
<g:unless test="${outofscopeOnly && propScope == PropertyScope.InstanceOnly}">
    <g:set var="valueText" value="${values && null != values[prop.name] ? values[prop.name] : prop.defaultValue}"/>
    <g:set var="propkey" value="${g.rkey()}"/>
    <g:set var="fieldid" value="${g.rkey()}"/>
    <g:embedJSON id="${propkey}_json" data="${[
            service         : service,
            provider        : provider,
            name            : prop.name,
            origfieldname   : origfieldname,
            fieldname       : fieldname,
            fieldid         : fieldid,
            value           : valueText,
            project         : project ?: params.project ?: request.project,
            renderingOptions: prop.renderingOptions

    ]}"/>
    <g:javascript>"use strict";

    jQuery(function () {
        if(typeof(window.PluginProperties)!=='object'){
            window.PluginProperties={};
        }
        var json=loadJsonData('${propkey}_json');
        var prop = new PluginProperty(json);
        ko.applyBindings(prop,jQuery('#${propkey}')[0]);
        PluginProperties['${propkey}']=prop;
    })
    </g:javascript>
    <div class="form-group ${enc(attr: hasError)}" id="${propkey}">
    <g:if test="${outofscope}">
        <label class="${labelColType} form-control-static ${error ? 'has-error' : ''}  ${prop.required ? 'required' :
                                                                                         ''}">
            <stepplugin:message
                    service="${service}"
                    name="${provider}"
                    code="${messagePrefix}property.${prop.name}.title"
                    default="${prop.title ?: prop.name}"/>:
        </label>
    </g:if>
    <g:elseif test="${prop.type.toString() == 'Boolean'}">

        <div class="${offsetColType}">
            <g:hiddenField name="${origfieldname}" value="${values && values[prop.name] ? values[prop.name] : ''}"/>
            <div class="checkbox">
                <label
                        for="${enc(attr: fieldid)}">
                    <g:checkBox name="${fieldname}" value="true"
                                checked="${values && values[prop.name] ? values[prop.name] == 'true' :
                                           prop.defaultValue == 'true'}"
                                id="${fieldid}"/>
                    <stepplugin:message
                            service="${service}"
                            name="${provider}"
                            code="${messagePrefix}property.${prop.name}.title"
                            default="${prop.title ?: prop.name}"/>
                </label>
            </div>
        </div>
    </g:elseif>
    <g:elseif test="${prop.type.toString() == 'Select' || prop.type.toString() == 'FreeSelect'}">

        <label class="${labelColType}   ${prop.required ? 'required' : ''}"
               for="${enc(attr: fieldid)}"><stepplugin:message
                service="${service}"
                name="${provider}"
                code="${messagePrefix}property.${prop.name}.title"
                default="${prop.title ?: prop.name}"/></label>

        <g:hiddenField name="${origfieldname}" value="${values && values[prop.name] ? values[prop.name] : ''}"/>
        <g:set var="inputValues" value="${(prop.selectLabels ?: [:])}"/>
        <g:if test="${prop.type.toString() == 'FreeSelect'}">
            <div class="${valueColTypeSplitA}">
                <g:textField name="${fieldname}"
                             value="${inputValues && null != inputValues[prop.name] ? inputValues[prop.name] :
                                      prop.defaultValue}"
                             id="${fieldid}" size="100" class="${formControlType}${extraInputCss}"/>
            </div>

            <div class="${valueColTypeSplitB}">
                <g:set var="propSelectLabels" value="${dynamicPropertiesLabels ?: (prop.selectLabels ?: [:])}"/>
                <g:set var="selectValues" value="${dynamicProperties ?: (prop.selectValues ?: [:])}"/>
                <g:set var="propSelectValues" value="${selectValues.collect {
                    [key: it.encodeAsHTML(), value: (propSelectLabels[it] ?: it)]
                }}"/>
                <g:select name="${fieldid + '_sel'}" from="${propSelectValues}" id="${fieldid}"
                          optionKey="key" optionValue="value"
                          value="${(values && null != values[prop.name] ? values[prop.name] : prop.defaultValue)?.
                                  encodeAsHTML()}"
                          noSelection="['': '-choose a value-']"
                          onchange="if(this.value){\$('${fieldid}').value=this.value;}"
                          class="${formControlType}"/>
            </div>
        </g:if>
        <g:else>
            <g:set var="propSelectLabels" value="${dynamicPropertiesLabels ?: (prop.selectLabels ?: [:])}"/>
            <g:set var="selectValues" value="${dynamicProperties ?: (prop.selectValues ?: [:])}"/>
            <g:set var="propSelectValues"
                   value="${selectValues.collect { [key: it.encodeAsHTML(), value: (propSelectLabels[it] ?: it)] }}"/>
            <g:set var="noSelectionValue" value="${prop.required ? null : ['': '-none selected-']}"/>
            <div class="${valueColType}">
                <g:select name="${fieldname}" from="${propSelectValues}" id="${fieldid}"
                          optionKey="key" optionValue="value"
                          noSelection="${noSelectionValue}"
                          value="${(values && null != values[prop.name] ? values[prop.name] : prop.defaultValue)?.
                                  encodeAsHTML()}"
                          class="${formControlType}"/>
            </div>
        </g:else>
    </g:elseif>
    <g:elseif test="${prop.type.toString() == 'Options'}">

        <label class="${labelColType}   ${prop.required ? 'required' : ''}"
               for="${enc(attr: fieldid)}"><stepplugin:message
                service="${service}"
                name="${provider}"
                code="${messagePrefix}property.${prop.name}.title"
                default="${prop.title ?: prop.name}"/></label>

        <g:hiddenField name="${origfieldname}" value="${values && values[prop.name] ? values[prop.name] : ''}"/>

        <g:set var="propSelectLabels" value="${dynamicPropertiesLabels ?: (prop.selectLabels ?: [:])}"/>
        <g:set var="selectValues" value="${dynamicProperties ?: (prop.selectValues ?: [:])}"/>
        <g:set var="propSelectValues"
               value="${selectValues.collect { [value: it, label: (propSelectLabels[it] ?: it)] }}"/>
        <g:set var="noSelectionValue" value="${prop.required ? null : ['': '-none selected-']}"/>

        <g:set var="defval" value="${values && null != values[prop.name] ? values[prop.name] : prop.defaultValue}"/>
        <g:set var="defvalset" value="${defval && defval instanceof String ? defval.split(', *') :
                                        defval && defval instanceof Collection ? defval :
                                        []}"/>

        <div class="${valueColType} ">
            <div class=" grid">

                <g:each in="${propSelectValues}" var="propval">
                    <div class="optionvaluemulti ">
                        <label class="grid-row optionvaluemulti">
                            <span class="grid-cell grid-front">
                                <g:checkBox name="${fieldname}" checked="${propval.value in defvalset}"
                                            value="${propval.value}"/>
                            </span>
                            <span class="grid-cell grid-rest">
                                ${propval.label}
                            </span>
                        </label>
                    </div>
                </g:each>
            </div>
        </div>
    </g:elseif>
%{-- todo: Map type --}%
    <g:else>

        <g:set var="hasStorageSelector"
               value="${prop.renderingOptions?.(StringRenderingConstants.SELECTION_ACCESSOR_KEY) in [StringRenderingConstants.SelectionAccessor.STORAGE_PATH, 'STORAGE_PATH']}"/>
        <g:set var="hasJobSelector"
               value="${prop.renderingOptions?.(StringRenderingConstants.SELECTION_ACCESSOR_KEY) in [StringRenderingConstants.SelectionAccessor.RUNDECK_JOB, 'RUNDECK_JOB']}"/>
        <g:set var="hasJobOptionsSelector"
               value="${prop.renderingOptions?.(StringRenderingConstants.SELECTION_ACCESSOR_KEY) in [StringRenderingConstants.SelectionAccessor.RUNDECK_JOB_OPTIONS, 'RUNDECK_JOB_OPTIONS']}"/>
        <g:set var="hasSelector" value="${hasStorageSelector || hasJobSelector || hasJobOptionsSelector}"/>
        <label class="${labelColType}  ${prop.required ? 'required' : ''}"
               for="${enc(attr: fieldid)}"><stepplugin:message
                service="${service}"
                name="${provider}"
                code="${messagePrefix}property.${prop.name}.title"
                default="${prop.title ?: prop.name}"/></label>

        <div class="${hasSelector ? valueColTypeSplit80 : valueColType}">
            <g:hiddenField name="${origfieldname}" value="${values && values[prop.name] ? values[prop.name] : ''}"/>
            <g:if test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in [StringRenderingConstants.DisplayType.MULTI_LINE, 'MULTI_LINE']}">
                <g:textArea name="${fieldname}" value="${valueText}"
                            id="${fieldid}" rows="10" cols="100" class="${formControlType}"/>
            </g:if>
            <g:elseif
                    test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in [StringRenderingConstants.DisplayType.CODE, 'CODE']}">
                <g:set var="syntax" value="${prop.renderingOptions?.(StringRenderingConstants.CODE_SYNTAX_MODE)}"/>
                <g:set var="syntaxSelectable"
                       value="${prop.renderingOptions?.(StringRenderingConstants.CODE_SYNTAX_SELECTABLE)}"/>
                <g:textArea name="${fieldname}" value="${valueText}"
                            data-ace-session-mode="${syntax}"
                            data-ace-control-syntax="${syntaxSelectable ? true : false}"
                            id="${fieldid}" rows="10" cols="100" class="${formControlCodeType}${extraInputCss}"/>
            </g:elseif>
            <g:elseif
                    test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in [StringRenderingConstants.DisplayType.PASSWORD, 'PASSWORD']}">
                <g:passwordField name="${fieldname}" value="${valueText}"
                                 autocomplete="new-password"
                                 id="${fieldid}" cols="100" class="${formControlType}"/>
            </g:elseif>
            <g:elseif
                    test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in [StringRenderingConstants.DisplayType.STATIC_TEXT, 'STATIC_TEXT']}">
            %{--display value/defaultValue as static text in some format--}%
            %{--text/html--}%
                <g:set var="staticTextValue" value="${
                    stepplugin.messageText(
                            service: service,
                            name: provider,
                            code: 'property.' + prop.name + '.defaultValue',
                            default: prop.defaultValue
                    )
                }"/>
                <g:if test="${prop.renderingOptions?.(StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY) in ['text/html']}">
                    <g:enc sanitize="${staticTextValue}"/>
                </g:if>
                <g:elseif
                        test="${prop.renderingOptions?.(StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY) in ['text/x-markdown']}">
                %{--markdown--}%
                    <g:markdown>${staticTextValue}</g:markdown>
                </g:elseif>
                <g:else>
                %{--plain--}%
                    <g:enc html="${staticTextValue}"/>
                </g:else>
            </g:elseif>
            <g:elseif test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in ['RUNDECK_JOB']}">


                <span data-bind="visible: !toggle.value()">
                    <span data-bind="if: value">
                        <job-link params="id: value, project:project, action: toggle.toggle"></job-link>
                    </span>
                    <span data-bind="if: !value()" class="text-info">
                        Select a Job
                    </span>
                </span>
                <span class="textbtn textbtn-info" data-bind="click: toggle.toggle, visible: !toggle.value()">
                    <g:icon name="pencil"/>
                </span>
                <span data-bind="visible: toggle.value">
                    <input name="${fieldname}"
                           id="${fieldid}"
                           value="${valueText}"
                           data-bind="value: value, valueUpdate:'input', hasFocus: toggle.value, executeOnEnter: toggle.toggle"
                           placeholder="Job UUID"
                           type="text" class="form-control"/>

                </span>

            </g:elseif>
            <g:elseif test="${prop.renderingOptions?.(StringRenderingConstants.SELECTION_ACCESSOR_KEY) in ['datetime']}">

                <div class='input-group date'
                     data-bind="datetimepicker: value, dateFormat: renderingOptions()['dateFormat'], minDate: 'now'">
                    <span class='input-group-addon'>
                        <span class='glyphicon glyphicon-calendar'></span>
                    </span>
                    <input name="${fieldname}"
                           id="${fieldid}"
                           value="${valueText}"
                           data-bind="value: value, attr: { placeholder: renderingOptions()['dateFormat']}"
                           placeholder="Date/time"
                           type="datetime" class="form-control"/>
                </div>

            </g:elseif>
            <g:else>
                <g:textField name="${fieldname}" value="${valueText}"
                             id="${fieldid}" size="100" class="${formControlType}${extraInputCss}"/>
            </g:else>
        </div>
        <g:if test="${hasStorageSelector}">
            <div class="${valueColTypeSplit20}">
                %{-- selector for accessible storage --}%
                <g:set var="storageRoot"
                       value="${prop.renderingOptions?.(StringRenderingConstants.STORAGE_PATH_ROOT_KEY) ?: '/'}"/>
                <g:set var="storageFilter"
                       value="${prop.renderingOptions?.(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY) ?: ''}"/>
                <a class="btn btn-sm btn-default obs-select-storage-path"
                   data-toggle="modal"
                   href="#storagebrowse"
                   data-storage-root="${enc(attr: storageRoot)}"
                   data-storage-filter="${enc(attr: storageFilter)}"
                   data-field="#${enc(attr: fieldid)}"><g:message code="select"/> <i
                        class="glyphicon glyphicon-folder-open"></i></a>
            </div>
        </g:if>
        <g:if test="${hasJobSelector}">
            <div class="${valueColTypeSplit20}">
                %{-- selector for a job --}%

                <g:set var="pjkey" value="${g.rkey()}"/>
                <g:set var="selectionComponent"
                       value="${prop.renderingOptions.get(StringRenderingConstants.SELECTION_COMPONENT_KEY) ?:
                                'uuid'}"/>

                <span class="btn  btn-default act_choose_job"
                      onclick="loadJobChooserModalObserve(this, PluginProperties['${propkey}'].value, null, null, null, 'jobrefpicker${pjkey}', 'jobrefpicker${pjkey}_content');"
                      id="jobChooseBtn${pjkey}"
                      title="${message(code: "select.an.existing.job.to.use")}"
                      data-loading-text="Loading...">
                    <g:message code="choose.a.job..."/>

                </span>

                <g:render template="/common/modal"
                          model="${[modalid  : 'jobrefpicker' + pjkey,
                                    modalsize: 'modal-lg',
                                    title    : message(code: "choose.a.job..."),
                                    buttons  : []]
                          }"/>
            </div>
        </g:if>
        <g:if test="${hasJobOptionsSelector}">
            <div class="${valueColTypeSplit20}">
                %{-- selector for job options --}%

                %{-- TODO: select options for a job --}%
                <g:set var="pjkey" value="${g.rkey()}"/>

                <span class="btn  btn-default act_choose_job"
                      onclick="loadJobChooserModalObserve(this, PluginProperties['${propkey}'].value, null, null, null, 'jobrefpicker${pjkey}', 'jobrefpicker${pjkey}_content');"
                      id="jobChooseBtn${pjkey}"
                      title="${message(code: "select.an.existing.job.to.use")}"
                      data-loading-text="Loading...">
                    <g:message code="choose.a.job..."/>

                </span>

                <g:render template="/common/modal"
                          model="${[modalid  : 'jobrefpicker' + pjkey,
                                    modalsize: 'modal-lg',
                                    title    : message(code: "choose.a.job..."),
                                    buttons  : []]
                          }"/>
            </div>
        </g:if>
        </div>
    </g:else>
<div class="${outofscope?valueColType:offsetColType}">
    <div class="help-block"> <g:render template="/scheduledExecution/description"
                                       model="[description: stepplugin.messageText(
                                               service: service,
                                               name: provider,
                                               code: 'property.' + prop.name + '.description',
                                               default: prop.description
                                       ), textCss         : '',
                                               mode       : 'collapsed', rkey: g.rkey()]"/></div>
    <g:if test="${error}">
        <div class="text-warning"><g:enc>${error}</g:enc></div>
    </g:if>
    <g:if test="${outofscope}">
        <g:render template="/framework/pluginConfigPropertyScopeInfo" model="[prefix:prefix,specialConfiguration:specialConfiguration,propScope:propScope,mapping:mapping, frameworkMapping: frameworkMapping, hideMissingFrameworkMapping: hideMissingFrameworkMapping]"/>
    </g:if>
</div>
</div>
</g:unless>
