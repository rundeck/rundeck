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
  Date: 4/18/16
  Time: 12:54 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>

    <meta name="layout" content="base"/>
    <meta name="tabpage" content="home"/>
    <title><g:appTitle/><g:message code="page.welcome.title.suffix" /></title>
</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <div class="jumbotron">
            <h2>
                <g:message code="app.firstRun.title"
                           args="${[g.appTitle(), grailsApplication.metadata['build.ident']]}"/>
            </h2>

            <g:markdown><g:autoLink>${message(code: "app.firstRun.md")}</g:autoLink></g:markdown>

            <g:link controller="menu" action="index" class="btn btn-lg btn-success">
                <g:message code="welcome.button.use.rundeck" />
            </g:link>
        </div>
    </div>
</div>
</body>
</html>