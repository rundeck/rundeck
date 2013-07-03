<g:set var="notifications" value="${scheduledExecution.notifications}"/>
<g:set var="defSuccess" value="${scheduledExecution.findNotification('onsuccess', 'email')}"/>
<g:set var="isSuccess" value="${params.notifySuccessRecipients && 'true' == params.notifyOnsuccess || defSuccess}"/>
<g:set var="defSuccessUrl" value="${scheduledExecution.findNotification('onsuccess', 'url')}"/>
<g:set var="isSuccessUrl" value="${params.notifySuccessUrl && 'true' == params.notifyOnsuccessUrl || defSuccessUrl}"/>

<g:set var="defFailure" value="${scheduledExecution.findNotification('onfailure', 'email')}"/>
<g:set var="isFailure" value="${params.notifyFailureRecipients && 'true' == params.notifyOnfailure || defFailure}"/>
<g:set var="defFailureUrl" value="${scheduledExecution.findNotification('onfailure', 'url')}"/>
<g:set var="isFailureUrl" value="${params.notifyFailureUrl && 'true' == params.notifyOnfailureUrl || defFailureUrl}"/>

<g:set var="defStart" value="${scheduledExecution.findNotification('onstart', 'email')}"/>
<g:set var="defStartUrl" value="${scheduledExecution.findNotification('onstart', 'url')}"/>
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
<g:render template="/scheduledExecution/editNotificationsTrigger"
    model="${[
            isVisible:( notifications ),
            trigger:'success',
            isEmail:isSuccess,
            isUrl:isSuccessUrl,
            definedNotifications: scheduledExecution.notifications?.findAll{it.eventTrigger=='onsuccess'},
            adminauth: adminauth
    ]}"
    />
<g:render template="/scheduledExecution/editNotificationsTrigger"
          model="${[
                  isVisible: (notifications),
                  trigger: 'failure',
                  isEmail: isFailure,
                  isUrl: isFailureUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == 'onfailure' },
                  adminauth: adminauth
          ]}"/>

<g:render template="/scheduledExecution/editNotificationsTrigger"
          model="${[
                  isVisible: (notifications),
                  trigger: 'start',
                  isEmail: defStart,
                  isUrl: defStartUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == 'onstart' },
                  adminauth: adminauth
          ]}"/>
