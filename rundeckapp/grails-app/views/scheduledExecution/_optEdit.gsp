<%@ page import="com.dtolabs.rundeck.core.dispatcher.DataContextUtils" %>
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
   _optEdit.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Aug 2, 2010 4:42:44 PM
   $Id$
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<div class="popout optEditForm" style="background:white">
    <g:hasErrors bean="${option}">
        <div class="error message">
            <g:renderErrors bean="${option}" as="list"/>
        </div>
    </g:hasErrors>
    <g:render template="/common/messages"/>
    <div id="optedit_${rkey}">

    <%-- Option edit form fields --%>
        <g:if test="${newoption}">
            <div>
                <span class="prompt major">Add New Option</span>
            </div>
        </g:if>

        <div class="presentation">
            <label for="optname_${rkey}" class="  left  ${hasErrors(bean:option,field:'name','fieldError')}" style="width:22em;">Option Name</label>
            <label for="optdesc_${rkey}" class=" right">Description</label>
            <div class="clear"></div>
            <g:textField name="name" class="left half restrictOptName" value="${option?.name}" size="40" placeholder="Option Name" id="optname_${rkey}"/>
            <g:textField name="description"  class="right" value="${option?.description}" size="40" placeholder="Option description" id="optdesc_${rkey}"/>

            <label>
                <div class="${hasErrors(bean:option,field:'defaultValue','fieldError')}">Default Value</div>
                <input type="text" class="right" name="defaultValue" value="${option?.defaultValue}" size="40" placeholder="Default value"/>
            </label>

            <g:if test="${origName || option?.name && !newoption}">
                <g:hiddenField name="origName" value="${origName?origName:option?.name}"/>
            </g:if>
        </div>
        <div>
            <span class="prompt">Allowed Values</span>
            <div class="presentation inputset">
                <div>
                    <label class="left ${hasErrors(bean:option,field:'values','fieldError')}"><g:radio name="valuesType" value="list" checked="${!option || !option.valuesUrl && params.valuesType!='url' ?true:false}" id="vtrlist_${rkey}"/> List:</label>
                    <g:set var="listvalue" value="${option?.valuesList}"/>
                    <g:set var="listjoin" value="${option?.values }"/>
                    <g:textField name="valuesList" class="right" value="${listvalue? listvalue.encodeAsHTML() : listjoin ? listjoin.join(',')?.encodeAsHTML():''}" size="60" placeholder="Comma separated list" id="vlist_${rkey}"/>
                    <wdgt:eventHandler for="vlist_${rkey}" state="unempty" target="vtrlist_${rkey}" check="true" inline="true"  action="keydown"/>
                    <wdgt:eventHandler for="vtrlist_${rkey}" state="unempty" target="vlist_${rkey}" focus="true" inline="true"/>
                </div>
                <div>
                    <label class="left ${hasErrors(bean:option,field:'valuesUrl','fieldError')}"><g:radio name="valuesType" value="url" checked="${option?.valuesUrl || option?.valuesUrlString || params.valuesType=='url'?true:false}"  id="vtrurl_${rkey}"/> Remote URL:</label>
                    <input type="url" class="right" name="valuesUrl" value="${option?.valuesUrlString? option.valuesUrlString : option?.valuesUrl }" size="60" placeholder="Remote URL" id="vurl_${rkey}"/>
                    <div class="info note right">A URL to a Remote JSON service. See <a href="http://dtolabs.com/wiki/Job_Remote_option_values" target="_blank">wiki:Remote option values</a></div>
                    <wdgt:eventHandler for="vurl_${rkey}" state="unempty" target="vtrurl_${rkey}" check="true" inline="true" action="keydown"/>
                    <wdgt:eventHandler for="vtrurl_${rkey}" state="unempty" target="vurl_${rkey}" focus="true" inline="true"/>
                </div>
            </div>
        </div>
        <div>
            <span class="prompt">Restrictions</span>
            <div class="presentation">
                <div>
                    <label><g:radio name="enforcedType" value="none" checked="${!option || !option?.enforced && null==option?.regex}"/> None</label>
                    <span class="info note">Any values can be used</span>
                </div>
                <div>
                    <label class="${hasErrors(bean:option,field:'enforced','fieldError')}"><g:radio name="enforcedType" value="enforced" checked="${option?.enforced?true:false}"/> Enforced from Allowed Values</label>
                </div>
                <div>
                    <label class="${hasErrors(bean:option,field:'regex','fieldError')}"><g:radio name="enforcedType" value="regex" checked="${option?.regex?true:false}" id="etregex_${rkey}"/>
                    Match Regular Expression:</label>
                    <input type="text" name="regex" value="${option?.regex}" size="40" placeholder="Enter a Regular Expression" id="vregex_${rkey}"/>
                    <wdgt:eventHandler for="vregex_${rkey}" state="unempty" target="etregex_${rkey}" check="true" inline="true" action="keydown"/>
                    <wdgt:eventHandler for="etregex_${rkey}" state="unempty" target="vregex_${rkey}" focus="true" inline="true"/>
                </div>
                <g:if test="${regexError}">
                    <pre class="error note">${regexError.trim()}</pre>
                </g:if>
            </div>
        </div>
        <div>
            <span class="prompt">Requirement</span>
            <div class="presentation">
                <div>
                    <span class="info note">Require this option to be specified when running the Job</span>
                </div>
                <div>
                    <label><g:radio name="required" value="false" checked="${!option || !option.required}"/> No</label>
                    <label><g:radio name="required" value="true" checked="${option?.required}"/> Yes</label>
                </div>
            </div>
        </div>
        <div>
            <span class="prompt">Multi-valued</span>
            <div class="presentation">
                <div>
                    <span class="info note"><g:message code="form.option.multivalued.description"/></span>
                </div>
                <div>
                    <label><g:radio name="multivalued" value="false" checked="${!option || !option.multivalued}"/> No</label>
                    <label><g:radio name="multivalued" value="true" checked="${option?.multivalued}" id="cdelimiter_${rkey}"/>
                    Yes with delimiter
                    </label>
                    <input type="text" name="delimiter" value="${option?.delimiter}" size="5" placeholder="Value delimiter string" id="vdelimiter_${rkey}"/>
                    <wdgt:eventHandler for="vdelimiter_${rkey}" state="unempty" target="cdelimiter_${rkey}" check="true" inline="true" action="keydown"/>
                    <wdgt:eventHandler for="cdelimiter_${rkey}" state="unempty" target="vdelimiter_${rkey}" focus="true" inline="true"/>
                </div>
                <div>
                    <span class="info note"><g:message code="form.option.delimiter.description"/></span>
                </div>
            </div>
        </div>
        <div id="preview_${rkey}" style="${wdgt.styleVisible(if:option?.name)}">
            <span class="prompt">Usage</span>
            <div class="presentation">
                <span class="info note">The option values will be available to scripts in these forms:</span>
                <div>
                    Bash: <code>$<span id="bashpreview${rkey}">${option?.name?DataContextUtils.generateEnvVarName('option.'+option.name):''}</span></code>
                </div>
                <div>
                    Commandline Arguments: <code>$<!-- -->{option.<span id="clipreview${rkey}">${option?.name?.encodeAsHTML()}</span>}</code>
                </div>
                <div>
                    Script Content: <code>@option.<span id="scptpreview${rkey}">${option?.name?.encodeAsHTML()}</span>@</code>
                </div>
            </div>
        </div>
        <g:javascript>
        function _tobashvar(str){
            return "${DataContextUtils.ENV_VAR_PREFIX}OPTION_"+str.toUpperCase().replace(/[.]/g,'_').replace(/[{}$]/,'');
        }
        <wdgt:eventHandler for="optname_${rkey}" state="unempty" inline="true" jsonly="true" action="keyup">
            <wdgt:action target="preview_${rkey}" visible="true" test="true"/>
            <wdgt:action target="bashpreview${rkey}" copy="tohtml" test="true" transformfuncname="_tobashvar"/>
            <wdgt:action target="clipreview${rkey}" copy="tohtml" test="true"/>
            <wdgt:action target="scptpreview${rkey}" copy="tohtml" test="true"/>
        </wdgt:eventHandler>
        <wdgt:eventHandler for="optname_${rkey}" state="empty" inline="true"  jsonly="true">
            <wdgt:action target="preview_${rkey}" visible="false"/>
        </wdgt:eventHandler>
        </g:javascript>

        <g:hiddenField name="scheduledExecutionId" value="${scheduledExecutionId}"/>
        <div class="floatr" style="margin:10px 0;">
            <g:if test="${newoption}">
                <g:hiddenField name="newoption" value="true"/>
                <span class="action button small textbtn" onclick="_optcancelnew('${option?.name}');" title="Cancel adding new option">Cancel</span>
                <span class="action button small textbtn" onclick="_optsavenew('optedit_${rkey}');" title="Save the new option">Save</span>
                <g:javascript>
                    fireWhenReady('optname_${rkey}',function(){
                        $('optname_${rkey}').focus();
                    });
                </g:javascript>
            </g:if>
            <g:else>
                <span class="action button small textbtn" onclick="_optview('${(origName?origName:option?.name).encodeAsJavaScript()}',$(this).up('li.optEntry'));" title="Discard changes to the option">Discard</span>
                <span class="action button small textbtn" onclick="_optsave('optedit_${rkey}',$(this).up('li.optEntry'));" title="Save changes to the option">Save</span>
            </g:else>
        </div>
        <div class="clear"></div>
    </div>
</div>