<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
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
            code="ScheduledExecution.page.edit.title"/></title>
    <asset:javascript src="jquery.autocomplete.min.js"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="jobEditPage_bundle.js"/>
    <asset:javascript src="util/markdeep.js"/>
    <asset:javascript src="util/yellowfade.js"/>
    <asset:javascript src="util/tab-router.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>
        var workflowEditor = new WorkflowEditor();
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        _onJobEdit(confirm.setNeedsConfirm);
        jQuery(function () {
            setupTabRouter('#job_edit_tabs', 'tab_');
        })
    </g:javascript>
    <g:embedJSON data="${globalVars ?: []}" id="globalVarData"/>
    <g:embedJSON data="${timeZones ?: []}" id="timeZonesData"/>
</head>
<body>


    <tmpl:editForm model="[scheduledExecution:scheduledExecution,crontab:crontab,authorized:authorized, notificationPlugins: notificationPlugins]"/>
</body>
</html>
