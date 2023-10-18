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
  %{-- Core theme styles from ui-trellis --}%
  <asset:stylesheet href="static/css/components/theme.css"/>

  <g:if test="${Environment.isDevelopmentEnvironmentAvailable()}">
    <asset:javascript src="vendor/vue.js"/>
  </g:if>
  <g:else>
    <asset:javascript src="vendor/vue.min.js"/>
  </g:else>
  <asset:javascript src="static/js/chunk-common.js"/>
  <asset:javascript src="static/js/chunk-vendors.js"/>
  <asset:javascript src="static/components/server-identity.js" asset-defer="true" />
  <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
  <asset:javascript src="vendor/jquery.js" />
  <g:render template="/common/css"/>
  <asset:javascript src="bootstrap.js" />
</head>

<g:if test="${!exception && request.getAttribute('javax.servlet.error.exception')}">
  <%
    exception=request.getAttribute('javax.servlet.error.exception').cause
  %>
</g:if>
<body id="four-oh-four-page">
    <div class="four-oh-four">
      <div style="padding-top:16vh;">
        <div>
          <div class="col-xs-12 col-sm-6 space-cat-container">
            <g:if test="${!grailsApplication.config.rundeck?.feature?.fourOhFour?.hideSpaceCat in [true, 'true']}">
              <asset:image src="spacecat/saucer-cat.png" class="img-responsive"
                alt="Space Cat" />
            </g:if>
          </div>
          <div class="col-xs-12 col-sm-6 four-oh-four-messaging">
            <h1>${response.getStatus()}</h1>
            <h3>There was an unexpected error</h3>
            <h4>Please check the service log for more details.</h4>
            <h4>At ${new Date()}</h4>
            <h5><g:enc>${exception?.message}</g:enc></h5>
            <div>
              <h5>
                URI: <g:enc>${request.forwardURI}</g:enc>
              </h5>
            </div>
            <div>
              <a href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}"
                class="
                btn btn-lg return-button">Return to Home Page</a>
            </div>
          </div>
        </div>
      </div>
      <g:render template="/common/footer" />
    </div>
<asset:deferredScripts/>
</body>

</html>
