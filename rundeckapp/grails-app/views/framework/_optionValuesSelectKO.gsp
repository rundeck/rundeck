<%@ page import="java.util.regex.Pattern" %>
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
    _optionValuesSelect.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: May 7, 2010 2:42:23 PM
    $Id$
 --%>

<g:set var="rkey" value="${rkey ?: g.rkey()}"/>
<g:set var="fkey" value="${rkey}"/>
<div class="row" data-bind="if: option">
    <g:set var="textcolsize" value="${hasExtended ? '8' : '12'}"/>
    <g:set var="extcolsize" value="${hasTextfield ? '4' : '12'}"/>
    <%-- Print out the input box for random input --%>
    <div data-bind="if: hasTextfield">
        <div data-bind="css: {'col-sm-8': hasExtended(), 'col-sm-12': !hasExtended() }">
            <div data-bind="if: secureInput()">
                <g:passwordField name="-"
                                 data-bind="value: value, attr: {name: fieldName}"
                                 class="optionvaluesfield  form-control"
                                 value=""
                                 size="40"/>
                <div data-bind="if: defaultStoragePath">
                    <span class="input-group-addon has_tooltip"
                          data-placement="left"
                          title="${message(code: "form.option.defaultStoragePath.present.description")}">
                        <g:icon name="lock"/>
                    </span>
                </div>
            </div>

            <div data-bind="if: !secureInput()">
                <g:textField name="-"
                             data-bind="value: value, attr: {name: fieldName}"
                             class="optionvaluesfield form-control"
                             value=""
                             size="40"/>
            </div>
        </div>
    </div>

    <div data-bind="if: !hasTextfield() && enforced() && hasError()">
        <div data-bind="css: {'col-sm-8': hasExtended(), 'col-sm-12': !hasExtended() }">
            <span class="info note"><g:message code="Execution.option.enforced.values.could.not.be.loaded"/></span>
            <input type="hidden"
                   data-bind="value: value, attr: {name: fieldName}"
                   value=""/>
        </div>
    </div>
    <%-- The Dropdown list --%>
    <div data-bind="if: hasExtended()">

        <div data-bind="css: {'col-sm-4': hasTextfield(), 'col-sm-12': !hasTextfield() }">

            %{--<g:set var="labelsSet" value="${values && values instanceof Map ? values.keySet() :--}%
            %{--values ? values : optionSelect.values ? optionSelect.values : []}"/>--}%
            %{--<g:set var="valuesMap" value="${values && values instanceof Map ? values : null}"/>--}%
            %{-- set of all of the values that will be pre-shown in the multivalue list --}%
            %{--<g:set var="labelsSetValues" value="${labelsSet?.collect {--}%
            %{--it instanceof Map ? it.value : it--}%
            %{--}}"/>--}%

            <div data-bind="if: hasSingleEnforcedValue()">
                %{--<g:set var="selentry" value="${labelsSet.iterator().next()}"/>--}%
                %{--<g:if test="${selentry instanceof Map}">--}%
                %{--<g:set var="sellabel" value="${selentry.name}"/>--}%
                %{--<g:set var="selvalue" value="${selentry.value}"/>--}%
                %{--</g:if>--}%
                %{--<g:else>--}%
                %{--<g:set var="sellabel" value="${selentry}"/>--}%
                %{--<g:set var="selvalue" value="${valuesMap?valuesMap[sellabel]:sellabel}"/>--}%
                %{--</g:else>--}%
                <g:hiddenField
                        data-bind="attr: {name: fieldName}, value: singleEnforcedValue"
                        name="-" value=""/>
                <p class="form-control-static"><span class="singlelabel" data-bind="text: singleEnforcedValue()"></span>
                </p>
            </div>

            <div data-bind="if:!hasSingleEnforcedValue()">

                %{--<g:if test="${optionSelect.multivalued}">--}%
                %{--<!-- use checkboxes -->--}%
                %{--<g:set var="defaultMultiValues" value="${optionSelect.listDefaultMultiValues()}"/>--}%
                %{--<div class="optionmultiarea " id="${enc(attr: fieldwatchid)}">--}%
                %{--<g:if test="${selectedoptsmap && selectedoptsmap[optName] &&--}%
                %{--selectedoptsmap[optName] instanceof String}">--}%
                %{--%{--}%
                %{--selectedoptsmap[optName] = selectedoptsmap[optName].split(--}%
                %{--Pattern.quote(optionSelect.delimiter)--}%
                %{--) as List--}%
                %{--}%--}%
                %{--</g:if>--}%
                %{--<g:if test="${!optionSelect.enforced}">--}%
                %{--<%-- variable input text fields --%>--}%
                %{--<div class="container">--}%
                %{--<div class="row">--}%
                %{--<div class="col-sm-12 optionvaluemulti-add">--}%
                %{--<span class="btn btn-default btn-xs obs_addvar">--}%
                %{--New Value <i class="glyphicon glyphicon-plus"></i>--}%
                %{--</span>--}%
                %{--</div>--}%
                %{--</div>--}%
                %{--</div>--}%

                %{--<div class="">--}%
                %{--<div id="${enc(attr: rkey)}varinput" class="">--}%

                %{--</div>--}%
                %{--</div>--}%
                %{----}%
                %{--Determine any new values (via selectedoptsmap) that should be added--}%
                %{--to the multivalue list, and preselected--}%
                %{----}%
                %{--<g:set var="newvals"--}%
                %{--value="${selectedoptsmap ? labelsSetValues ? selectedoptsmap[optName].findAll {--}%
                %{--!labelsSetValues.contains(it)--}%
                %{--} : selectedoptsmap[optName] : null}"/>--}%
                %{--<g:if test="${newvals}">--}%
                %{--<g:javascript>--}%
                %{--fireWhenReady('${enc(js: rkey)}varinput', function(){--}%
                %{--<g:each in="${newvals}" var="nvalue">--}%
                %{--ExecutionOptions.addMultivarValue('${enc(js: optName)}','${enc(--}%
                %{--js: rkey--}%
                %{--)}varinput','${enc(js: nvalue)}',null,'bottom');--}%
                %{--</g:each>--}%
                %{--}--}%
                %{--);--}%
                %{--</g:javascript>--}%
                %{--</g:if>--}%
                %{--<g:if test="${!labelsSet && !newvals}">--}%
                %{--<g:javascript>--}%
                %{--fireWhenReady('${enc(--}%
                %{--js: rkey--}%
                %{--)}varinput', function(){ ExecutionOptions.addMultivarValue('${enc(js: optName)}','${enc(--}%
                %{--js: rkey--}%
                %{--)}varinput'); } );--}%
                %{--</g:javascript>--}%
                %{--</g:if>--}%
                %{--</g:if>--}%
                %{--<g:each in="${labelsSet}" var="sellabel">--}%
                %{--<g:set var="entry"--}%
                %{--value="${sellabel instanceof Map ? sellabel : [name: sellabel, value: sellabel]}"/>--}%
                %{--<div class="">--}%
                %{--<div class="">--}%
                %{--<div class="optionvaluemulti ">--}%
                %{--<label>--}%
                %{--<g:set var="ischecked" value="${selectedvalue && entry.value ==--}%
                %{--selectedvalue ||--}%
                %{--(defaultMultiValues ? entry.value in defaultMultiValues :--}%
                %{--entry.value == optionSelect.defaultValue) ||--}%
                %{--selectedoptsmap &&--}%
                %{--entry.value in--}%
                %{--selectedoptsmap[optName]}"/>--}%
                %{--<input type="checkbox" name="${enc(attr: realFieldName)}"--}%
                %{--value="${enc(attr: entry.value)}" ${ischecked ? 'checked' : ''}/>--}%
                %{--<g:enc>${entry.name}</g:enc>--}%
                %{--</label>--}%
                %{--</div>--}%
                %{--</div>--}%
                %{--</div>--}%

                %{--</g:each>--}%
                %{--</div>--}%
                %{--<g:javascript>--}%
                %{--fireWhenReady('${enc(js: fieldwatchid)}', function(){--}%
                %{--$$('#${enc(js: fieldwatchid)} input[type="checkbox"]').each(function(e){--}%
                %{--Event.observe(e,'change',ExecutionOptions.multiVarCheckboxChangeWarningHandler.curry('${--}%
                %{--enc(js: optName)}'));--}%
                %{--});--}%
                %{--$$('#${enc(js: fieldwatchid)} .obs_addvar').each(function(e){--}%
                %{--Event.observe(e,'click', function(evt){--}%
                %{--var roc=_remoteOptionControl('_commandOptions');--}%
                %{--ExecutionOptions.addMultivarValue('${enc(js: optName)}','${enc(js: rkey)}varinput',null,roc.observeMultiCheckbox.bind(roc));--}%
                %{--});--}%
                %{--});--}%
                %{--}--}%
                %{--);--}%
                %{--</g:javascript>--}%
                %{--</g:if>--}%
                <div data-bind="if:!multivalued()">
                    <select class="optionvalues form-control"
                            data-bind="attr: {name: !hasTextfield()?fieldName():'' },
                         options: selectOptions,
                         optionsText: 'label',
                        value:selectedOptionValue">
                    </select>

                </div>

            </div>
        </div>
    </div>

    <div data-bind="if: showDefaultButton()">
        <span class="textbtn textbtn-default"
              data-bind="attr: { title: defaultValue() }, click: setDefault"
              title="Click to use default value: xx">
            default: <span data-bind="text: truncateDefaultValue()"></span>
        </span>
    </div>

</div>

<div data-bind="if: remoteError()">
    <div class="row">
        <div class="col-sm-12">
            <div data-bind="if: remoteError().code == 'empty'">
                %{--TODO: wrap field contents--}%
                <div class="info note emptyMessage">No values to choose from</div>
            </div>
            <span key="${rkey}_error_detail"
                        class="text-warning _error_detail" data-bind="text: remoteError().message">

            </span>
%{--TODO expander--}%
            <div class="alert alert-warning _error_detail" style="" id="${enc(attr: rkey)}_error_detail">
                <span data-bind="if: remoteError().exception">
                    <div>Exception: <span data-bind="text: remoteError().exception"></span></div>
                </span>
                <span data-bind="if: remoteError().url">
                    <div>URL: <span data-bind="text: remoteError().url"></span></div>
                </span>
            </div>
        </div>
    </div>
</div>
