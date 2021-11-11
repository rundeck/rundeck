<%@ page import="org.rundeck.core.auth.AuthConstants" %>
%{--
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
    <meta name="tabpage" content="projectHome"/>
    <title><g:appTitle/></title>
    <asset:stylesheet src="static/css/pages/repository.css"/>
</head>

<body>
<div class="content">
<div id="layoutBody">
<g:set var="pluginInstall" value="${auth.resourceAllowedTest(
        type: AuthConstants.TYPE_RESOURCE,
        kind: AuthConstants.TYPE_PLUGIN,
        action: [AuthConstants.ACTION_INSTALL, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN],
        any: true,
        context: AuthConstants.CTX_APPLICATION
)}"/>
<script type="text/javascript">
    window.repocaninstall = ${pluginInstall ?: false};
</script>

<g:set var="repoEnabled" value="${grailsApplication.config.getProperty("rundeck.feature.repository.enabled", String.class) in [true,"true"]}"/>
<script type="text/javascript">
  window.repoEnabled = ${repoEnabled ?: false};
</script>

<g:set var="localSearchOnly" value="${grailsApplication.config.getProperty("rundeck.feature.repository.localSearchOnly", String.class)}"/>
<script type="text/javascript">
  window.repositoryLocalSearchOnly = ${localSearchOnly ?: false};
</script>

<div class="container-fluid">
  <div id=repository-vue></div>
</div>
<asset:javascript src="static/pages/repository.js"/>
</div>
</div>
</body>
</html>
