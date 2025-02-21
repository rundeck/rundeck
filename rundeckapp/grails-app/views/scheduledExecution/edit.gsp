<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="skipPrototypeJs" content="true"/>

    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <asset:javascript src="static/pages/dynamic-form.js" defer="defer"/>
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
    <tmpl:jobEditCommon/>
</head>
<body>
<div class="content">
<div id="layoutBody">
    <tmpl:editForm model="[scheduledExecution:scheduledExecution,crontab:crontab,authorized:authorized, notificationPlugins: notificationPlugins, sessionOpts: sessionOpts]"/>
</div>
</div>
</body>
</html>
