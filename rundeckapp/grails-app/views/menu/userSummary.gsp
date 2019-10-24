    MenuControllerSpec.groovy    %{--
     - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
 <meta name="tabpage" content="configure"/>
 <meta name="tabtitle" content="${g.message(code:'page.users.title')}"/>
 <title><g:message code="page.users.title"/></title>
</head>

<body>

<div id=user-summary-vue></div>

<asset:javascript src="static/pages/user-summary.js"/>
<script type="text/javascript">
</script>
</body>
</html>