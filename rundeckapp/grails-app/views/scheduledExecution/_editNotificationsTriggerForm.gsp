<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:set var="tkey" value="${g.rkey()}"/>
<g:set var="Trigger" value="${trigger[0].toUpperCase()+trigger.substring(1)}"/>
<g:set var="defEmail" value="${definedNotifications?.find {it.type=='email'}}"/>
<g:set var="defUrl" value="${definedNotifications?.find {it.type=='url'}}"/>
<g:set var="isEmail" value="${defEmail || params['notify'+Trigger+'Recipients'] && 'true' == params['notifyOn'+trigger]}"/>
<g:set var="notifyRecipients" value="${'notify' + Trigger + 'Recipients'}"/>
<g:set var="notifyUrl" value="${'notify' + Trigger + 'Url'}"/>
<div class="notifyFields form-group ${hasErrors(bean: scheduledExecution, field: notifyRecipients, 'has-error')} ${hasErrors(bean: scheduledExecution, field: notifyUrl, 'has-error')}"
     style="${wdgt.styleVisible(if: isVisible)}">

    <!-- ${trigger} -->
    <div class="control-label text-form-label col-sm-2">
        <g:message code="notification.event.on${trigger}"/>
    </div>
    <div class="col-sm-10">
        <div class="form-inline">
            <div class="form-group">
                <g:checkBox name="notifyOn${trigger}Email" value="true" checked="${isEmail}"/>
                <label for="notifyOn${trigger}Email">
                    Send Email
                </label>
            </div>
            <span id="notifholder${tkey}" style="${wdgt.styleVisible(if: isEmail)}">
                <div class="form-group">
                    <label class="sr-only">to</label>
                    <g:textField
                            name="notify${Trigger}Recipients"
                            value="${defEmail?.content ?: params[notifyRecipients]}"
                            class="form-control input-sm"
                            size="60"
                            placeholder="user@dns.tld"
                    />

                </div>

                <div class="help-block">
                    <g:message code="notification.email.description"/>
                </div>
            </span>

            <g:hasErrors bean="${scheduledExecution}" field="${notifyRecipients}">
                <div class="text-warning">
                    <g:renderErrors bean="${scheduledExecution}" as="list" field="${notifyRecipients}"/>
                </div>
            </g:hasErrors>
            <wdgt:eventHandler for="notifyOn${trigger}Email" state="checked" target="notifholder${tkey}" visible="true"
            />
        </div>

        <div class="form-inline">
            <div class="form-group">
                <g:checkBox name="notifyOn${trigger}Url" value="true" checked="${isUrl}"/>
                <label for="notifyOn${trigger}Url">
                    Webhook
                </label>
            </div>
            <span id="notifholder_url_${tkey}" style="${wdgt.styleVisible(if: isUrl)}">
                <div class="form-group">
                    <label class="sr-only">POST to URLs:</label>
                    <g:set var="notifurlcontent"
                               value="${defUrl?.content ?: params[notifyUrl]}"/>
                    <g:if test="${notifurlcontent && notifurlcontent.size() > 30}">
                        <textarea name="${notifyUrl}"
                                  style="vertical-align:top;"
                                  placeholder="http://"
                                  rows="6"
                                  cols="40"
                                  class="form-control input-sm">${notifurlcontent?.encodeAsHTML()}</textarea>
                    </g:if>
                    <g:else>
                        <g:textField name="${notifyUrl}"
                                     value="${notifurlcontent?.encodeAsHTML()}"
                                     class="form-control input-sm"
                                     size="60"
                                     placeholder="http://"
                        />
                    </g:else>
                </div>

                <div class="help-block">comma-separated URLs</div>
                <g:hasErrors bean="${scheduledExecution}" field="${notifyUrl}">
                    <div class="text-warning">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="${notifyUrl}"/>
                    </div>
                </g:hasErrors>
            </span>
            <wdgt:eventHandler for="notifyOn${trigger}Url" state="checked" target="notifholder_url_${tkey}" visible="true"/>
        </div>
        <g:each in="${notificationPlugins?.keySet()}" var="pluginName">
            <g:set var="plugin" value="${notificationPlugins[pluginName]}"/>
            <g:set var="pluginInstance" value="${notificationPlugins[pluginName]['instance']}"/>
            <g:set var="pkey" value="${g.rkey()}"/>
            <g:set var="definedNotif" value="${scheduledExecution?.findNotification('on'+trigger, pluginName)}"/>
            <g:set var="definedConfig"
                   value="${params.notifyPlugin?.get(trigger)?.get(pluginName)?.config ?: definedNotif?.configuration}"/>
            <g:set var="pluginDescription" value="${plugin.description}"/>
            <g:set var="validation" value="${notificationValidation?.get('on'+trigger)?.get(pluginName)?.report}"/>
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
                                <g:if test="${!outofscope || adminauth}">
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
