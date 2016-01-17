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
<div class="container optEditForm" >
    <g:hasErrors bean="${option}">
        <div class="alert alert-danger">
            <g:renderErrors bean="${option}" as="list"/>
        </div>
    </g:hasErrors>
    <g:render template="/common/messages"/>
    <div id="optedit_${rkey}">

    <%-- Option edit form fields --%>
        <g:if test="${newoption}">
            <div class="row">
            <div class="col-sm-12">
                <span class="h4"><g:message code="add.new.option" /></span>
            </div>
            </div>
        </g:if>

        <div class="form-group">

            <label for="optname_${rkey}" class="col-sm-2 control-label    ${hasErrors(bean:option,field:'name','has-error')}"><g:message code="form.option.name.label" /></label>
            <div class="col-sm-10">
                <g:textField
                        data-bind="value: name, valueUpdate: 'keyup'"
                        name="name"
                        class="form-control restrictOptName"
                        value="${option?.name}"
                        size="40"
                        placeholder="Option Name"
                        id="optname_${rkey}"
                />
            </div>
        </div>
        <div class="form-group ${hasErrors(bean: option, field: 'description', 'has-error')}">

            <label class="col-sm-2 control-label" for="optdesc_${rkey}" ><g:message code="form.option.description.label" /></label>
            <div class="col-sm-10">
                <g:textArea name="description"
                            value="${option?.description}"
                            rows="3"
                            placeholder="Description"
                            class="form-control ace_editor"
                            data-ace-session-mode="markdown"
                            data-ace-height="120px"
                            id="optdesc_${rkey}"
                />
                <div class="help-block">
                    <g:message code="Option.property.description.description"/>
                    <a href="http://en.wikipedia.org/wiki/Markdown" target="_blank" class="text-info">
                        <i class="glyphicon glyphicon-question-sign"></i>
                    </a>
                </div>
                <g:javascript>
                    jQuery(function () {
                        jQuery('#optedit_${rkey}').find('textarea.ace_editor').each(function () {
                            _addAceTextarea(this);
                        });
                    });
                </g:javascript>
            </div>
        </div>
        <div class="form-group ${hasErrors(bean: option, field: 'defaultValue', 'has-error')}">
            <label class="col-sm-2 control-label"><g:message code="form.option.defaultValue.label" /></label>
            <div class="col-sm-10">
                            <input type="text"
                                   class="form-control"
                                   name="defaultValue"
                                   value="${enc(attr:option?.defaultValue)}"
                                   size="40"
                                   placeholder="Default value"
                            />
            </div>

            <g:if test="${origName || option?.name && !newoption}">
                <g:hiddenField name="origName" value="${origName?origName:option?.name}"/>
            </g:if>
        </div>

        <div class="opt_sec_enabled form-group ${hasErrors(bean: option, field: 'defaultStoragePath', 'has-error')}"
             style="${wdgt.styleVisible(if: option?.secureInput)}">
            <label class="col-sm-2 control-label">
                <g:message code="form.option.defaultStoragePath.label"/>
            </label>

            <div class="col-sm-10">
                <g:set var="storagePathKey" value="${g.rkey()}"/>

                <div class="input-group">
                    <span class="input-group-addon has_tooltip" title="${message(code:"form.option.defaultStoragePath.description")}">
                        <g:icon name="lock"/>
                    </span>

                    <input type="text"
                           class="form-control"
                           id="defaultStoragePath_${storagePathKey}"
                           name="defaultStoragePath"
                           value="${enc(attr: option?.defaultStoragePath)}"
                           size="40"
                           placeholder="${message(code:"form.option.defaultStoragePath.description")}"
                    />

                    <span class="input-group-btn">
                        <g:set var="storageRoot" value="keys"/>
                        <g:set var="storageFilter" value="Rundeck-data-type=password"/>
                        <a class="btn btn-default obs-select-storage-path"
                           data-toggle="modal"
                           href="#storagebrowse"
                           data-storage-root="${enc(attr:storageRoot)}"
                           data-storage-filter="${enc(attr:storageFilter)}"
                           data-field="#defaultStoragePath_${storagePathKey}"
                        >
                            <g:message code="select" /> <g:icon name="folder-open"/>
                        </a>
                    </span>
                </div>

            </div>
        </div>

        <div class="form-group">

            <label class="col-sm-2 control-label"><g:message code="form.option.inputType.label"/></label>
            <div class="col-sm-10">


                    <div class="radio">
                        <label>
                            <g:radio name="inputType" value="plain" checked="${!option?.secureInput}" id="inputplain_${rkey}"/>
                            <g:message code="form.option.secureInput.false.label"/>
                        </label>
                    </div>


                    <div class="radio">
                        <label>
                            <g:radio
                                    name="inputType"
                                    value="secureExposed"
                                    checked="${option?.secureInput && option?.secureExposed}"
                                    id="sectrue_${rkey}"
                            />

                            <g:message code="form.option.secureExposed.true.label"/> <span class="text-danger small">&dagger;</span>
                        </label>
                        <span class="text-muted">
                            <g:message code="form.option.secureExposed.true.description"/>
                        </span>

                     </div>

                    <div class="radio">
                        <label>
                            <g:radio name="inputType" value="secure"
                                     checked="${option?.secureInput && !option?.secureExposed}"
                                     id="secexpfalse_${rkey}"/>
                            <g:message code="form.option.secureExposed.false.label"/>
                            <span class="text-danger small">&dagger;</span>
                        </label>
                        <span class="text-muted">
                            <g:message code="form.option.secureExposed.false.description"/>
                        </span>
                    </div>
                <div class="help-block">
                    <span class="text-danger small">&dagger;</span>
                    <g:message code="form.option.secureInput.description"/>
                </div>
            </div>
    
            <wdgt:eventHandler for="sectrue_${rkey}" state="unempty" inline="true" oneway="true">
                <wdgt:action target="mvfalse_${rkey}" check="true"/>
                <wdgt:action targetSelector=".opt_sec_nexp_disabled" visible="true"/>
                <wdgt:action targetSelector=".opt_sec_nexp_enabled" visible="false"/>
                <wdgt:action targetSelector=".opt_sec_disabled" visible="false"/>
                <wdgt:action targetSelector=".opt_sec_enabled" visible="true"/>
            </wdgt:eventHandler>
            <wdgt:eventHandler for="secexpfalse_${rkey}" state="unempty" inline="true" oneway="true">
                <wdgt:action targetSelector=".opt_sec_nexp_disabled" visible="false"/>
                <wdgt:action targetSelector=".opt_sec_nexp_enabled" visible="true"/>
                <wdgt:action targetSelector=".opt_sec_disabled" visible="false"/>
                <wdgt:action targetSelector=".opt_sec_enabled" visible="true"/>
            </wdgt:eventHandler>
            <wdgt:eventHandler for="inputplain_${rkey}" state="unempty" inline="true" oneway="true">
                <wdgt:action targetSelector=".opt_sec_nexp_disabled" visible="true"/>
                <wdgt:action targetSelector=".opt_sec_nexp_enabled" visible="false"/>
                <wdgt:action targetSelector=".opt_sec_disabled" visible="true"/>
                <wdgt:action targetSelector=".opt_sec_enabled" visible="false"/>
            </wdgt:eventHandler>

        </div>
        <div class="form-group">
            <label class="col-sm-2 control-label"><g:message code="form.option.values.label" /></label>
            <div class="col-sm-10">
                <g:set var="valueTypeListChecked" value="${!option || !option.realValuesUrl && params.valuesType != 'url' ? true : false}"/>
                <div>
                    <div class="radio-inline">
                        <label class=" ${hasErrors(bean: option, field: 'values', 'has-error')}">
                            <g:radio name="valuesType"
                                     value="list"
                                     checked="${valueTypeListChecked}"
                                     id="vtrlist_${rkey}"/>
                            <g:message code="form.label.valuesType.list.label" />
                        </label>
                    </div>

                    <div class="radio-inline">
                        <label class="left ${hasErrors(bean: option, field: 'valuesUrl', 'fieldError')}">
                            <g:radio name="valuesType" value="url"
                                     checked="${option?.realValuesUrl || params.valuesType == 'url' ? true : false}"
                                     id="vtrurl_${rkey}"/>
                            <g:message code="form.option.valuesType.url.label" />
                        </label>
                    </div>

                    <g:set var="listvalue" value="${option?.valuesList}"/>
                    <g:set var="listjoin" value="${option?.values }"/>

                </div>
                <div id="vlist_${rkey}_section" style="${wdgt.styleVisible(if: valueTypeListChecked)}">

                    <g:textField name="valuesList"
                                 class="form-control"
                                 value="${listvalue ? listvalue : listjoin ? listjoin.join(',') : ''}"
                                 size="60"
                                 placeholder="Comma separated list"
                                 id="vlist_${rkey}"
                    />

                </div>

                <div id="vurl_${enc(attr: rkey)}_section"
                     style="${wdgt.styleVisible(if: option?.realValuesUrl)}">
                    <g:textField type="url"
                           class=" form-control"
                           name="valuesUrl"
                           value="${option?.realValuesUrl?.toString()}"
                           size="60"
                           placeholder="Remote URL"
                           id="vurl_${rkey}"
                    />

                    <div class="help-block">
                        <g:message code="form.option.valuesUrl.description" />
                        <a href="${g.helpLinkUrl(path: '/manual/jobs.html#option-model-provider')}"
                            target="_blank">
                            <i class="glyphicon glyphicon-question-sign"></i>
                            <g:message code="rundeck.user.guide.option.model.provider" />
                        </a>
                    </div>
                </div>

                %{--automatically check appropriate radio button when text is entered in the list or url field--}%
                <wdgt:eventHandler for="vlist_${rkey}" state="unempty" target="vtrlist_${rkey}"
                                   check="true" inline="true" action="keydown"/>
                <wdgt:eventHandler for="vurl_${rkey}" state="unempty" target="vtrurl_${rkey}"
                                   check="true" inline="true" action="keydown"/>

                %{--
                auto-focus appropriate text field when a radio button is selected,
                and hide other text field
                --}%
                <wdgt:eventHandler for="vtrlist_${rkey}" state="unempty" inline="true">
                    <wdgt:action target="vlist_${rkey}" focus="true"/>
                    <wdgt:action target="vlist_${rkey}_section" visible="true"/>
                    <wdgt:action target="vurl_${rkey}_section" visible="false"/>
               </wdgt:eventHandler>
                <wdgt:eventHandler for="vtrurl_${rkey}" state="unempty"  inline="true">
                    <wdgt:action target="vurl_${rkey}" focus="true"/>
                    <wdgt:action target="vlist_${rkey}_section" visible="false"/>
                    <wdgt:action target="vurl_${rkey}_section" visible="true"/>
                </wdgt:eventHandler>
            </div>
        </div>
        <div class="form-group">
            <label class="col-sm-2 control-label"><g:message code="form.option.enforcedType.label" /></label>
            <div class="col-sm-10">
                <div class="radio">
                    <label>
                        <g:radio name="enforcedType" value="none" checked="${!option || !option?.enforced && null==option?.regex}"
                                 class="evnonregex"/>
                        <g:message code="none" />
                    </label>
                    <span class="text-muted"><g:message code="form.option.enforcedType.none.label" /></span>
                </div>
                <div class="radio">
                    <label class="${hasErrors(bean:option,field:'enforced','fieldError')}">
                        <g:radio name="enforcedType" value="enforced" checked="${option?.enforced?true:false}" class="evnonregex"/>
                        <g:message code="form.option.enforced.label" />
                    </label>
                </div>
                <div class="radio">
                    <label class="${hasErrors(bean:option,field:'regex','fieldError')}">
                        <g:radio name="enforcedType" value="regex" checked="${option?.regex?true:false}" id="etregex_${enc(attr:rkey)}"/>
                        <g:message code="form.option.regex.label" />
                    </label>
                </div>
            </div>
            <div class="col-sm-10 col-sm-offset-2">

                <g:textField
                        name="regex"
                        class="form-control"
                        value="${option?.regex}"
                        style="${wdgt.styleVisible(if: option?.regex)}"
                        size="40"
                        placeholder="Enter a Regular Expression"
                        id="vregex_${enc(attr: rkey)}"/>
                    <g:if test="${regexError}">
                        <pre class="text-danger"><g:enc>${regexError.trim()}</g:enc></pre>
                    </g:if>
            </div>
            <wdgt:eventHandler for="etregex_${rkey}" state="unempty" inline="true">
                <wdgt:action target="vregex_${rkey}" focus="true"/>
                <wdgt:action target="vregex_${rkey}" visible="true"/>
            </wdgt:eventHandler>
            <wdgt:eventHandler forSelector=".evnonregex" state="unempty" inline="true">
                <wdgt:action target="vregex_${rkey}" visible="false"/>
            </wdgt:eventHandler>
        </div>
        <div class="form-group">
            <label class="col-sm-2 control-label"><g:message code="Option.required.label" /></label>
            <div class="col-sm-10">
                <div class="radio-inline">
                    <label>
                        <g:radio name="required" value="false" checked="${!option || !option.required}"/>
                        <g:message code="no" />
                    </label>
                </div>
                <div class="radio-inline">
                    <label>
                        <g:radio name="required" value="true" checked="${option?.required}"/>
                        <g:message code="yes" />
                    </label>
                </div>
                <div class="help-block">
                    <g:message code="Option.required.description"/>
                </div>
            </div>
        </div>
        <div class="row form-inline">
            <label class="col-sm-2 control-label ${hasErrors(bean: option, field: 'multivalued', 'has-error')}">
                <g:message code="form.option.multivalued.label" />
            </label>
            <div class="col-sm-10">
                <div class=" opt_sec_disabled" style="${wdgt.styleVisible(unless: option?.secureInput)}">
                    <div class="radio-inline">
                        <label>
                            <g:radio name="multivalued" value="false" checked="${!option || !option.multivalued}" id="mvfalse_${rkey}"/>
                            <g:message code="no" />
                        </label>
                    </div>
                    <div class="radio-inline">
                        <label class="${hasErrors(bean: option, field: 'multivalued', 'fieldError')}">
                            <g:radio name="multivalued" value="true" checked="${option?.multivalued}" id="cdelimiter_${rkey}"/>
                            <g:message code="yes" />
                        </label>
                    </div>

                    <div class="help-block obs_multivalued_false" style="${wdgt.styleVisible(if: !option || !option.multivalued)}">
                        <g:message code="form.option.multivalued.description"/>
                    </div>
                    <div class="form-group obs_multivalued_true" style="${wdgt.styleVisible(if: option?.multivalued)}">
                    <div class="input-group col-sm-3 ${hasErrors(bean: option, field: 'delimiter', 'has-error')}">
                            <div class="input-group-addon">
                                <g:message code="form.option.delimiter.label" />
                            </div>
                            <input type="text"
                                   name="delimiter"
                                   value="${enc(attr:option?.delimiter)}"
                                   size="5"
                                    class="form-control"
                                   id="vdelimiter_${enc(attr:rkey)}"
                            />

                    </div>
                        <span class="help-block">
                            <g:message code="form.option.delimiter.description"/>
                        </span>

                    </div>


                    %{-- hide delimiter textfield if false is chosen --}%
                    <wdgt:eventHandler for="mvfalse_${rkey}" state="unempty" inline="true">
                        <wdgt:action targetSelector=".obs_multivalued_true" visible="false"/>
                        <wdgt:action targetSelector=".obs_multivalued_false" visible="true"/>
                    </wdgt:eventHandler>
                    %{--show delimiter textfield and focus it if true is chosen--}%
                    <wdgt:eventHandler for="cdelimiter_${rkey}" state="unempty" inline="true">
                        <wdgt:action target="vdelimiter_${rkey}" focus="true"/>
                        <wdgt:action targetSelector=".obs_multivalued_true" visible="true"/>
                        <wdgt:action targetSelector=".obs_multivalued_false" visible="false"/>
                   </wdgt:eventHandler>


                </div>
                <div class="presentation opt_sec_enabled" id="mvsecnote" style="${wdgt.styleVisible(if: option?.secureInput)}">
                    <span class="warn note"><g:message code="form.option.multivalued.secure-conflict.message"/></span>
                </div>
            </div>
        </div>
    <section id="preview_${enc(attr: rkey)}" style="${wdgt.styleVisible(if: option?.name)}" class="section-separator-solo" data-bind="visible: name">
        <div  class="row">
            <label class="col-sm-2 control-label"><g:message code="usage" /></label>
            <div class="col-sm-10 opt_sec_nexp_disabled" style="${wdgt.styleVisible(unless: option?.secureInput && !option?.secureExposed)}">
                <span class="text-muted"><g:message code="the.option.values.will.be.available.to.scripts.in.these.forms" /></span>
                <div>
                    <g:message code="bash.prompt" /> <code>$<span data-bind="text: bashVarPreview"></span></code>
                </div>
                <div>
                    <g:message code="commandline.arguments.prompt" /> <code>$<!-- -->{option.<span data-bind="text: name"></span>}</code>
                </div>
                <div>
                    <g:message code="script.content.prompt" /> <code>@option.<span data-bind="text: name"></span>@</code>
                </div>
            </div>

            <div class="col-sm-10 opt_sec_nexp_enabled" style="${wdgt.styleVisible(if: option?.secureInput && !option?.secureExposed)}">
                <span class="warn note"><g:message code="form.option.usage.secureAuth.message"/></span>
            </div>
        </div>
    </section>

        <g:hiddenField name="scheduledExecutionId" value="${scheduledExecutionId}"/>
        <div class="floatr" style="margin:10px 0;">
            <span class="warn note cancelsavemsg" style="display:none;">
                <g:message code="scheduledExecution.option.unsaved.warning"/>
            </span>
            <g:if test="${newoption}">
                <g:hiddenField name="newoption" value="true"/>
                <span class="btn btn-default btn-sm" onclick="_optcancelnew();"
                      title="${g.message(code:'form.option.cancel.title',encodeAs:'HTMLAttribute')}"><g:message code="cancel" /></span>
                <span class="btn btn-primary btn-sm" onclick="_optsavenew('optedit_${enc(attr:rkey)}', 'reqtoken_${enc(attr:rkey)}');"
                      title="${g.message(code:'form.option.create.title', encodeAs: 'HTMLAttribute')}"><g:message code="save" /></span>
                <g:javascript>
                    fireWhenReady('optname_${enc(js:rkey)}',function(){
                        $('optname_${enc(js:rkey)}').focus();
                    });
                </g:javascript>
            </g:if>
            <g:else>
                <span class="btn btn-default btn-sm" onclick="_optview('${enc(js:origName?:option?.name)}',$(this).up('li.optEntry'));"
                      title="${g.message(code:'form.option.discard.title', encodeAs: 'HTMLAttribute')}"><g:message code="discard" /></span>
                <span class="btn btn-primary btn-sm" onclick="_optsave('optedit_${enc(attr:rkey)}','reqtoken_${enc(attr:rkey)}',$(this).up('li.optEntry'));"
                      title="${g.message(code:'form.option.save.title', encodeAs: 'HTMLAttribute')}"><g:message code="save" /></span>
            </g:else>
        </div>
        <g:jsonToken id="reqtoken_${rkey}" url="${request.forwardURI}"/>
        <div class="clear"></div>
    </div>
<g:javascript>
                fireWhenReady('optedit_${enc(js: rkey)}',function(){
                    var editor=new OptionEditor({name:"${option?.name}",bashVarPrefix:'${DataContextUtils.ENV_VAR_PREFIX}'});
                    ko.applyBindings(editor,jQuery('#optedit_${enc(js:rkey)}')[0]);
                });
</g:javascript>
</div>
