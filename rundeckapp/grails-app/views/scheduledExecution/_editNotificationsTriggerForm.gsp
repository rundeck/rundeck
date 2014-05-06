<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:set var="tkey" value="${g.rkey()}"/>
<div class="notifyFields form-group"
     style="${wdgt.styleVisible(if: isVisible)}">

    <!-- ${trigger} -->
    <div class="control-label text-form-label col-sm-2">
        <g:message code="notification.event.${trigger}"/>
    </div>
    <div class="col-sm-10 ">
        <div class="row row-space form-inline">
            <div class="form-group  col-sm-12 ${hasErrors(bean: scheduledExecution, field: triggerEmailRecipientsName,
                    'has-error')} ">
                <g:checkBox name="${triggerEmailCheckboxName}" value="true" checked="${isEmail}"/>
                <label for="${triggerEmailCheckboxName}">
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
                    <label class=" input-group-addon" for="${triggerEmailSubjectName}"><g:message code="subject" />
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
                <div class="text-warning">
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
                <label for="${triggerUrlCheckboxName}">
                    <g:message code="notification.webhook.label" />
                </label>
            </div>
            <span id="notifholder_url_${tkey}" style="${wdgt.styleVisible(if: isUrl)}">
                <div class="form-group  ${hasErrors(bean: scheduledExecution, field: triggerUrlFieldName, 'has-error')}">
                    <label class="sr-only"><g:message code="notification.webhook.field.title" /></label>
                    <g:set var="notifurlcontent"
                               value="${params[triggerUrlFieldName] ?: defUrl?.content}"/>
                    <g:if test="${notifurlcontent && notifurlcontent.size() > 30}">
                        <textarea name="${triggerUrlFieldName}"
                                  style="vertical-align:top;"
                                  placeholder="http://"
                                  rows="6"
                                  cols="40"
                                  class="form-control input-sm">${notifurlcontent?.encodeAsHTML()}</textarea>
                    </g:if>
                    <g:else>
                        <g:textField name="${triggerUrlFieldName}"
                                     value="${notifurlcontent?.encodeAsHTML()}"
                                     class="form-control input-sm"
                                     size="60"
                                     placeholder="http://"
                        />
                    </g:else>
                </div>

                <div class="help-block"><g:message code="notification.webhook.field.description" /></div>
                <g:hasErrors bean="${scheduledExecution}" field="${triggerUrlFieldName}">
                    <div class="text-warning">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="${triggerUrlFieldName}"/>
                    </div>
                </g:hasErrors>
            </span>
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
            <g:set var="checkboxFieldName" value="notifyPlugin.${trigger}.enabled.${pluginName.encodeAsHTML()}"/>
            <div>
                <g:hiddenField name="notifyPlugin.${trigger}.type" value="${pluginName}"/>
                <span>
                    <g:checkBox name="${checkboxFieldName}" value="true"
                                checked="${definedNotif ? true : false}"/>
                    <label for="${checkboxFieldName}">${pluginDescription['title'] ?: pluginDescription['name'] ?: pluginName}</label>
                    <g:if test="${pluginDescription['description']}">
                        <span class="text-muted">${pluginDescription['description']?.encodeAsHTML()}</span>
                    </g:if>
                </span>
                <div id="notifholderPlugin${pkey}" style="${wdgt.styleVisible(if: definedNotif ? true : false)}"
                      class="notificationplugin panel panel-default">
                    <div class="panel-body">
                    <g:set var="prefix" value="${('notifyPlugin.'+trigger+'.' + pluginName + '.config.').encodeAsHTML()}"/>
                    <g:if test="${pluginDescription instanceof Description}">
                        <div class="form-horizontal">
                            <g:each in="${pluginDescription?.properties}" var="prop">
                                <g:set var="outofscope" value="${prop.scope && !prop.scope.isInstanceLevel() && !prop.scope.isUnspecified()}"/>
                                <g:if test="${!outofscope}">
                                    <g:render
                                            template="/framework/pluginConfigPropertyFormField"
                                            model="${[prop: prop, prefix: prefix,
                                                    error: validation?.errors ? validation?.errors[prop.name] : null,
                                                    values: definedConfig,
                                                    fieldname: prefix + prop.name,
                                                    origfieldname: 'orig.' + prefix + prop.name,
                                                    outofscope: outofscope,
                                                    pluginName: pluginName
                                            ]}"/>
                                </g:if>
                            </g:each>
                        </div>
                    </g:if>
                    </div>
                </div>
                <wdgt:eventHandler for="${checkboxFieldName}" state="checked"
                                   target="notifholderPlugin${pkey}" visible="true"/>
            </div>
        </g:each>
    </div>
</div>
