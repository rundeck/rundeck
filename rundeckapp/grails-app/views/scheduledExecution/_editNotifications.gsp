<g:set var="notifications" value="${scheduledExecution.notifications}"/>
<g:set var="defSuccess" value="${scheduledExecution.findNotification('onsuccess', 'email')}"/>
<g:set var="isSuccess" value="${params.notifySuccessRecipients && 'true' == params.notifyOnsuccess || defSuccess}"/>
<g:set var="defSuccessUrl" value="${scheduledExecution.findNotification('onsuccess', 'url')}"/>
<g:set var="isSuccessUrl" value="${params.notifySuccessUrl && 'true' == params.notifyOnsuccessUrl || defSuccessUrl}"/>

<g:set var="defFailure" value="${scheduledExecution.findNotification('onfailure', 'email')}"/>
<g:set var="isFailure" value="${params.notifyFailureRecipients && 'true' == params.notifyOnfailure || defFailure}"/>
<g:set var="defFailureUrl" value="${scheduledExecution.findNotification('onfailure', 'url')}"/>
<g:set var="isFailureUrl" value="${params.notifyFailureUrl && 'true' == params.notifyOnfailureUrl || defFailureUrl}"/>
<tr>
    <td>
        Send Notification?
    </td>
    <td>
        <label>
            <g:radio value="false" name="notified"
                     checked="${!(notifications || params.notified=='true')}"
                     id="notifiedFalse"/>
            No
        </label>

        <label>
            <g:radio name="notified" value="true"
                     checked="${notifications || params.notified == 'true'}"
                     id="notifiedTrue"/>
            Yes
        </label>

        <g:javascript>
            <wdgt:eventHandlerJS for="notifiedTrue" state="unempty">
                <wdgt:action visible="true" targetSelector="tr.notifyFields"/>
            </wdgt:eventHandlerJS>
            <wdgt:eventHandlerJS for="notifiedFalse" state="unempty">
                <wdgt:action visible="false" targetSelector="tr.notifyFields"/>

                <wdgt:action check="false" target="notifyOnsuccess"/>
                <wdgt:action visible="false" target="notifSuccessholder"/>
                <wdgt:action check="false" target="notifyOnfailure"/>
                <wdgt:action visible="false" target="notifFailureholder"/>
            </wdgt:eventHandlerJS>
        </g:javascript>
    </td>
</tr>
<tr class="notifyFields" style="${wdgt.styleVisible(if: isFailure || isSuccess || isSuccessUrl || isFailureUrl)}">

    <!-- onsuccess-->
    <td>
        <label for="notifyOnsuccess"
               class=" ${hasErrors(bean: scheduledExecution, field: 'notifySuccessRecipients', 'fieldError')} ${hasErrors(bean: scheduledExecution, field: 'notifySuccessUrl', 'fieldError')}">
            <g:message code="notification.event.onsuccess"/>
        </label>
    </td>
    <td>
        <div>
            <span>
                <g:checkBox name="notifyOnsuccess" value="true" checked="${isSuccess ? true : false}"/>
                <label for="notifyOnsuccess">Send Email</label>
            </span>
            <span id="notifSuccessholder" style="${wdgt.styleVisible(if: isSuccess)}">
                <label>to: <g:textField name="notifySuccessRecipients" cols="70" rows="3"
                                        value="${defSuccess ? defSuccess.content : params.notifySuccessRecipients}"
                                        size="60"/></label>

                <div class="info note">comma-separated email addresses</div>
                <g:hasErrors bean="${scheduledExecution}" field="notifySuccessRecipients">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="notifySuccessRecipients"/>
                    </div>
                </g:hasErrors>
            </span>
            <wdgt:eventHandler for="notifyOnsuccess" state="checked" target="notifSuccessholder" visible="true"/>
        </div>

        <div>
            <span>
                <g:checkBox name="notifyOnsuccessUrl" value="true" checked="${isSuccessUrl ? true : false}"/>
                <label for="notifyOnsuccessUrl">Webhook</label>
            </span>
            <span id="notifSuccessholder2" style="${wdgt.styleVisible(if: isSuccessUrl)}">
                <label>POST to URLs:
                    <g:set var="notifsuccessurlcontent"
                           value="${defSuccessUrl ? defSuccessUrl.content : params.notifySuccessUrl}"/>
                    <g:if test="${notifsuccessurlcontent && notifsuccessurlcontent.length() > 30}">
                        <textarea name="notifySuccessUrl"
                                  style="vertical-align:top;"
                                  rows="6" cols="40">${notifsuccessurlcontent?.encodeAsHTML()}</textarea>
                    </g:if>
                    <g:else>
                        <g:textField name="notifySuccessUrl" cols="70" rows="3"
                                     value="${notifsuccessurlcontent?.encodeAsHTML()}" size="60"/>
                    </g:else>
                </label>

                <div class="info note">comma-separated URLs</div>
                <g:hasErrors bean="${scheduledExecution}" field="notifySuccessUrl">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="notifySuccessUrl"/>
                    </div>
                </g:hasErrors>
            </span>
            <wdgt:eventHandler for="notifyOnsuccessUrl" state="checked" target="notifSuccessholder2" visible="true"/>
        </div>

        %{--TODO: list plugin types--}%
        <g:each in="${notificationPlugins?.keySet()}" var="pluginName">
        <g:set var="plugin" value="${notificationPlugins[pluginName]}"/>
        <g:set var="pluginInstance" value="${notificationPlugins[pluginName]['instance']}"/>
        <g:set var="pkey" value="${g.rkey()}"/>
        <g:set var="definedNotif" value="${scheduledExecution?.findNotification('onsuccess',pluginName)}"/>
        <g:set var="definedConfig" value="${definedNotif?.configuration?:[:]}"/>
        <g:set var="pluginDescription" value="${plugin.description}"/>
        <div>
            <span>
                <g:checkBox name="notifyOnsuccessPlugin.${pluginName.encodeAsHTML()}" value="true" checked="${definedNotif ? true : false}"/>
                <label for="notifyOnsuccessPlugin.${pluginName.encodeAsHTML()}">${pluginDescription['title']?: pluginDescription['name']?: pluginName}</label>
                <g:if test="${pluginDescription['description']}">
                    <span class="info note">${pluginDescription['description']?.encodeAsHTML()}</span>
                </g:if>
            </span>
            <span id="notifSuccessholderPlugin${pkey}" style="${wdgt.styleVisible(if: definedNotif ? true : false)}"
                  class="notificationplugin">
                %{--<label>Content:--}%
                    %{--<textarea name="notifySuccessPluginContent.${pluginName}"--}%
                              %{--style="vertical-align:top;"--}%
                              %{--rows="6" cols="40">${params.notifySuccessPluginContent?.encodeAsHTML()}</textarea>--}%
                %{--</label>--}%
                <g:set var="prefix" value="${('notifySuccessPluginConfig.' + pluginName + '.').encodeAsHTML()}"/>
                %{--XXX: TODO: use plugin descriptor properties?--}%
                %{--<g:each in="${pluginInstance.configurationProperties?.keySet()}" var="confKey">--}%
                    %{--<label for="${rkey}_conf_${confKey.encodeAsHTML()}">${pluginInstance.configurationProperties[confKey]['title']?:confKey}</label>--}%
                    %{--<g:if test="${pluginInstance.configurationProperties[confKey]}">--}%
                        %{--<input type="text" name="${prefix}${confKey.encodeAsHTML()}"--}%
                               %{--placeholder="${pluginInstance.configurationProperties[confKey]['placeholder']?.encodeAsHTML()}"--}%
                               %{--value="${definedConfig?.get(confKey)?.encodeAsHTML()}"--}%
                            %{--id="${rkey}_conf_${confKey.encodeAsHTML()}"--}%
                        %{--/>--}%
                    %{--</g:if>--}%
                    %{--<span class="info note">${pluginInstance.configurationProperties[confKey]['description']?.encodeAsHTML()}</span>--}%
                %{--</g:each>--}%
                <g:if test="${pluginDescription instanceof com.dtolabs.rundeck.core.plugins.configuration.Description}">
                    <table class="simpleForm">
                    <g:each in="${pluginDescription?.properties}" var="prop">
                        <tr>
                            <g:render
                                    template="/framework/pluginConfigPropertyField"
                                    model="${[prop: prop, prefix: prefix,
                                            error: nodeexecreport?.errors ? nodeexecreport?.errors[prop.name] : null,
                                            values: definedConfig,
                                            fieldname: prefix+ prop.name,
                                            origfieldname: 'orig.' + prefix + prop.name]}"/>
                        </tr>
                    </g:each>
                    </table>
                </g:if>
                %{--<g:hasErrors bean="${scheduledExecution}" field="notifySuccessUrl">--}%
                    %{--<div class="fieldError">--}%
                        %{--<g:renderErrors bean="${scheduledExecution}" as="list" field="notifySuccessUrl"/>--}%
                    %{--</div>--}%
                %{--</g:hasErrors>--}%
            </span>
            <wdgt:eventHandler for="notifyOnsuccessPlugin.${pluginName}" state="checked" target="notifSuccessholderPlugin${pkey}" visible="true"/>
        </div>
        </g:each>
    </td>
</tr>

<tr class="notifyFields" style="${wdgt.styleVisible(if: isFailure || isSuccess || isSuccessUrl || isFailureUrl)}">

    <!-- onfailure-->
    <td>
        <label for="notifyOnfailure"
               class=" ${hasErrors(bean: scheduledExecution, field: 'notifyFailureRecipients', 'fieldError')}">
            <g:message code="notification.event.onfailure"/>
        </label>
    </td>
    <td>
        <div>
            <span>
                <g:checkBox name="notifyOnfailure" value="true" checked="${isFailure ? true : false}"/>
                <label for="notifyOnfailure">Send Email</label>
            </span>
            <span id="notifFailureholder" style="${wdgt.styleVisible(if: isFailure)}">
                <label>to: <g:textField name="notifyFailureRecipients" cols="70" rows="3"
                                        value="${defFailure ? defFailure.content : params.notifyFailureRecipients}"
                                        size="60"/></label>

                <div class="info note">comma-separated email addresses</div>
                <g:hasErrors bean="${scheduledExecution}" field="notifyFailureRecipients">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="notifyFailureRecipients"/>
                    </div>
                </g:hasErrors>
            </span>
            <wdgt:eventHandler for="notifyOnfailure" state="checked" target="notifFailureholder" visible="true"/>
        </div>

        <div>
            <span>
                <g:checkBox name="notifyOnfailureUrl" value="true" checked="${isFailureUrl ? true : false}"/>
                <label for="notifyOnfailureUrl">Webhook</label>
            </span>
            <span id="notifFailureholder2" style="${wdgt.styleVisible(if: isFailureUrl)}">
                <label>POST to URLs:
                    <g:set var="notiffailureurlcontent"
                           value="${defFailureUrl ? defFailureUrl.content : params.notifyFailureUrl}"/>
                    <g:if test="${notiffailureurlcontent && notiffailureurlcontent.length() > 30}">
                        <textarea name="notifyFailureUrl"
                                  style="vertical-align:top;"
                                  rows="6" cols="40">${notiffailureurlcontent?.encodeAsHTML()}</textarea>
                    </g:if>
                    <g:else>
                        <g:textField name="notifyFailureUrl" cols="70" rows="3"
                                     value="${notiffailureurlcontent?.encodeAsHTML()}" size="60"/>
                    </g:else>
                </label>

                <div class="info note">comma-separated URLs</div>
                <g:hasErrors bean="${scheduledExecution}" field="notifyFailureUrl">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="notifyFailureUrl"/>
                    </div>
                </g:hasErrors>
            </span>
            <wdgt:eventHandler for="notifyOnfailureUrl" state="checked" target="notifFailureholder2" visible="true"/>
        </div>
    </td>
</tr>
