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
<g:set var="pluginInstall" value="${auth.resourceAllowedTest(
        type: 'resource',
        kind: 'plugin',
        action: ["install"],
        context: 'application'
)}"/>
<script type="text/javascript">
    window.repocaninstall = ${pluginInstall}
</script>
<g:set var="repoEnabled" value="${grailsApplication.config.rundeck?.features?.repository?.enabled}"/>
<g:if test="${repoEnabled == 'true'}">
<div class="container-fluid">
    <div id="repository-artifact-content">
        <div id=repository-vue></div>
    </div>
</div>
<asset:javascript src="static/manifest.js"/>
<asset:javascript src="static/vendor.js"/>
<asset:javascript src="static/pages/repository.js"/>
</g:if>
<g:else>
    <h3>This feature is not enabled</h3>
</g:else>
</body>
</html>
