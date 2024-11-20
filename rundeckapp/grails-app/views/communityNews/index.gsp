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
    <title><g:appTitle/></title>
    <!-- VUE CSS MODULES -->
    <g:if test="${!grailsApplication.config.getProperty("rundeck.spa.vite.enabled", Boolean.class,false)}">
    <asset:stylesheet href="static/css/pages/community-news.css"/>
    </g:if>
    <!-- /VUE CSS MODULES -->
</head>

<body>
<div class="content">
<div id="layoutBody">
  <div id="community-news-vue"></div>
  <!-- VUE JS MODULES -->
  <g:if test="${grailsApplication.config.getProperty("rundeck.spa.vite.enabled", Boolean.class,false)}">
    <g:loadEntryAssets entry="pages/community-news" />
  </g:if>
  <g:else>
  <asset:javascript src="static/pages/community-news.js"/>
  </g:else>
  <!-- /VUE JS MODULES -->
</div>
</div>
</body>
</html>
