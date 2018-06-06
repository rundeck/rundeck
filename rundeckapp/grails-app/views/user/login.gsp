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
    <asset:stylesheet href="app.scss.css"/>
    <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
    <asset:javascript src="jquery.js"/>
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
  <div class="wrapper wrapper-full-page">
    <div class="full-page login-page">
    <!-- <div class="full-page login-page" data-color="" data-image="static/img/background/background-2.jpg"> -->
      <div class="content">
        <div class="container">
          <div class="row">
            <div class="col-md-4 col-sm-6 col-md-offset-4 col-sm-offset-3">
              <form action="j_security_check" method="post" class="form " role="form">
                <div class="card" data-background="color" data-color="blue">
                  <div class="card-header">
                    <h3 class="card-title">
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
                    </h3>
                  </div>
                  <div class="card-content">
                    <g:set var="loginhtml" value="${grailsApplication.config.rundeck?.gui?.login?.welcomeHtml ?: ''}"/>
                    <g:if test="${loginhtml}">
                      <div>
                        <span>
                          ${enc(sanitize:loginhtml)}
                        </span>
                      </div>
                    </g:if>
                    <g:set var="loginmsg" value="${grailsApplication.config.rundeck?.gui?.login?.welcome ?: g.message(code: 'gui.login.welcome', default: '')}"/>
                    <g:if test="${loginmsg}">
                      <div>
                        <span>
                          <h3 class="text-primary">
                            <g:enc>${loginmsg}</g:enc>
                          </h3>
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
                  </div>
                  <div class="card-footer text-center">
                    <g:if test="${flash.loginerror}">
                      <div class="alert alert-danger">
                          <span><g:enc>${flash.loginerror}</g:enc></span>
                      </div>
                    </g:if>
                    <button type="submit" class="btn btn-fill btn-wd "><g:message code="user.login.login.button"/></button>

                      <g:if test="${grailsApplication.config.rundeck.sso.loginButton.enabled?.asBoolean()}">
                          <div class='form-group'>
                              <a class='btn btn-default' href='${grailsApplication.config.rundeck.sso.loginButton.url}'>${grailsApplication.config.rundeck.sso.loginButton.title}</a>
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
      </div>
    </div>
    <g:render template="/common/footer"/>
  </div>
</div>
</body>
</html>
