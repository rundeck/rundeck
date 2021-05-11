<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="skipPrototypeJs" content="true"/>

    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title>%{--
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

<g:appTitle/> - <g:message
            code="ScheduledExecution.page.create.title"/></title>
    <asset:javascript src="jquery.autocomplete.min.js"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="jobEditPage_bundle.js"/>
    <asset:javascript src="util/markdeep.js"/>
    <asset:javascript src="util/yellowfade.js"/>
    <asset:javascript src="util/tab-router.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <asset:javascript src="static/pages/job/editor.js" defer="defer"/>
    <asset:stylesheet src="static/css/pages/job/editor.css" />
    <g:jsMessages code="
    yes,
    no,
    scheduledExecution.property.notified.label.text,
    scheduledExecution.property.notifyAvgDurationThreshold.label,
    scheduledExecution.property.notifyAvgDurationThreshold.description,
    to,
    subject,
    notification.email.description,
    notification.email.subject.description,
    notification.email.subject.helpLink,
    attach.output.log,
    attach.output.log.asFile,
    attach.output.log.inline,
    notification.webhook.field.title,
    notification.webhook.field.description,
    notify.url.format.label,
    notify.url.format.xml,
    notify.url.format.json,
"/>
    <g:jsMessages codes="${[
            'onsuccess',
            'onfailure',
            'onstart',
            'onavgduration',
            'onretryablefailure'
    ].collect{'notification.event.'+it}}"/>

    <g:embedJSON id="jobNotificationsJSON"
                 data="${ [notifications:scheduledExecution.notifications?.collect{it.toNormalizedMap()}?:[],
                           notifyAvgDurationThreshold:scheduledExecution?.notifyAvgDurationThreshold,
                 ]}"/>

    <g:javascript>
        window._rundeck = Object.assign(window._rundeck || {}, {
            data: {notificationData: loadJsonData('jobNotificationsJSON')}
        })
        console.log("loaded data",window._rundeck)
        var workflowEditor = new WorkflowEditor();
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        _onJobEdit(confirm.setNeedsConfirm);
        jQuery(function () {
            setupTabRouter('#job_edit_tabs', 'tab_');
            jQuery('input').not(".allowenter").on('keydown', noenter);
        })
    </g:javascript>
    <g:embedJSON data="${globalVars ?: []}" id="globalVarData"/>
    <g:embedJSON data="${timeZones ?: []}" id="timeZonesData"/>
</head>
<body>
<div class="content">
<div id="layoutBody">
    <g:render template="/scheduledExecution/createForm" model="[scheduledExecution:scheduledExecution,crontab:crontab,iscopy:iscopy,authorized:authorized]"/>
</div>
</div>
</body>
</html>
