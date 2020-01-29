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

<%@ page import="rundeck.controllers.ScheduledExecutionController; com.dtolabs.rundeck.plugins.ServiceNameConstants" %>
<g:set var="notifications" value="${scheduledExecution.notifications}"/>
<g:set var="defSuccess" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE)}"/>
<g:set var="isSuccess" value="${'true' == params[ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL] || null== params[ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL] && defSuccess}"/>
<g:set var="defSuccessUrl" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE)}"/>
<g:set var="isSuccessUrl" value="${('true' == params[ScheduledExecutionController.NOTIFY_ONSUCCESS_URL]) || (null==params[ScheduledExecutionController.NOTIFY_ONSUCCESS_URL] && defSuccessUrl)}"/>

<g:set var="defFailure" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE)}"/>
<g:set var="isFailure" value="${'true' == params[ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL] || null == params[ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL] &&defFailure}"/>
<g:set var="defFailureUrl" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE)}"/>
<g:set var="isFailureUrl" value="${'true' == params[ScheduledExecutionController.NOTIFY_ONFAILURE_URL] || null == params[ScheduledExecutionController.NOTIFY_ONFAILURE_URL] &&defFailureUrl}"/>

<g:set var="defStart" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONSTART_TRIGGER_NAME, ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE)}"/>
<g:set var="isStart"
       value="${'true' == params[ScheduledExecutionController.NOTIFY_ONSTART_EMAIL] || null == params[ScheduledExecutionController.NOTIFY_ONSTART_EMAIL] && defStart}"/>
<g:set var="defStartUrl" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONSTART_TRIGGER_NAME, ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE)}"/>
<g:set var="isStartUrl"
       value="${'true' == params[ScheduledExecutionController.NOTIFY_ONSTART_URL] || null == params[ScheduledExecutionController.NOTIFY_ONSTART_URL] && defStartUrl}"/>
<g:set var="defAvg" value="${scheduledExecution.findNotification(ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME, ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE)}"/>
<g:set var="isAvg"
       value="${'true' == params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL] || null == params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL] && defAvg}"/>
<g:set var="defAvgUrl" value="${scheduledExecution.findNotification(ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME, ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE)}"/>
<g:set var="isAvgUrl"
       value="${'true' == params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL] || null == params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL] && defAvgUrl}"/>

<g:set var="defRetryableFailure" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME, ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE)}"/>
<g:set var="isRetryableFailure" value="${'true' == params[ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL] || null == params[ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL] &&defRetryableFailure}"/>
<g:set var="defRetryableFailureUrl" value="${scheduledExecution.findNotification(ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME, ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE)}"/>
<g:set var="isRetryableFailureUrl" value="${'true' == params[ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_URL] || null == params[ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_URL] &&defRetryableFailureUrl}"/>


<div class="form-group">
    <div class="col-sm-2 control-label text-form-label">
        <g:message code="scheduledExecution.property.notified.label.text" />
    </div>
    <div class="col-sm-10">
      <div class="radio radio-inline">
        <g:radio value="false" name="notified"
                 checked="${!(notifications || params.notified=='true')}"
                 id="notifiedFalse"/>
         <label for="notifiedFalse">
             <g:message code="no" />
         </label>
      </div>
      <div class="radio radio-inline">
        <g:radio name="notified" value="true"
                 checked="${notifications || params.notified == 'true'}"
                 id="notifiedTrue"/>
        <label for="notifiedTrue">
            <g:message code="yes" />
        </label>
      </div>
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
            isVisible:( notifications || params.notified == 'true'),
            trigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
            triggerEmailCheckboxName: ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL,
            triggerEmailRecipientsName: ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS,
            triggerEmailSubjectName: ScheduledExecutionController.NOTIFY_SUCCESS_SUBJECT,
            triggerEmailAttachName: ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH,
            triggerEmailAttachTypeName: ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH_TYPE,
            triggerUrlCheckboxName: ScheduledExecutionController.NOTIFY_ONSUCCESS_URL,
            triggerUrlFieldName: ScheduledExecutionController.NOTIFY_SUCCESS_URL,
            isEmail:isSuccess,
            isUrl:isSuccessUrl,
            defEmail:defSuccess,
            defUrl:defSuccessUrl,
            definedNotifications: scheduledExecution.notifications?.findAll{it.eventTrigger== ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME},
            adminauth: adminauth,
            serviceName: ServiceNameConstants.Notification
    ]}"
    />
<g:render template="/scheduledExecution/editNotificationsTriggerForm"
          model="${[
                  isVisible: (notifications|| params.notified == 'true'),
                  trigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                  triggerEmailCheckboxName: ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL,
                  triggerEmailRecipientsName: ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS,
                  triggerEmailSubjectName: ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT,
                  triggerEmailAttachName: ScheduledExecutionController.NOTIFY_FAILURE_ATTACH,
                  triggerEmailAttachTypeName: ScheduledExecutionController.NOTIFY_FAILURE_ATTACH_TYPE,
                  triggerUrlCheckboxName: ScheduledExecutionController.NOTIFY_ONFAILURE_URL,
                  triggerUrlFieldName: ScheduledExecutionController.NOTIFY_FAILURE_URL,
                  isEmail: isFailure,
                  isUrl: isFailureUrl,
                  defEmail: defFailure,
                  defUrl: defFailureUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == ScheduledExecutionController.ONFAILURE_TRIGGER_NAME },
                  adminauth: adminauth,
                  serviceName: ServiceNameConstants.Notification
          ]}"/>

<g:render template="/scheduledExecution/editNotificationsTriggerForm"
          model="${[
                  isVisible: (notifications|| params.notified == 'true'),
                  trigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME,
                  triggerEmailCheckboxName: ScheduledExecutionController.NOTIFY_ONSTART_EMAIL,
                  triggerEmailRecipientsName: ScheduledExecutionController.NOTIFY_START_RECIPIENTS,
                  triggerEmailSubjectName: ScheduledExecutionController.NOTIFY_START_SUBJECT,
                  triggerUrlCheckboxName: ScheduledExecutionController.NOTIFY_ONSTART_URL,
                  triggerUrlFieldName: ScheduledExecutionController.NOTIFY_START_URL,
                  isEmail: isStart,
                  isUrl: isStartUrl,
                  defEmail: defStart,
                  defUrl: defStartUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == ScheduledExecutionController.ONSTART_TRIGGER_NAME },
                  adminauth: adminauth,
                  serviceName: ServiceNameConstants.Notification
          ]}"/>
<g:render template="/scheduledExecution/editNotificationsTriggerForm"
          model="${[
                  isVisible: (notifications|| params.notified == 'true'),
                  trigger: ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME,
                  triggerEmailCheckboxName: ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL,
                  triggerEmailRecipientsName: ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS,
                  triggerEmailSubjectName: ScheduledExecutionController.NOTIFY_OVERAVGDURATION_SUBJECT,
                  triggerUrlCheckboxName: ScheduledExecutionController.NOTIFY_ONOVERAVGDURATION_URL,
                  triggerUrlFieldName: ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL,
                  isEmail: isAvg,
                  isUrl: isAvgUrl,
                  defEmail: defAvg,
                  defUrl: defAvgUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME },
                  adminauth: adminauth,
                  serviceName: ServiceNameConstants.Notification
          ]}"/>

<g:render template="/scheduledExecution/editNotificationsTriggerForm"
          model="${[
                  isVisible: (notifications|| params.notified == 'true'),
                  trigger: ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME,
                  triggerEmailCheckboxName: ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL,
                  triggerEmailRecipientsName: ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS,
                  triggerEmailSubjectName: ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_SUBJECT,
                  triggerEmailAttachName: ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_ATTACH,
                  triggerEmailAttachTypeName: ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_ATTACH_TYPE,
                  triggerUrlCheckboxName: ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_URL,
                  triggerUrlFieldName: ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL,
                  isEmail: isRetryableFailure,
                  isUrl: isRetryableFailureUrl,
                  defEmail: defRetryableFailure,
                  defUrl: defRetryableFailureUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME },
                  adminauth: adminauth,
                  serviceName: ServiceNameConstants.Notification
          ]}"/>
