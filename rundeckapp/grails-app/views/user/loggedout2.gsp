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
  Date: 3/12/14
  Time: 11:36 AM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head><title><g:appTitle/> - Logged Out</title>
    <meta name="layout" content="base"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="SHORTCUT" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="favicon" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="shortcut icon" href="${g.resource(dir: 'images', file: 'favicon.ico')}"/>
    <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <asset:stylesheet href="bootstrap.min.css"/>
    <asset:stylesheet href="app.scss.css"/>
</head>

<body>
<div class="container-fluid">
  <div class="row">
    <div class="col-sm-12">
      <div class="card">
        <div class="card-header">
          <h4 class="card-title">
            <g:message code="you.are.now.logged.out"/>
          </h4>
        </div>
        <div class="card-content">
          <p>
            <g:link controller="menu" action="home" class="btn btn-default btn-large">
              <g:message code="login.again" />
            </g:link>
          </p>
        </div>
      </div>
    </div>
  </div>
</div>
</body>
</html>
