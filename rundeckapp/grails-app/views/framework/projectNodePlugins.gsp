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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 2019-02-22
  Time: 15:12
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code: 'Node.plural')}"/>

    <!-- VUE JS REQUIREMENTS -->
    <asset:javascript src="static/manifest.js"/>
    <asset:javascript src="static/vendor.js"/>
    <!-- /VUE JS REQUIREMENTS -->

    <title><g:message code="Node.plural"/></title>

</head>


<body>
<div class="container-fluid">
    <div class="row">
        <div class="col-xs-12">
            <g:render template="/common/messages"/>
        </div>
    </div>

    <div class="row">
        <div class="col-xs-12">
            <div class="card" id="createform">
                <div class="card-header">
                    <h3 class="card-title">
                        <g:message code="Node.plural" default="Nodes"/>
                        <g:message code="page.Plugins.title" />

                        : <g:enc>${params.project ?: request.project}</g:enc>
                    </h3>
                </div>

                <div class="card-content">

                    <div id="project-nodes-config-vue"></div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- VUE JS MODULES -->
<asset:javascript src="static/pages/project-nodes-config.js"/>
<!-- /VUE JS MODULES -->
</body>
</html>
