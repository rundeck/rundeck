<!DOCTYPE html>
<!--[if lt IE 7 ]> <html class="ie6"> <![endif]-->
<!--[if IE 7 ]>    <html class="ie7"> <![endif]-->
<!--[if IE 8 ]>    <html class="ie8"> <![endif]-->
<!--[if IE 9 ]>    <html class="ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en"><!--<![endif]-->
<head>
    <title>%{--
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

<g:appTitle/> - Login</title>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache">
    <META HTTP-EQUIV="Expires" CONTENT="-1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="SHORTCUT" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="favicon" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="shortcut icon" href="${g.resource(dir: 'images', file: 'favicon.ico')}"/>
    <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <asset:stylesheet href="rundeck.css"/>
    <asset:stylesheet href="non_responsive.css"/>
    <!--[if lt IE 9]>
    <g:javascript library="respond.min"/>
    <![endif]-->
    <asset:javascript src="jquery.js"/>
    <asset:javascript src="prototype.min.js"/>
    <asset:javascript src="versionIdentity.js"/>
    <g:render template="/common/css"/>
    <script language="javascript">
        //<!--
        Event.observe(window, 'load', loadFocus, false);

        function loadFocus() {
            $('login').focus();
        }
        if (typeof(oopsEmbeddedLogin) == 'function') {
            oopsEmbeddedLogin();
        }
        //-->
    </script>
</head>
<body id="loginpage">

<g:render template="/common/topbar"/>
<div class="container">

<div class="col-sm-4 col-sm-push-4">
    <div class="panel panel-primary ">
        <div class="panel-body">
        <form action="j_security_check" method="post" class="form " role="form">

            <g:set var="loginhtml" value="${grailsApplication.config.rundeck?.gui?.login?.welcomeHtml ?: ''}"/>
            <g:if test="${loginhtml}">
            <div class="row">
                <span class="col-sm-12">
                    ${enc(sanitize:loginhtml)}
                </span>
            </div>
            </g:if>
            <g:set var="loginmsg"
                   value="${grailsApplication.config.rundeck?.gui?.login?.welcome ?: g.message(code: 'gui.login.welcome', default: '')}"/>
            <g:if test="${loginmsg}">
            <div class="row">
                <span class="col-sm-12">
                    <h3 class="text-muted">
                        <g:enc>${loginmsg}</g:enc>
                    </h3>
                </span>
            </div>
            </g:if>
            <div class="form-group">
                <label for="login">Username</label>
                <input type="text" name="j_username" id="login" class="form-control" autofocus="true"/>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" name="j_password" id="password" class="form-control"/>
            </div>

            <div class="form-group">
                <button type="submit" class="btn btn-primary">Login</button>
            </div>
        </form>
        </div>
        <g:if test="${flash.loginerror}">
            <div class="panel-footer panel-danger">
                <g:enc>${flash.loginerror}</g:enc>
            </div>
        </g:if>
    </div>
</div>
</div>
<div class="container footer">
    <g:render template="/common/footer"/>
</div>
</body>
</html>
