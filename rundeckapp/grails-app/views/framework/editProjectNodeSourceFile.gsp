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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 9/7/17
  Time: 10:36 AM
--%>

<%@ page import="org.rundeck.core.auth.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="skipPrototypeJs" content="true"/>

    <meta name="projtabtitle" content="${message(code: 'edit.nodes.file')}"/>
    <title><g:message code="edit.nodes.file"/></title>

    <asset:javascript src="leavePageConfirm.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <asset:stylesheet href="static/css/pages/project-nodes-editor.css"/>
    <asset:javascript src="static/pages/project-nodes-editor.js" defer="defer"/>
    <g:embedJSON data="${[index:index,nextPageUrl:g.createLink(controller: 'framework',
                                                           action: 'projectNodeSources',
                                                           params: [project: project],absolute:true)]}"
                 id="editProjectNodeSourceData"></g:embedJSON>
    <g:set var="legacyUi" value="${params.legacyUi || feature.isEnabled(name:'legacyUi')}"/>
    <g:javascript>

        function init() {
            <g:if test="${legacyUi}">
            jQuery('input').each(function (elem) {
                if (elem.type == 'text') {
                    elem.observe('keypress', noenter);
                }
            });
            var confirm = new PageConfirm(message('page.unsaved.changes'));
            jQuery('.apply_ace').each(function () {
                _setupAceTextareaEditor(this, confirm.setNeetsConfirm);
            });
            </g:if>
            window._rundeck.data = Object.assign(window._rundeck.data || {}, {
                "editProjectNodeSourceData": loadJsonData('editProjectNodeSourceData')
            })
        }
        jQuery(init);
    </g:javascript>
</head>

<body>
<div class="content">
<div id="layoutBody">
    <div class="container-fluid">
  <div class="row">
      <div class="col-sm-12">
          <g:render template="/common/messages"/>
      </div>
  </div>

  <div class="row">
    <div class="col-xs-12">
      <div class="card">
          <g:if test="${legacyUi}">
            <tmpl:legacyEditProjectNodeSourceFile/>
          </g:if>
          <g:else>
            <div class="vue-ui-socket">
              <ui-socket section="edit-project-node-source-file" location="main"/>
            </div>
          </g:else>
      </div>
    </div>

  </div>

</div>
</div>
</div>
<g:if test="${legacyUi}">
<!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
</g:if>
</body>
</html>
