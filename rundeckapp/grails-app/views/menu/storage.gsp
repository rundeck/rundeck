%{-- - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com) - - Licensed under the Apache License, Version 2.0 (the "License"); - you may not use this file except in compliance with the License. - You may obtain a copy of the License at - -
http://www.apache.org/licenses/LICENSE-2.0 - - Unless required by applicable law or agreed to in writing, software - distributed under the License is distributed on an "AS IS" BASIS, - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. - See the License for the specific language governing permissions and - limitations under the License. --}%

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 9/29/14
  Time: 3:04 PM
  To change this template use File | Settings | File Templates.
--%>

  <%@ page contentType="text/html;charset=UTF-8" %>
    <html xmlns="http://www.w3.org/1999/html">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="base"/>
        <meta name="tabpage" content="configure"/>
        <meta name="tabtitle" content="${g.message(code: 'gui.menu.KeyStorage')}"/>
        <title><g:message code="gui.menu.KeyStorage"/></title>
        <g:set var="downloadenabled" value="${cfg.getBoolean(config: "gui.keystorage.downloadenabled", default: true)}"/>

        <g:embedJSON id="storageData" data="[
                resourcePath:params.resourcePath,
                project:params.project,
                downloadenabled: downloadenabled

        ]"/>
        <asset:javascript src="static/components/storage.js"/>
        <asset:javascript src="static/pages/project-config.js" defer="defer" />
        <asset:stylesheet href="static/css/pages/project-config.css" />
      </head>

      <body>
        <div class="content">
          <div id="layoutBody">
            <div class="title">
              <span class="text-h3"><i class="fas fa-key"></i> ${g.message(code:"gui.menu.KeyStorage")}</span>
            </div>
            <div id="keyStorageView">
              <key-storage-view :read-only="true" :allow-upload="true"></key-storage-view>
            </div>
          </div>
        </div>
      </body>
