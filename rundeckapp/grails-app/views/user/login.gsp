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
    <asset:stylesheet href="bootstrap.min.css"/>
    <asset:stylesheet href="app.css"/>
    <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
    <asset:javascript src="jquery.js"/>
    <asset:javascript src="versionIdentity.js"/>
    <g:render template="/common/css"/>
    <script language="javascript">
        //<!--
        jQuery(function() {jQuery('#login').focus();});
        if (typeof(oopsEmbeddedLogin) == 'function') {
            oopsEmbeddedLogin();
        }
        //-->
    </script>
    <style type="text/css">
        .sso-login-container {
            display: flex;
            flex-wrap: nowrap;
            padding: 0 30px;
            align-items: center;
            justify-items: center;
        }
        .sso-login {
            margin: 10px auto;
            border-bottom: 1px solid #0f0f0f;
            text-align: center;
        }
        .sso-login-img {
            border: 2px solid #167df0;
            border-radius: 2px 0 0 2px;
            padding: 7px 6px 6px 6px;
            flex: 0 0 auto;
        }
        .sso-login-link {
            flex: auto;
            padding: 4px 10px;
            border-radius: 0 2px 2px 0;
            border: 1px solid #167df0;
            background-color: #167df0;
            color: #fff;
            vertical-align: middle;
            font-size: 1.2em;
        }
    </style>
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
              <form action="${g.createLink(uri:"/j_security_check")}" method="post" class="form " role="form">
                <div class="card" data-background="color" data-color="blue">
                  <div class="card-header">
                    <h3 class="card-title">
                      <div class="logo">
                          <a href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}" title="Home">
                            <img src="${resource(dir: 'images', file: 'rundeck-full-logo-black.png')}" alt="Rundeck" style="height: 20px; width: auto;"/>
                          </a>
                          <g:set var="userDefinedLogo" value="${grailsApplication.config.rundeck?.gui?.logo}"/>
                          <g:if test="${userDefinedLogo}">
                            <g:set var="userAssetBase" value="/user-assets" />
                            <g:set var="safeUserLogo" value="${userDefinedLogo.toString().encodeAsSanitizedHTML()}" />
                            <div style="margin-top:2em">
                              <img src="${g.createLink(uri:userAssetBase+"/"+safeUserLogo)}">
                            </div>
                          </g:if>
                      </div>
                    </h3>
                  </div>
                  <div class="card-content">
                    <g:set var="loginhtml" value="${grailsApplication.config.rundeck?.gui?.login?.welcomeHtml ?: ''}"/>
                    <g:if test="${loginhtml}">
                      <div style="margin-bottom:2em;">
                        <span>
                          ${enc(sanitize:loginhtml)}
                        </span>
                      </div>
                    </g:if>
                    <!--SSO Login Feature-->
                    <g:if test="${grailsApplication.config.rundeck.sso.loginButton.enabled in [true,'true']}">
                          <div class="sso-login">
                              <div class='form-group'>
                                  <div class="sso-login-container">
                                  <g:if test="${grailsApplication.config.rundeck.sso.loginButton.image.enabled in [true,'true']}"><img src="${resource(dir: 'images', file: 'rundeck2-icon-16.png')}" alt="Rundeck" class="sso-login-img" /></g:if>
                                  <a class='sso-login-link' href='${grailsApplication.config.rundeck.sso.loginButton.url}'>${grailsApplication.config.rundeck.sso.loginButton.title}</a>
                                  </div>
                              </div>
                          </div>
                    </g:if>
                    <!--/SSO Login Feature-->
                    <g:showLocalLogin>
                    <g:set var="loginmsg" value="${grailsApplication.config.rundeck?.gui?.login?.welcome ?: g.message(code: 'gui.login.welcome', default: '')}"/>
                    <g:if test="${loginmsg}">
                      <div style="margin-bottom:2em;">
                        <span>
                          <h4 class="text-default">
                            <g:enc>${loginmsg}</g:enc>
                          </h4>
                        </span>
                      </div>
                    </g:if>
                    <div class="form-group">
                        <label for="login"><g:message code="user.login.username.label"/></label>
                        <input type="text" name="j_username" id="login" class="form-control input-no-border" autofocus="true"/>
                    </div>

                    <div class="form-group">
                        <label for="password"><g:message code="user.login.password.label"/></label>
                        <input type="password" name="j_password" id="password" class="form-control input-no-border"/>
                    </div>
                        <div class="card-footer text-center">
                            <button type="submit" id="btn-login" class="btn btn-fill btn-wd "><g:message code="user.login.login.button"/></button>
                        </div>
                    </g:showLocalLogin>
                  </div>
                  <div class="card-footer text-center">
                    <g:if test="${flash.loginerror}">
                      <div class="alert alert-danger">
                          <span><g:enc>${flash.loginerror}</g:enc></span>
                      </div>
                    </g:if>

                    <g:set var="footermessagehtml" value="${grailsApplication.config.rundeck?.gui?.login?.footerMessageHtml ?: ''}"/>
                    <g:if test="${footermessagehtml}">
                      <div>
                        <span>
                            ${enc(sanitize:footermessagehtml)}
                        </span>
                      </div>
                    </g:if>
                  </div>
              </form>
            </div>
          </div>
        </div>
        <g:set var="loginDisclaimer" value="${grailsApplication.config.rundeck?.gui?.login?.disclaimer ?: ''}"/>
        <g:if test="${loginDisclaimer}">
          <div class="row" style="margin-top:1em;">
            <div class="col-xs-12 col-sm-8 col-sm-offset-2 card">
              <div class="card-content">
                ${enc(sanitize:loginDisclaimer)}
              </div>
            </div>
          </div>
        </g:if>

      </div>
    </div>
    <g:render template="/common/footer"/>
  </div>
</div>
</body>
</html>
