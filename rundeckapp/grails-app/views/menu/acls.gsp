%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
  Date: 10/28/13
  Time: 12:06 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="gui.menu.AccessControl"/></title>
</head>
<body>
<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'securityConfig']"/>
    </div>

    <div class="col-sm-9">
        <h4><g:message code="system.access.control.title"/></h4>

        <div class="text-muted">
            <g:message code="to.modify.access.control.rules.create.or.edit.a.aclpolicy.file.in.the.rundeck.etc.directory" />
        </div>
        <div>
            <g:message code="list.of.acl.policy.files.in.directory" /> <code>${fwkConfigDir.absolutePath}</code>:
            <ul>
            <g:each in="${aclFileList}" var="file">
                <g:render template="/menu/aclValidationListItem" model="${[
                        policyFile: file.name,
                        validation: validations[file]
                ]}"/>

            </g:each>
            </ul>
        </div>
        <g:if test="${params.project}">
            <h4><g:message code="project.access.control.title" args="${[params.project]}"/></h4>

            <div class="text-muted">
                <g:message code="project.access.control.description"/>
            </div>

            <div>
                <span class="text-info">${projectlist.size()}</span>
                <g:message code="project.access.control.prompt" args="${[params.project]}"/>:
                <ul>
                    <g:each in="${projectlist}" var="file">
                        <g:render template="/menu/aclValidationListItem" model="${[
                                policyFile: file,
                                validation: [valid: true]
                        ]}"/>

                    </g:each>
                </ul>
            </div>

        </g:if>
    </div>
</div>
</body>
</html>
