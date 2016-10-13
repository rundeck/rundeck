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

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:set var="tkey" value="${g.rkey()}"/>
<div class="notifyFields form-group"
     style="${wdgt.styleVisible(if: isVisible)}">

    %{-- ${trigger} --}%
    <div class="control-label text-form-label col-sm-2">
        <g:message code="notification.event.${trigger}"/>
    </div>
    <div class="col-sm-10 ">
        <div class="row row-space form-inline">
            <div class="form-group  col-sm-12 ${hasErrors(bean: scheduledExecution, field: triggerEmailRecipientsName,
                    'has-error')} ">
                <g:checkBox name="${triggerEmailCheckboxName}" value="true" checked="${isEmail}"/>
                <label for="${enc(attr:triggerEmailCheckboxName)}">
                    <g:message code="notification.email.label" />
                </label>
            </div>

            <div id="notifholder${tkey}" style="${wdgt.styleVisible(if: isEmail)}">
            <div  class="well well-sm well-nobg">
                <div class="form-group col-sm-5 ${hasErrors(bean: scheduledExecution, field:
                        triggerEmailRecipientsName,
                        'has-error')} ">
                    <div class="input-group  input-group-sm">
                    <label class="input-group-addon"><g:message code="to" /></label>
                    <g:textField
                            name="${triggerEmailRecipientsName}"
                            value="${params[triggerEmailRecipientsName] ?: defEmail?.mailConfiguration()?.recipients}"
                            class="form-control"
                            size="60"
                            placeholder="user@dns.tld"
                    />
                        <g:helpTooltip code="notification.email.description"
                                       css="input-group-addon text-info"/>
                    </div>

                </div>

                <div class="form-group col-sm-5 ${hasErrors(bean: scheduledExecution, field: triggerEmailSubjectName
                        , 'has-error')} ">
                    <div class="input-group input-group-sm">
                    <label class=" input-group-addon" for="${enc(attr:triggerEmailSubjectName)}"><g:message code="subject" />
                    </label>
                        <g:textField
                                name="${triggerEmailSubjectName}"
                                value="${params[triggerEmailSubjectName] ?: defEmail?.mailConfiguration()?.subject}"
                                class="form-control "
                                size="60"
                                placeholder="Subject"/>
                        <g:helpTooltip code="notification.email.subject.description" css="input-group-addon text-info"/>
                        <span class=" input-group-addon">
                            <a  href="${g.message(code: 'notification.email.subject.helpLink')}" title="Help docs"
                                target="_blank">
                                <i class="glyphicon glyphicon-circle-arrow-right"></i>
                            </a>
                        </span>
                    </div>

                </div>

                <g:if test="${triggerEmailAttachName}">
                    <div class="form-group col-sm-2 ${hasErrors(bean: scheduledExecution, field:
                            triggerEmailAttachName,
                            'has-error')} ">
                        <g:set var="attachTrue" value="${params[triggerEmailAttachName] == 'true' ||
                            defEmail?.mailConfiguration()?.attachLog in ['true', true]}"/>
                        <label class="${attachTrue ? 'active' : ''}">
                            <g:checkBox
                                    name="${triggerEmailAttachName}"
                                    value="true"
                                    checked="${attachTrue}"
                            />
                            <g:message code="attach.output.log" />
                        </label>

                    </div>
                </g:if>

        </div>

            <g:hasErrors bean="${scheduledExecution}" field="${triggerEmailRecipientsName }">
                <div class="col-sm-12 text-warning">
                    <g:renderErrors bean="${scheduledExecution}" as="list" field="${triggerEmailRecipientsName}"/>
                </div>
            </g:hasErrors>
            <wdgt:eventHandler for="${triggerEmailCheckboxName}" state="checked" target="notifholder${tkey}" visible="true"
            />
        </div>
        </div>

        <div class="row row-space form-inline">
            <div class="form-group col-sm-12 ${hasErrors(bean: scheduledExecution, field: triggerUrlFieldName, 'has-error')}">
                <g:checkBox name="${triggerUrlCheckboxName}" value="true" checked="${isUrl}"/>
                <label for="${enc(attr:triggerUrlCheckboxName)}">
                    <g:message code="notification.webhook.label" />
                </label>
            </div>
            <div id="notifholder_url_${tkey}" style="${wdgt.styleVisible(if: isUrl)}">
                <div class="well well-sm well-nobg">
                    <div class="form-group col-sm-10 ${hasErrors(bean: scheduledExecution, field: triggerUrlFieldName,
                            'has-error')} ">

                            <g:set var="notifurlcontent"
                                   value="${params[triggerUrlFieldName] ?: defUrl?.content}"/>
                            <g:if test="${notifurlcontent && notifurlcontent.size() > 100}">
                                <textarea name="${enc(attr:triggerUrlFieldName)}"
                                          style="vertical-align:top;"
                                          placeholder="http://"
                                          rows="6"
                                          cols="40"
                                          class="form-control "><g:enc>${notifurlcontent}</g:enc></textarea>

                                <span class=" text-muted">
                                    <g:message code="notification.webhook.field.description"/>
                                </span>
                            </g:if>
                        <g:else>
                                <div class="input-group  input-group-sm">
                                        <label class="input-group-addon"><g:message
                                        code="notification.webhook.field.title"/></label>
                                <g:textField name="${triggerUrlFieldName}"
                                             value="${notifurlcontent}"
                                             class="form-control "
                                             size="60"
                                             placeholder="http://"/>
                                <g:helpTooltip code="notification.webhook.field.description"
                                               css="input-group-addon text-info"/>
                                </div>
                            </g:else>


                    </div>



                <g:hasErrors bean="${scheduledExecution}" field="${triggerUrlFieldName}">
                    <div class="col-sm-12 text-warning">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="${triggerUrlFieldName}"/>
                    </div>
                </g:hasErrors>

                </div>
            </div>
            <wdgt:eventHandler for="${triggerUrlCheckboxName}" state="checked" target="notifholder_url_${tkey}" visible="true"/>
        </div>
        <g:each in="${notificationPlugins?.keySet()}" var="pluginName">
            <g:set var="plugin" value="${notificationPlugins[pluginName]}"/>
            <g:set var="pluginInstance" value="${notificationPlugins[pluginName]['instance']}"/>
            <g:set var="pkey" value="${g.rkey()}"/>
            <g:set var="definedNotif" value="${scheduledExecution?.findNotification(trigger, pluginName)}"/>
            <g:set var="definedConfig"
                   value="${params.notifyPlugin?.get(trigger)?.get(pluginName)?.config ?: definedNotif?.configuration}"/>
            <g:set var="pluginDescription" value="${plugin.description}"/>
            <g:set var="validation" value="${notificationValidation?.get(trigger)?.get(pluginName)?.report}"/>
            <g:set var="checkboxFieldName" value="notifyPlugin.${trigger}.enabled.${pluginName}"/>

            <div class="row row-space">
                <div class="col-sm-12">

                <g:hiddenField name="notifyPlugin.${trigger}.type" value="${pluginName}"/>
                <div>
                    <g:checkBox name="${checkboxFieldName}" value="true"
                                checked="${definedNotif ? true : false}"/>
                    <label for="${enc(attr:checkboxFieldName)}">
                    <stepplugin:pluginIcon service="Notification"
                                           name="${pluginName}"
                                           width="16px"
                                           height="16px">
                    </stepplugin:pluginIcon>
                    <stepplugin:message
                            service="Notification"
                            name="${pluginName}"
                            code="plugin.title"
                            default="${pluginDescription.title?:pluginName}"/>
                </label>
                    <g:if test="${pluginDescription['description']}">
                        <span class="text-muted"><g:render template="/scheduledExecution/description"
                                                           model="[description:
                                                                           stepplugin.messageText(
                                                                                   service: 'Notification',
                                                                                   name: pluginName,
                                                                                   code: 'plugin.description',
                                                                                   default: pluginDescription.description
                                                                           ),
                                                                   textCss    : '',
                                                                   mode       : 'hidden', rkey: g.rkey()]"/></span>
                    </g:if>
                </div>



                <div id="notifholderPlugin${enc(attr:pkey)}" style="${wdgt.styleVisible(if: definedNotif ? true : false)}"
                      class="notificationplugin panel panel-default">
                    <div class="panel-body">
                    <g:set var="prefix" value="${'notifyPlugin.'+trigger+'.' + pluginName + '.config.'}"/>
                    <g:if test="${pluginDescription instanceof Description}">
                        <div class="form-horizontal">

                            <g:if test="${pluginDescription?.properties}">
                                <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                        service:com.dtolabs.rundeck.plugins.ServiceNameConstants.Notification,
                                        provider:pluginDescription.name,
                                        properties:pluginDescription?.properties,
                                        report:validation,
                                        prefix:prefix,
                                        values:definedConfig,
                                        fieldnamePrefix:prefix,
                                        origfieldnamePrefix:'orig.' + prefix,
                                        allowedScope:PropertyScope.Instance
                                ]}"/>
                            </g:if>

                            <g:if test="${!pluginDescription?.properties}">
                                <span class="text-muted">
                                    <g:message code="notification.plugin.configuration.noproperties.message"
                                               args="${pluginDescription['title'] ?:
                                                       pluginDescription['name'] ?: pluginName}"/>
                                </span>
                            </g:if>
                        </div>
                    </g:if>
                    </div>
                </div>
                <wdgt:eventHandler for="${checkboxFieldName}" state="checked"
                                   target="notifholderPlugin${pkey}" visible="true"/>
            </div>
            </div>
        </g:each>
    </div>
</div>
