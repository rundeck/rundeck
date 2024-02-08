%{--
- Copyright 2018 Rundeck, Inc. (http://rundeck.com)
-
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
- You may obtain a copy of the License at
-
- http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an "AS IS" BASIS,
- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
- See the License for the specific language governing permissions and
- limitations under the License.
--}%

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 9/6/17
  Time: 9:39 AM
--%>

<%@ page import="com.dtolabs.rundeck.core.common.FrameworkProject" contentType="text/html;charset=UTF-8" %>
<html>

<head>
  <g:set var="rkey" value="${g.rkey()}" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="layout" content="base" />
  <meta name="tabpage" content="projectconfigure" />
  <meta name="projtabtitle" content="${message(code: 'edit.nodes.title')}" />
  <meta name="projconfigselected" content="edit-nodes" />
  <meta name="skipPrototypeJs" content="true" />

  <title>
    <g:message code="edit.nodes.title" />: <g:enc>${params.project ?: request.project}</g:enc>
  </title>

  <asset:javascript src="util/tab-router.js"/>
  <g:javascript>
    jQuery(function () {
      setupTabRouter('#node_config_tabs', 'node_');
    })
    window._rundeck = Object.assign(window._rundeck || {}, {
            data: {
                projectAclConfigPageUrl:"${enc(js:createLink(controller:'menu',action:'createProjectAclFile',params:[project:params.project?:project]))}",
                systemAclConfigPageUrl:"${enc(js:createLink(controller:'menu',action:'createSystemAclFile'))}",

            }
        });
  </g:javascript>
  <!-- VUE JS MODULES -->
  <asset:javascript src="static/pages/project-nodes-config.js" defer="defer" />
  <asset:stylesheet href="static/css/pages/project-nodes-config.css" />
  <!-- /VUE JS MODULES -->
</head>

<body>
<div class="project-plugin-config-vue">
<project-node-sources>

</project-node-sources>
</div>
</body>

</html>
