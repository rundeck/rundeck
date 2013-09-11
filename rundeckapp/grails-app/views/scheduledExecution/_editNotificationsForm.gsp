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
<div class="form-group">
    <div class="col-sm-2 text-form-label">
        Send Notification?
    </div>
    <div class="col-sm-10">
        <label class="radio-inline">
            <g:radio value="false" name="notified"
                     checked="${!(notifications || params.notified=='true')}"
                     id="notifiedFalse"/>
            No
        </label>

        <label class="radio-inline">
            <g:radio name="notified" value="true"
                     checked="${notifications || params.notified == 'true'}"
                     id="notifiedTrue"/>
            Yes
        </label>

        <g:javascript>
            <wdgt:eventHandlerJS for="notifiedTrue" state="unempty">
                <wdgt:action visible="true" targetSelector=".notifyFields.form-group"/>
            </wdgt:eventHandlerJS>
            <wdgt:eventHandlerJS for="notifiedFalse" state="unempty">
                <wdgt:action visible="false" targetSelector=".notifyFields.form-group"/>
            </wdgt:eventHandlerJS>
        </g:javascript>
    </div>
</div>
<g:render template="/scheduledExecution/editNotificationsTriggerForm"
    model="${[
            isVisible:( notifications ),
            trigger:'success',
            isEmail:isSuccess,
            isUrl:isSuccessUrl,
            definedNotifications: scheduledExecution.notifications?.findAll{it.eventTrigger=='onsuccess'},
            adminauth: adminauth
    ]}"
    />
<g:render template="/scheduledExecution/editNotificationsTriggerForm"
          model="${[
                  isVisible: (notifications),
                  trigger: 'failure',
                  isEmail: isFailure,
                  isUrl: isFailureUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == 'onfailure' },
                  adminauth: adminauth
          ]}"/>

<g:render template="/scheduledExecution/editNotificationsTriggerForm"
          model="${[
                  isVisible: (notifications),
                  trigger: 'start',
                  isEmail: defStart,
                  isUrl: defStartUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == 'onstart' },
                  adminauth: adminauth
          ]}"/>
