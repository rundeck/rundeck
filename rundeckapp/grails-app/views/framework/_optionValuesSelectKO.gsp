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

<div class="row" data-bind="if: option">
    <%-- Print out the input box for random input --%>
    <div data-bind="if: hasTextfield">
        <div data-bind="css: {'col-sm-8': hasExtended(), 'col-sm-12': !hasExtended() }">
            <div data-bind="if: secureInput()">
                <g:passwordField name="-"
                                 data-bind="value: value, attr: {name: fieldName}"
                                 class="optionvaluesfield  form-control"
                                 value=""
                                 autocomplete="new-password"
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
                    <div class="optionmultiarea ">

                        <div data-bind="if: !enforced()">
                            <%-- variable input text fields --%>
                            <div class="container">
                                <div class="row">
                                    <div class="col-sm-12 optionvaluemulti-add">
                                        <span class="btn btn-default btn-xs " data-bind="click: newMultivalueEntry">
                                            New Value <i class="glyphicon glyphicon-plus"></i>
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div data-bind="foreach: multiValueList(), beforeRemove: animateRemove, beforeAdd: animateAdd">
                            <div class="optionvaluemulti form-inline">
                                <label>
                                    <input type="checkbox"
                                           data-bind="attr: { name: $parent.fieldName }, value: value, checked: selected"
                                           value=""/>

                                    <span data-bind="if: !editable()">
                                        <span data-bind="text: label"></span>
                                    </span>
                                    <span data-bind="if: editable()">
                                        <input data-bind="value: value" class="form-control"/>
                                    </span>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>

                <div data-bind="if:!multivalued()">
                    <select class="optionvalues form-control"
                            data-bind="attr: {name: !hasTextfield()?fieldName():'' },
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
            <span class="text-warning _error_detail" data-bind="text: remoteError().message">

            </span>
            %{--TODO expander--}%
            <div class="alert alert-warning _error_detail" data-bind="visible: remoteError().exception || remoteError().url">
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
