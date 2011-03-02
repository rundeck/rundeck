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

<g:set var="rkey" value="${g.rkey()}"/>
<g:set var="realFieldName" value="${(fieldPrefix?fieldPrefix:'')+(fieldName?fieldName:'option.'+optionSelect.name)}"/>
<g:if test="${optionSelect}">
    <g:set var="optName" value="${optionSelect.name}"/>
    
    <%-- Print out the input box for random input --%>
    <g:if test="${!optionSelect.enforced && !optionSelect.multivalued || err}">
        <g:textField name="${realFieldName}"
            class="optionvaluesfield"
            value="${selectedvalue?selectedvalue:selectedoptsmap && selectedoptsmap[optName]?selectedoptsmap[optName]:optionSelect.defaultValue?optionSelect.defaultValue:''}"
            maxlength="256" size="40"
            id="${rkey}"/>
            <%-- event handler: when text field is empty, show required option value warning icon if it exists--%>
            <wdgt:eventHandler for="${rkey}" state="empty" visible="true" targetSelector="${'#'+optName.encodeAsHTML()+'_state span.reqwarning'}" frequency="1"  inline='true'/>
    </g:if>

    <%-- The Dropdown list --%>
    <g:if test="${(values || optionSelect.values) && !err}">
        
    
        <g:set var="labelsSet" value="${values && values instanceof Map?values.keySet():values?values:optionSelect.values?optionSelect.values:[]}"/>
        <g:set var="valuesMap" value="${values && values instanceof Map?values:null}"/>

        <g:if test="${labelsSet && 1==labelsSet.size() && optionSelect.enforced}">
            <g:set var="selentry" value="${labelsSet.iterator().next()}"/>
            <g:if test="${selentry instanceof Map}">
                <g:set var="sellabel" value="${selentry.name}"/>
                <g:set var="selvalue" value="${selentry.value}"/>
            </g:if>
            <g:else>
                <g:set var="sellabel" value="${selentry}"/>
                <g:set var="selvalue" value="${valuesMap?valuesMap[sellabel]:sellabel}"/>
            </g:else>
            <g:hiddenField name="${realFieldName}" value="${selvalue.encodeAsHTML()}"/>
            <span class="singlelabel">${sellabel.encodeAsHTML()}</span>
        </g:if>
        <g:else>

            <g:if test="${optionSelect.multivalued}">
                <!-- use checkboxes -->
                <div class="optionmultiarea" id="${rkey}multiarea">
                    <g:if test="${!optionSelect.enforced}">
                        <%-- variable input text fields --%>
                        <div class="optionvaluemulti ">
                            <span class="action button obs_addvar" style="margin-left:20px" onclick="ExecutionOptions.addMultivarValue('${optName.encodeAsJavaScript()}','${rkey}varinput');">
                                New Value&hellip;
                            </span>
                        </div>
                        <div id="${rkey}varinput">

                        </div>
                        <g:set var="newvals" value="${selectedoptsmap?selectedoptsmap[optName].findAll {optionSelect.values && !optionSelect.values.contains(it)}:null}"/>
                        <g:if test="${newvals}">
                            <g:javascript>
                                fireWhenReady('${rkey}varinput', function(){
                                <g:each in="${newvals}" var="nvalue">
                                    ExecutionOptions.addMultivarValue('${optName.encodeAsJavaScript()}','${rkey}varinput','${nvalue.encodeAsJavaScript()}');
                                </g:each>
                                }
                                );
                            </g:javascript>
                        </g:if>
                    </g:if>
                    <g:each in="${labelsSet}" var="sellabel">
                        <g:set var="entry" value="${sellabel instanceof Map?sellabel:[name:sellabel,value:sellabel]}"/>
                        <div class="optionvaluemulti">
                            <label>
                                <input type="checkbox" name="${realFieldName.encodeAsHTML()}" value="${entry.value.encodeAsHTML()}" ${selectedvalue && entry.value == selectedvalue || entry.value == optionSelect.defaultValue || selectedoptsmap && entry.value in selectedoptsmap[optName] ? 'checked' : ''} /> ${entry.name.encodeAsHTML()}
                            </label>
                        </div>

                    </g:each>
                </div>
                <g:javascript>
                    fireWhenReady('${rkey}multiarea',
                        function(){$$('#${rkey}multiarea input[type="checkbox"]').each(function(e){
                            Event.observe(e,'change',ExecutionOptions.multiVarCheckboxChangeWarningHandler.curry('${optName.encodeAsHTML()}'));
                        });}
                    );
                </g:javascript>
            </g:if>
            <g:else>
                <select class="optionvalues" id="${rkey}_sel" ${optionSelect.enforced ? 'name="' + realFieldName.encodeAsHTML() + '"' : ''}>
                    <g:if test="${!optionSelect.enforced && !optionSelect.multivalued}">
                        <option value="">-choose-</option>
                    </g:if>

                    <g:each in="${labelsSet}" var="sellabel">
                        <g:set var="entry" value="${sellabel instanceof Map?sellabel:[name:sellabel,value:sellabel]}"/>
                        <option value="${entry.value.encodeAsHTML()}" ${selectedvalue && entry.value == selectedvalue || entry.value == optionSelect.defaultValue || selectedoptsmap && entry.value == selectedoptsmap[optName] ? 'selected' : ''}>${entry.name.encodeAsHTML()}</option>
                    </g:each>
                </select>
                <g:if test="${!optionSelect.enforced || err}">
                <%-- event handler: when select popup value is changed, copy the value to the textfield --%>
                    <wdgt:eventHandler for="${rkey}_sel" notequals="" copy="value" target="${rkey}" inline='true' multivaluedelimiter="${optionSelect.multivalued?optionSelect.delimiter:null}"/>
                </g:if>
            </g:else>

        </g:else>
        <g:if test="${optionSelect.enforced}">
            <g:javascript>
            fireWhenReady('${optName.encodeAsJavaScript()}_state',
            function(){ $$('${'#' + optName.encodeAsJavaScript()+'_state span.reqwarning'}').each(function(e){$(e).hide();}); }
            );

            </g:javascript>
        </g:if>
    </g:if>
</g:if>
<g:if test="${err}">
    <g:expander key="${rkey}_error_detail" classnames="error label">${err.message.encodeAsHTML()}</g:expander>
    
    <span class="error note" style="display:none" id="${rkey}_error_detail">
        <g:if test="${err.exception}">
            <div>Exception: ${err.exception.message.encodeAsHTML()}</div>
        </g:if>
        <g:if test="${srcUrl}">
            <div>URL: ${srcUrl.encodeAsHTML()}</div>
        </g:if>
    </span>
</g:if>
<g:elseif test="${values}">
    <g:img file="icon-tiny-ok.png" title="Remote option values loaded from URL: ${srcUrl.encodeAsHTML()}"/>
</g:elseif>