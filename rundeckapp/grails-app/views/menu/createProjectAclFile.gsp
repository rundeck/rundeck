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
  Date: 9/14/17
  Time: 11:49 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<g:set var="uiType" value="${params.nextUi ? 'next' : 'current'}"/>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code: 'create.project.acl.file')}"/>
    <title><g:message code="create.project.acl.file"/></title>

    <g:if test="${uiType!='next'}">
        <asset:javascript src="prototype/effects"/>
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
    </g:if>
    <g:if test="${uiType=='next'}">
        <g:set var="aclFieldErrors" value="${['name', 'id', 'fileText'].collectEntries { field ->
            [(field): input?.errors?.getFieldErrors(field)?.collect { error -> g.message(error: error) } ?: []]
        }}"/>
        <g:set var="aclCreateData" value="${[
            project     : project ?: params.project,
            name        : name,
            fileText    : fileText ?: '',
            create      : true,
            fieldErrors : aclFieldErrors,
            validation  : validation ? [valid: validation.valid, errors: validation.errors] : null,
            conflict    : response.status == 409,
            saveUrl     : g.createLink(
                controller: 'menu',
                action: 'saveProjectAclFile',
                params: [project: project ?: params.project, nextUi: params.nextUi]
            ),
            cancelUrl   : g.createLink(controller: 'menu', action: 'projectAcls', params: [project: project ?: params.project]),
            backUrl     : g.createLink(controller: 'menu', action: 'projectAcls', params: [project: project ?: params.project]),
            helpLinks   : [
                accessControlPolicy: 'https://docs.rundeck.com/docs/administration/security/authorization.html',
                aclPolicyFormat    : 'https://docs.rundeck.com/docs/administration/security/authorization.html#example'
            ]
        ]}"/>
        <g:embedJSON id="aclCreatePageData" data="${aclCreateData}"/>
        <g:jsonToken id="aclCreateCsrf" url="${request.forwardURI}"/>
        <g:javascript>
            window._rundeck = Object.assign(window._rundeck || {}, {
                data: Object.assign((window._rundeck || {}).data || {}, {
                    aclCreatePageData: Object.assign(
                        JSON.parse(document.getElementById('aclCreatePageData').textContent),
                        {csrf: JSON.parse(document.getElementById('aclCreateCsrf').textContent)}
                    )
                })
            });
        </g:javascript>
        <asset:stylesheet src="static/css/pages/menu-create-project-acl-file.css"/>
        <asset:javascript src="static/pages/menu-create-project-acl-file.js" defer="defer"/>
    </g:if>
</head>

<body>
<g:if test="${uiType!='next'}">
    <div class="content">
    <div id="layoutBody">
    <div class="container-fluid">
        <div class="row">
            <div class="col-xs-12">
                <g:render template="/common/messages"/>
            </div>
        </div>

        <div class="row">
            <g:form action="saveProjectAclFile" method="post"
                    params="${[project: params.project]}"
                    useToken="true"
                    class="form-horizontal">
                <div class="col-xs-12">
                    <g:render template="editAclFile" model="${[
                        backHref                : g.createLink(
                            controller: 'menu',
                            action: 'projectAcls',
                            params: [project: project]
                        ),
                        title                   : g.message(code: 'create.project.acl.file'),
                        primaryLabel            : g.message(code: 'project.label.prompt'),
                        primaryValue            : project,
                        fileText                : fileText,
                        validationDocumentPrefix: 'acls/' + id,
                        createField             : 'name',
                        createFieldLabel        : g.message(code: 'policy.name.label.prompt'),
                        createFieldValue        : name,
                        createFieldHelpCode     : 'policy.name.description',
                        input                   : input
                    ]}"/>
                </div>
            </g:form>
        </div>
    </div>
        </div>
    </div>
    <!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
</g:if>
<g:if test="${uiType=='next'}">
    <div class="content">
        <div id="layoutBodyVue">
            <div class="container-fluid">
                <div class="row">
                    <div class="col-xs-12">
                        <g:render template="/common/messages"/>
                    </div>
                </div>
            </div>
            <div id="menu-create-project-acl-file"></div>
        </div>
    </div>
</g:if>
</body>
</html>
