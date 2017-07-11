%{--
  - Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
  Date: 7/7/17
  Time: 9:01 AM
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code:'list.acls')}"/>
    <title><g:message code="gui.menu.AccessControl"/></title>
</head>

<body>
<div class="row">

    <div class="col-sm-10 col-sm-offset-1">
        <div class="panel panel-default">
            <div class="panel-heading">
                <span class="panel-title">
                    <g:message code="project.access.control.title" args="${[params.project]}"/>
                </span>
            </div>

            <div class="panel-body">

                <p class="text-info">
                    <g:message code="project.access.control.description"/>
                </p>

                <div>
                    <span class="text-info">${acllist.size()}</span>
                    <g:message code="project.access.control.prompt" args="${[params.project]}"/>:
                    <ul>
                        <g:each in="${acllist}" var="file">
                            <g:render template="/menu/aclValidationListItem" model="${[
                                    policyFile: file,
                                    validation: [valid: true]
                            ]}"/>

                        </g:each>
                    </ul>
                </div>

            </div>
        </div>
    </div>
</div>
</body>
</html>
