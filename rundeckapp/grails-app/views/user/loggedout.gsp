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

    <g:appTitle/> - Logged Out</title>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache">
    <META HTTP-EQUIV="Expires" CONTENT="-1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="SHORTCUT" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="favicon" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="shortcut icon" href="${g.resource(dir: 'images', file: 'favicon.ico')}"/>
    <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <asset:stylesheet href="bootstrap.min.css"/>
    <asset:stylesheet href="app.scss.css"/>
    <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
    <asset:javascript src="jquery.js"/>
    <asset:javascript src="versionIdentity.js"/>
    <g:render template="/common/css"/>
</head>
<body id="loginpage">
  <div class="wrapper wrapper-full-page">
    <div class="full-page login-page">
    <!-- <div class="full-page login-page" data-color="" data-image="static/img/background/background-2.jpg"> -->
      <div class="content">
        <div class="container">
          <div class="row">
            <div class="col-md-4 col-sm-6 col-md-offset-4 col-sm-offset-3">
              <div class="card">
                <div class="card-header">
                  <h4 class="card-title">
                    <div class="logo">
                        <a href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}"
                           title="Home">
                            <g:set var="appTitle"
                                   value="${grailsApplication.config.rundeck?.gui?.title ?: g.message(code: 'main.app.name',default:'')}"/>
                            <g:set var="appDefaultTitle" value="${g.message(code: 'main.app.default.name',default:'')}"/>
                            <g:set var="brandHtml"
                                   value="${grailsApplication.config.rundeck?.gui?.brand?.html ?: g.message(code: 'main.app.brand.html',default:'')}"/>
                            <g:set var="brandDefaultHtml"
                                   value="${g.message(code: 'main.app.brand.default.html',default:'')}"/>
                            <i class="rdicon app-logo"></i>
                            <g:if test="${brandHtml}">
                                ${enc(sanitize:brandHtml)}
                            </g:if>
                            <g:elseif test="${appTitle}">
                                ${appTitle}
                            </g:elseif>
                            <g:elseif test="${brandDefaultHtml}">
                                ${enc(sanitize:brandDefaultHtml)}
                            </g:elseif>
                            <g:else>
                                ${appDefaultTitle}
                            </g:else>
                        </a>
                    </div>
                    <p class="text-center h4">
                      <g:message code="you.are.now.logged.out"/>
                    </p>
                  </h4>
                </div>
                <div class="card-content">
                  <p class="text-center">
                    <g:link controller="menu" action="home" class="btn btn-default btn-large">
                      <g:message code="login.again" />
                    </g:link>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
    </div>
    <g:render template="/common/footer"/>
  </div>
</div>
</body>
</html>
