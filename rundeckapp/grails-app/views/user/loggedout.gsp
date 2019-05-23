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
    <asset:stylesheet href="app.css"/>
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
            <g:set var="userDefinedInstanceName" value="${grailsApplication.config.rundeck?.gui?.instanceName}"/>
            <g:if test="${userDefinedInstanceName}">
              <div class="col-md-12" style="text-align:center;margin-bottom:3em;">
                  <span class="label label-white" style="padding:.8em;font-size: 20px; border-radius:3px;    box-shadow: 0 6px 10px -4px rgba(0, 0, 0, 0.15);">
                      ${enc(sanitize:userDefinedInstanceName)}
                  </span>
              </div>
            </g:if>
            <div class="col-md-4 col-sm-6 col-md-offset-4 col-sm-offset-3">
              <div class="card">
                <div class="card-header">
                  <h4 class="card-title">
                    <div class="logo">
                        <a href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}" title="Home">
                          <img src="${resource(dir: 'images', file: 'rundeck-full-logo-black.png')}" alt="Rundeck" style="height: 20px; width: auto;"/>
                        </a>

                        <g:set var="userDefinedLogo" value="${grailsApplication.config.rundeck?.gui?.logo}"/>
                        <g:if test="${userDefinedLogo}">
                          <g:set var="safeUserLogo" value="${userDefinedLogo.toString().encodeAsSanitizedHTML()}" />
                          <g:set var="userAssetBase" value="/user-assets" />
                          <div style="margin-top:2em">
                            <img src="${g.createLink(uri:userAssetBase+"/"+safeUserLogo)}">
                          </div>
                        </g:if>
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
