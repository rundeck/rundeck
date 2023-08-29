<%@ page import="grails.util.Environment" %>
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
    <link rel="shortcut icon" href="${g.resource(dir: 'images', file: g.appFavicon())}"/>
    <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>

    <g:if test="${Environment.isDevelopmentEnvironmentAvailable()}">
        <asset:javascript src="vendor/vue.global.js"/>
    </g:if>
    <g:else>
        <asset:javascript src="vendor/vue.global.prod.js"/>
    </g:else>
    %{-- Core theme styles from ui-trellis --}%
    <g:loadEntryAssets entry="components/theme"/>
    <g:loadEntryAssets entry="components/server-identity" />
    <g:loadEntryAssets entry="pages/login" />

    <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
    <asset:javascript src="vendor/jquery.js"/>
    <g:render template="/common/css"/>
</head>
<body id="loginpage">
    <div class="login-page">
      <div class="content">
        <div class="container">
          <div class="row">
            <g:set var="userDefinedInstanceName" value="${cfg.getString(config: "gui.instanceName")}"/>
            <g:if test="${userDefinedInstanceName}">
              <div class="col-md-12" style="text-align:center;margin-bottom:3em;">
                  <span class="label label-default instance-label" style="padding:.8em;font-size: 20px; border-radius:3px;    box-shadow: 0 6px 10px -4px rgba(0, 0, 0, 0.15);">
                      ${enc(sanitize:userDefinedInstanceName)}
                  </span>
              </div>
            </g:if>
            <div class="col-md-4 col-sm-6 col-md-offset-4 col-sm-offset-3">
              <div class="card">
                <div class="card-header">
                  <h4 class="card-title">
                    <div class="logo">
                        <g:set var="logoImage" value="${"static/img/${g.appLogo()}"}"/>
                        <g:set var="titleLink" value="${cfg.getString(config: "gui.titleLink")}"/>
                        <a href="${titleLink ? enc(attr:titleLink) : g.createLink(uri: '/')}" title="Home">
                            <asset:image src="${logoImage}" alt="Rundeck" style="width: 200px;" onload="onSvgLoaded(this)"/>
                        </a>

                        <g:set var="userDefinedLogo" value="${cfg.getString(config: "gui.logo")}"/>
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
                    <g:link controller="menu" action="home" class="btn btn-primary btn-large">
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
<asset:deferredScripts/>
<script type="application/javascript">
    function onSvgLoaded(image) {
        if (typeof SVGInject !== 'undefined') {
            return SVGInject(image)
        }
        window.addEventListener('load', function() { SVGInject(image) })
    }
</script>
</body>
</html>
