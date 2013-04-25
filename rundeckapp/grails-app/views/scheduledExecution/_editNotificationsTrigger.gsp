<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:set var="tkey" value="${g.rkey()}"/>
<g:set var="Trigger" value="${trigger[0].toUpperCase()+trigger.substring(1)}"/>
<g:set var="defEmail" value="${definedNotifications?.find {it.type=='email'}}"/>
<g:set var="defUrl" value="${definedNotifications?.find {it.type=='url'}}"/>
<g:set var="isEmail" value="${defEmail || params['notify'+Trigger+'Recipients'] && 'true' == params['notifyOn'+trigger]}"/>
<g:set var="notifyRecipients" value="${'notify' + Trigger + 'Recipients'}"/>
<g:set var="notifyUrl" value="${'notify' + Trigger + 'Url'}"/>
<tr class="notifyFields" style="${wdgt.styleVisible(if: isVisible)}">

    <!-- ${trigger} -->
    <td>
        <label for="notifyOn${trigger}"
               class=" ${hasErrors(bean: scheduledExecution, field: notifyRecipients, 'fieldError')} ${hasErrors(bean: scheduledExecution, field: notifyUrl, 'fieldError')}">
            <g:message code="notification.event.on${trigger}"/>
        </label>
    </td>
    <td>
        <div>
            <span>
                <g:checkBox name="notifyOn${trigger}" value="true" checked="${isEmail}"/>
                <label for="notifyOn${trigger}">Send Email</label>
            </span>
            <span id="notifholder${tkey}" style="${wdgt.styleVisible(if: isEmail)}">
                <label>to: <g:textField name="notify${Trigger}Recipients" cols="70" rows="3"
                                        value="${defEmail?.content ?: params[notifyRecipients]}"
                                        size="60"/></label>

                <div class="info note">comma-separated email addresses. You can substitute these variables:
                    $<!---->{job.user.name}, $<!---->{job.user.email}
                </div>
                <g:hasErrors bean="${scheduledExecution}" field="${notifyRecipients}">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="${notifyRecipients}"/>
                    </div>
                </g:hasErrors>
            </span>
            <wdgt:eventHandler for="notifyOn${trigger}" state="checked" target="notifholder${tkey}" visible="true"/>
        </div>

        <div>
            <span>
                <g:checkBox name="notifyOn${trigger}Url" value="true" checked="${isUrl}"/>
                <label for="notifyOn${trigger}Url">Webhook</label>
            </span>
            <span id="notifholder_url_${tkey}" style="${wdgt.styleVisible(if: isUrl)}">
                <label>POST to URLs:
                    <g:set var="notifurlcontent"
                           value="${defUrl?.content ?: params[notifyUrl]}"/>
                    <g:if test="${notifurlcontent && notifurlcontent.size() > 30}">
                        <textarea name="${notifyUrl}"
                                  style="vertical-align:top;"
                                  rows="6" cols="40">${notifurlcontent?.encodeAsHTML()}</textarea>
                    </g:if>
                    <g:else>
                        <g:textField name="${notifyUrl}" cols="70" rows="3"
                                     value="${notifurlcontent?.encodeAsHTML()}" size="60"/>
                    </g:else>
                </label>

                <div class="info note">comma-separated URLs</div>
                <g:hasErrors bean="${scheduledExecution}" field="${notifyUrl}">
                    <div class="fieldError">
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
                        <span class="info note">${pluginDescription['description']?.encodeAsHTML()}</span>
                    </g:if>
                </span>
                <span id="notifholderPlugin${pkey}" style="${wdgt.styleVisible(if: definedNotif ? true : false)}"
                      class="notificationplugin">
                    <g:set var="prefix" value="${('notifyPlugin.'+trigger+'.' + pluginName + '.config.').encodeAsHTML()}"/>
                    <g:if test="${pluginDescription instanceof Description}">
                        <table class="simpleForm">
                            <g:each in="${pluginDescription?.properties}" var="prop">
                                <tr>
                                    <g:render
                                            template="/framework/pluginConfigPropertyField"
                                            model="${[prop: prop, prefix: prefix,
                                                    error: validation?.errors ? validation?.errors[prop.name] : null,
                                                    values: definedConfig,
                                                    fieldname: prefix + prop.name,
                                                    origfieldname: 'orig.' + prefix + prop.name]}"/>
                                </tr>
                            </g:each>
                        </table>
                    </g:if>
                </span>
                <wdgt:eventHandler for="${checkboxFieldName}" state="checked"
                                   target="notifholderPlugin${pkey}" visible="true"/>
            </div>
        </g:each>
    </td>
</tr>
