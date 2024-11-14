%{--
  - Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="skipPrototypeJs" content="true"/>
    <asset:javascript src="static/pages/migwiz.js"/>
    <asset:stylesheet src="static/css/pages/migwiz.css"/>
    <title><g:message code="gui.menu.TryRBA"/></title>
</head>

<body>
<div class="content">
    <div id="layoutBody" class="vue-ui-socket">
        <ui-socket section="migwiz" location="main"/>
    </div>
</div>

</body>
</html>
