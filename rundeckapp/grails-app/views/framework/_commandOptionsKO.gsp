<%@ page import="java.util.regex.Pattern; grails.converters.deep.JSON; grails.util.Environment" %>
<%--
used by _editOptions.gsp template
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<g:if test="${optionSelections}">
    <g:set var="usePrefix" value="${paramsPrefix ?: ''}"/>
    <g:set var="showDTFormat" value="${false}"/>
    <g:hiddenField name="${usePrefix + 'argString'}" value=""/>
    <%
        def optsmap = [:]
        if (optionSelections) {
            optionSelections.each {
                optsmap[it.name] = [selopt: it]
            }
        }
    %>
    <g:embedJSON data="${[options:
                                  (optionordering ?: optsmap.keySet().sort()).collect {
                                      def optionSelect = optsmap[it].selopt
                                      def optName = optionSelect.name
                                      return [
                                              name               : optName,
                                              required           : optionSelect.required,
                                              description        : optionSelect.description,
                                              descriptionHtml    : optionSelect.description?.decodeMarkdown(),
                                              enforced           : optionSelect.enforced,
                                              values             : optionSelect.values,
                                              defaultValue       : optionSelect.defaultValue,
                                              defaultStoragePath : optionSelect.defaultStoragePath,
                                              multivalued        : optionSelect.multivalued,
                                              defaultMultiValues : optionSelect.listDefaultMultiValues(),
                                              delimiter          : optionSelect.delimiter,
                                              selectedMultiValues: selectedoptsmap && selectedoptsmap[optName] && optionSelect.multivalued
                                                      && optionSelect.delimiter ? selectedoptsmap[optName]?.split(
                                                      java.util.regex.Pattern.quote(optionSelect.delimiter)
                                              ) as List : null,
                                              fieldName          : usePrefix + 'option.' + optName,
                                              hasValue           : !selectedoptsmap && optionSelect.defaultValue ||
                                                      selectedoptsmap &&
                                                      selectedoptsmap[optName] ||
                                                      !optionSelect.defaultValue &&
                                                      optionSelect.required
                                                      &&
                                                      optionSelect.enforced &&
                                                      optionSelect.values,
                                              hasError           : jobexecOptionErrors ? jobexecOptionErrors[optName] :
                                                      null,
                                              hasRemote          : optionSelect.realValuesUrl != null,
                                              optionDepsMet      : !optiondependencies[optName] || selectedoptsmap &&
                                                      optiondependencies[optName].every { selectedoptsmap[it] },
                                              secureInput        : optionSelect.secureInput,
                                              hasExtended        : !optionSelect.secureInput && (values || optionSelect.values ||
                                                      optionSelect.multivalued),
                                              value              : selectedvalue ? selectedvalue :
                                                      selectedoptsmap && null != selectedoptsmap[optName] ?
                                                              selectedoptsmap[optName] :
                                                              optionSelect.defaultValue ? optionSelect.defaultValue : ''
                                      ]
                                  }
    ]}" id="jobOptionData"/>
<%--
data for configuring remote option cascading/dependencies
--%>
    <g:embedJSON id="remoteOptionData"
                 data="${[options: remoteOptionData, optionsDependenciesCyclic: optionsDependenciesCyclic]}"/>

    <div id="_commandOptions" data-bind="foreach: {data: options(), as: 'option' }">
        <div class="form-group " data-bind="
    css: { 'has-warning': hasError, 'remote': hasRemote }
    ">
            <label class="remoteoptionfield col-sm-2 control-label"
                   data-bind="attr: { for: fieldId, id: fieldLabelId },click: reloadRemoteValues">
                <span data-bind="if: loading() && hasRemote()">
                    <g:img file="spinner-gray.gif" width="24px" height="24px"/>
                </span>
                <span class="remotestatus"
                      data-bind="if: hasRemote, css: {ok: !remoteError() && remoteValues, error: remoteError()}">
                </span>
                <span data-bind="text: name"></span>
            </label>

            <div class=" col-sm-9">

                <span class="info note" data-bind="if: !optionDepsMet && hasRemote()">
                    <g:message code="option.remote.dependency.emptyresult"/>
                </span>
                %{--<input type="hidden" data-bind="attr: { id: fieldId, name: fieldName }, value: value">--}%
                <g:render template="/framework/optionValuesSelectKO"/>

            </div>

            <div class="col-sm-1">
                <span data-bind="if: required">
                    <span class="reqwarning has_tooltip"
                          data-bind="attr: {title: hasError()||message('option.value.required') }, visible: !hasValue()"
                          data-toggle="tooltip">
                        <i class="glyphicon glyphicon-warning-sign"></i>
                    </span>
                </span>
            </div>

            <div class="col-sm-10 col-sm-offset-2">
                %{--<span class="help-block" data-bind="text: description"></span>--}%
                <span class="help-block" data-bind="html: descriptionHtml"></span>
            </div>

            <div class="col-sm-10 col-sm-offset-2" data-bind="if: hasError">
                <p class="text-warning" data-bind="text: hasError"></p>
            </div>
        </div>
    </div>

    <div id="_commandOptions" data-bind="foreach: {data: options(), as: 'option' }">
        <div><span data-bind="text: option.name"></span>=<span data-bind="text: option.value"></span></div>
    </div>
    <g:if test="${optionsDependenciesCyclic}">
        <g:message code="remote.options.warning.cyclicDependencies"/>
    </g:if>
    <g:if test="${showDTFormat}">
        <div class="info note help">

            <g:expander key="argStringDateFormatHelp">datestamp format</g:expander>

            <table id="argStringDateFormatHelp" style="display:none">
                <thead>
                <tr>
                    <th colspan="4">
                        <span style="color:green">$<!-- -->{DATE:<em>XXYYZZ</em>}
                        </span> can be used with these formatting characters:
                    </th>
                </tr>
                </thead>
                <tr>
                    <td>y</td>    <td>Year</td>
                    <td>M</td>    <td>Month in year</td>
                </tr>
                <tr>
                    <td>w</td>    <td>Week in year</td>
                    <td>W</td>    <td>Week in month</td>
                </tr>
                <tr>
                    <td>D</td>    <td>Day in year</td>
                    <td>d</td>    <td>Day in month</td>
                </tr>
                <tr>
                    <td>a</td>    <td>Am/pm marker</td>
                    <td>H</td>    <td>Hour in day (0-23)</td>
                </tr>
                <tr>
                    <td>k</td>    <td>Hour in day (1-24)</td>
                    <td>K</td>    <td>Hour in am/pm (0-11)</td>
                </tr>
                <tr>
                    <td>h</td>    <td>Hour in am/pm (1-12)</td>
                    <td>m</td>    <td>Minute in hour</td>
                </tr>
                <tr>
                    <td>s</td>    <td>Second in minute</td>
                    <td>S</td>    <td>Millisecond</td>
                </tr>
                <!--<tr><td>Z</td> 	<td>Time zone</td></tr>-->
            </table>
        </div>
    </g:if>

    </></g:if>
<g:elseif test="${!authorized}">
    <div class="info note">Not authorized to execute chosen job.</div>
    <g:if test="${selectedargstring}"><div>Old value: <g:enc>${selectedargstring}</g:enc></div></g:if>
</g:elseif>
<g:else>
    <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
            <g:message code="input.options"/>
        </div>

        <div class="col-sm-10">
            <p class="form-control-static text-muted"><g:message code="no.input.options.for.this.job"/></p>
        </div>
    </div>
</g:else>
