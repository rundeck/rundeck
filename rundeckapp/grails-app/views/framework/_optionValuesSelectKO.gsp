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

<%@ page import="java.util.regex.Pattern" %>
<%--
    _optionValuesSelect.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: May 7, 2010 2:42:23 PM
    $Id$
 --%>

<div class="row" data-bind="if: option">
    <%-- Print out the input box for random input --%>
    <div data-bind="if: hasTextfield">
        <div data-bind="css: {'col-sm-8': hasExtended(), 'col-sm-12': !hasExtended() }">
            <div data-bind="if: secureInput()" class="input-group">
                <g:passwordField name="-"
                                 data-bind="value: value, attr: {name: fieldName, id: fieldId}"
                                 class="optionvaluesfield  form-control"
                                 value=""
                                 autocomplete="new-password"
                                 size="40"/>
                <!-- ko if: defaultStoragePath -->
                    <span class=" has_tooltip input-group-addon"
                        data-bind="bootstrapTooltip: true"
                          data-placement="bottom"
                          data-container="body"
                          title="${message(code: "form.option.defaultStoragePath.present.description")}">
                        <g:icon name="lock"/>
                    </span>
                <!-- /ko -->
            </div>

            <div data-bind="if: !secureInput()">
                <div data-bind="if: isDate()">

                    <div class='input-group date' data-bind="datetimepicker: value, dateFormat: dateFormat, css: {'has-error': dateFormatErr}">
                        <span class="input-group-addon has_tooltip">
                            <span class="glyphicon glyphicon-calendar"></span>
                        </span>
                        <input type="datetime" name="-"
                               data-bind="value:value, attr: {name: fieldName, id: fieldId,title:  dateFormat},bootstrapTooltip: dateFormat"
                               class="optionvaluesfield form-control"
                               value=""
                               size="40"
                        />
                    </div>
                </div>
                <div data-bind="if: !isDate()">
                    <g:textField name="-"
                                 data-bind="value: value, attr: {name: fieldName, id: fieldId}"
                                 class="optionvaluesfield form-control"
                                 value=""
                                 size="40"/>
                </div>
            </div>
        </div>
    </div>

    <div data-bind="if: !hasTextfield() && enforced() && remoteError()">
        <input type="hidden" data-bind="value: value, attr: {name: fieldName}" value=""/>
    </div>
    <%-- The Dropdown list --%>
    <div data-bind="if: hasExtended()">

        <div data-bind="css: {'col-sm-4': hasTextfield(), 'col-sm-12': !hasTextfield() }">

            <div data-bind="if: hasSingleEnforcedValue()">
                <g:hiddenField
                        data-bind="attr: {name: fieldName}, value: singleEnforcedValue"
                        name="-" value=""/>
                <p class="form-control-static"><span class="singlelabel" data-bind="text: singleEnforcedValue()"></span>
                </p>
            </div>

            <div data-bind="if:!hasSingleEnforcedValue()">

                <div data-bind="if: multivalued">
                    <!-- use checkboxes -->
                    <div class="optionmultiarea " data-bind="visible: !enforced() || multiValueList().length>0">

                        <div data-bind="if: !enforced()">
                            <%-- variable input text fields --%>
                            <div class="container">
                                <div class="row">
                                    <div class="col-sm-12 optionvaluemulti-add">
                                        <span class="btn btn-default btn-xs " data-bind="click: newMultivalueEntry">
                                            <g:message code="job.option.multivalue.new.value.button.title" /> <i class="glyphicon glyphicon-plus"></i>
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="grid"
                              data-bind="foreach: {data: multiValueList(), beforeRemove: animateRemove, afterAdd: animateAdd }">
                            <div class="optionvaluemulti ">
                                <label class="grid-row optionvaluemulti">
                                    <span class="grid-cell grid-front">
                                        <input type="checkbox"
                                               data-bind="attr: { name: $parent.fieldName }, value: value, checked: selected"
                                               value=""/>
                                    </span>
                                    <span class="grid-cell grid-rest">
                                        <!-- ko if: !editable() -->
                                        <span data-bind="text: label"></span>
                                        <!-- /ko -->

                                        <!-- ko if: editable() -->
                                        <input type="text" data-bind="value: value, event: { keypress: option.multivalueFieldKeydown }"
                                               class="form-control "/>
                                        <!-- /ko -->
                                    </span>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>

                <div data-bind="if:!multivalued()">
                    <select class="optionvalues form-control"
                            data-bind="attr: {name: !hasTextfield()?fieldName():'', id: !hasTextfield()?fieldId():'' },
                         options: selectOptions,
                         optionsText: 'label',
                         optionsValue: 'value',
                        value:selectedOptionValue">
                    </select>

                </div>

            </div>
        </div>
    </div>

    <div data-bind="if: showDefaultButton()">
        <div class="col-sm-12">
            <span class="btn btn-sm btn-link"
                  data-bind="attr: { title: message('option.default.button.title') }, click: setDefault"
                  title="Click to use default value: xx">
                <g:icon name="hand-right"/>
                <span data-bind="messageTemplate: truncateDefaultValue"><g:message
                        code="option.default.button.text"/></span>
            </span>
        </div>
    </div>

</div>

<div data-bind="if: hasRemote() && remoteValues().length<1 && !remoteError()">
    <div class="row">
        <div class="col-sm-12">
            <div class="text-muted"><g:message code="option.remote.dependency.emptyresult"/></div>
        </div>
    </div>
</div>

<div data-bind="if: remoteError()">
    <div class="row">
        <div class="col-sm-12">
            <div data-bind="if: remoteError().code == 'empty'">
                <div class="text-muted"><g:message code="option.remote.dependency.emptyresult"/></div>
            </div>
            <span class="text-danger " data-bind="text: remoteError().error"></span>

            <div data-bind="if: remoteError().exception || remoteError().url">
                <a class="textbtn textbtn-warning "
                   data-toggle="collapse"
                   href="#"
                   data-bind="attr: {href: '#rerr_'+option.uid() }">
                   <span data-bind="text: remoteError().message"></span>
                    <i class="auto-caret"></i>
                </a>
            </div>
            <div data-bind="if: remoteError().message && !(remoteError().exception || remoteError().url)">
                <span class="text-warning" data-bind="text: remoteError().message"></span>
            </div>

            <div class="alert alert-warning collapse collapse-expandable" data-bind="attr: {id: 'rerr_'+option.uid() }">
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
