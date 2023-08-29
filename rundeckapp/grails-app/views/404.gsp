%{--
- Copyright 2019 Rundeck, Inc. (https://www.rundeck.com)
-
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
- You may obtain a copy of the License at
-
- http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an "AS IS" BASIS,
- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
- See the License for the specific language governing permissions and
- limitations under the License.
--}%

<%@ page import="grails.util.Environment" contentType="text/html;charset=UTF-8" %>
<html>

<head>
  <title>
    <g:appTitle /> -
    <g:message code="request.error.notfound.title" />
  </title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link rel="SHORTCUT" href="${g.resource(dir: 'images', file: 'favicon-152.png')}" />
  <link rel="favicon" href="${g.resource(dir: 'images', file: 'favicon-152.png')}" />
  <link rel="shortcut icon" href="${g.resource(dir: 'images', file: g.appFavicon())}"/>
  <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}" />

  <g:if test="${Environment.isDevelopmentEnvironmentAvailable()}">
    <asset:javascript src="vendor/vue.js"/>
  </g:if>
  <g:else>
    <asset:javascript src="vendor/vue.min.js"/>
  </g:else>
  %{-- Core theme styles from ui-trellis --}%
  <g:loadEntryAssets entry="components/theme" />
  <g:loadEntryAssets entry="components/server-identity" />
  <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
  <asset:javascript src="vendor/jquery.js" />
  <g:render template="/common/css"/>
  <asset:javascript src="bootstrap.js" />
</head>

<body id="four-oh-four-page">
    <div class="four-oh-four">
      <div class="nav-bar">
        <a
          href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}">
          <img src="${resource(dir: 'images', file: 'rundeck-full-logo-white.png')}" alt="Rundeck"
            style="height: 20px; width: auto;" />
        </a>
      </div>
      <div style="padding-top:16vh;">
        <div>
          <div class="col-xs-12 col-sm-6 space-cat-container">
            <g:if test="${!grailsApplication.config.rundeck?.feature?.fourOhFour?.hideSpaceCat in [true, 'true']}">
              <asset:image src="spacecat/saucer-cat.png" class="img-responsive"
                alt="Space Cat" />
            </g:if>
          </div>
          <div class="col-xs-12 col-sm-6 four-oh-four-messaging">
            <h1>404</h1>
            <h2>
              <g:message code="request.error.notfound.title" />
            </h2>
            <g:if test="${!grailsApplication.config.rundeck?.feature?.fourOhFour?.hideSpaceCat in [true, 'true']}">
              <h3>"We must be purrr-fectly lost"</h3>
            </g:if>
            <div>
              <h4 class="text-danger ">
                <g:message code="page.notfound.message" />
              </h4>
              <h5>
                URI: <g:enc>${request.forwardURI}</g:enc>
              </h5>
            </div>
            <div>
              <a href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}"
                class="
                btn btn-lg return-button">Return to Rundeck</a>
            </div>
          </div>
        </div>
      </div>
      <g:render template="/common/footer" />
    </div>
<asset:deferredScripts/>
</body>

</html>
