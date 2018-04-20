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
  Date: 9/13/17
  Time: 3:12 PM
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <meta name="tabtitle" content="${message(code: 'edit.system.acl.file')}"/>
    <title><g:message code="edit.system.acl.file"/></title>

    <g:javascript library="prototype/effects"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

        function init() {
            $$('input').each(function (elem) {
                if (elem.type == 'text') {
                    elem.observe('keypress', noenter);
                }
            });
            var confirm = new PageConfirm(message('page.unsaved.changes'));
            jQuery('.apply_ace').each(function () {
                _setupAceTextareaEditor(this, confirm.setNeetsConfirm);
            });
        }
        jQuery(init);
    </g:javascript>
</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>

<div class="row">
    <g:form action="saveSystemAclFile" method="post"
            params="${[project: params.project, id: id, fileType: fileType]}"
            useToken="true"
            class="form-horizontal">
        <div class="col-sm-10 col-sm-offset-1">
            <g:render template="editAclFile" model="${[
                    backHref                : g.createLink(controller:'menu',action:'acls'),
                    title                   : g.message(code: 'edit.system.acl.file'),
                    primaryLabel            : g.message(code: 'policy.name.label.prompt'),
                    primaryValue            : name,
                    secondaryLabel          : g.message(code: 'system.acl.location.prompt'),
                    secondaryValue          : g.message(code: 'system.acl.location.type.' + fileType + '.label'),
                    fileText                : fileText,
                    validationDocumentPrefix: 'acls/' + id,
                    input                   : input
            ]}"/>
        </div>
    </g:form>
</div>

<!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
</body>
</html>