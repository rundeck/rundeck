<%@ page import="grails.util.Environment" %>
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
<!DOCTYPE html>
<!--[if lt IE 7 ]> <html class="ie6"> <![endif]-->
<!--[if IE 7 ]>    <html class="ie7"> <![endif]-->
<!--[if IE 8 ]>    <html class="ie8"> <![endif]-->
<!--[if IE 9 ]>    <html class="ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="${response.locale.language}"><!--<![endif]-->
<head>
    <g:if test="${Environment.isDevelopmentEnvironmentAvailable()}">
        <asset:javascript src="vue.js"/>
    </g:if>
    <g:else>
        <asset:javascript src="vue.min.js"/>
    </g:else>
    <title>
      <g:layoutTitle default="${g.appTitle()}"/>
    </title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="locale" content="${response.locale.toString()}"/>
    <link rel="SHORTCUT" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="favicon" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="shortcut icon" href="${g.resource(dir: 'images', file: 'favicon.ico')}"/>
    <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>

    <asset:stylesheet href="bootstrap.min.css"/>
    <asset:stylesheet href="perfect-scrollbar.css"/>
    <asset:stylesheet href="app.css"/>
    <asset:stylesheet href="ansicolor.css"/>
    <asset:stylesheet href="github-markdown.css"/>
    <asset:stylesheet href="jquery-ui.css"/>

    <asset:javascript src="umd-vue-component-loader.js" />
    <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
    <asset:javascript src="jquery.js"/>
    <asset:javascript src="jquery-ui.js"/>
    <asset:javascript src="jquery-ui-timepicker-addon.js"/>
    <asset:javascript src="perfect-scrollbar.js"/>
    <asset:javascript src="bootstrap-all.js"/>
    <g:set var="includePrototypeJs" value="${true}" scope="page"/>

    <g:ifPageProperty name="meta.skipPrototypeJs">
        <g:set var="includePrototypeJs" value="${false}" scope="page"/>
    </g:ifPageProperty>
    <g:if test="${includePrototypeJs}">
    <asset:javascript src="prototype-bundle.js"/>
    </g:if>
    <asset:javascript src="application.js"/>
    <asset:javascript src="details-element-polyfill.js"/>
    <g:render template="/common/js"/>
    <g:render template="/common/css"/>

    <!-- VUE JS REQUIREMENTS -->
    <asset:javascript src="static/vendor.js"/>
    <!-- /VUE JS REQUIREMENTS -->

    <!-- VUE CSS MODULES -->
    <asset:stylesheet href="static/css/components/motd.css"/>
    <asset:stylesheet href="static/css/components/tour.css"/>
    <g:if test="${grailsApplication.config.rundeck.communityNews.disabled.isEmpty() ||!grailsApplication.config.rundeck.communityNews.disabled in [false,'false']}">
      <asset:stylesheet href="static/css/components/community-news-notification.css"/>
    </g:if>
    <!-- /VUE CSS MODULES -->

    <script language="javascript">
        function oopsEmbeddedLogin() {
        <%
            if (g.pageProperty(name: 'meta.tabpage')) { %>
                document.location = '${createLink(controller:"menu",params:params+[page:g.pageProperty(name: 'meta.tabpage')])}';
            <%
            } else { %>
                document.location = '${createLink(controller:"menu")}';
            <%
            }
        %>
        }
    </script>
    <g:jsonToken id="web_ui_token" url="${request.forwardURI}"/>
    <g:ifPageProperty name="meta.tabpage">
        <g:set var="_metaTabPage" value="${g.pageProperty(name: 'meta.tabpage')}" scope="page"/>
    </g:ifPageProperty>

    <!-- Placeholder for additional assets in footer -->
    <g:ifPageProperty name="page.footScripts">
        <g:pageProperty name="page.footScripts" />
    </g:ifPageProperty>
    <!-- END footer assets -->

    <g:if test="${pageProperty(name:'meta.rssfeed')}">
        <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
            <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="${pageProperty(name:'meta.rssfeed')}"/>
        </g:ifServletContextAttribute>
    </g:if>

    <%--
      _sidebarClass is the variable container for
      if the sidebar should be open or closed on
      page render
    --%>
    <g:set var="_sidebarClass" value="" scope="page"/>

    <g:if test="${session.filterPref?.sidebarClosed && session.filterPref?.sidebarClosed == 'true'}">
      <g:set var="_sidebarClass" value="sidebar-mini" scope="page"/>
    </g:if>

    <asset:javascript src="global/rundeckui.js"/>
    <script type="text/javascript">
      window._rundeck = Object.assign(window._rundeck || {}, {
        rdBase: '${g.createLink(uri:"/",absolute:true)}',
        context: '${grailsApplication.config.server.contextPath}',
        apiVersion: '${com.dtolabs.rundeck.app.api.ApiVersions.API_CURRENT_VERSION}',
        language: '${response.locale?.language ?: request.locale?.language}',
        locale: '${response.locale?.toString() ?: request.locale?.toString()}',
        projectName: '${enc(js:project?:params.project)}',
        activeTour: '${session.filterPref?.activeTour}',
        activeTourStep: '${session.filterPref?.activeTourStep}',
        hideVersionUpdateNotification: '${session.filterPref?.hideVersionUpdateNotification}',
        feature: {
            betaExecOutputViewer: {enabled: ${grailsApplication.config.rundeck?.feature?.betaExecOutputViewer?.enabled}}
        }
      })
    </script>

    <g:jsonToken id="ui_token" url="${request.forwardURI}"/>
    <asset:javascript src="static/components/central.js"/>
    <g:if test="${uiplugins && uipluginsPath && params.uiplugins!='false'}">

        <g:embedJSON id="uipluginData" data="${[path       : uipluginsPath,
                                                lang       : org.springframework.web.servlet.support.RequestContextUtils.getLocale(request).toLanguageTag(),
                                                project    : params.project ?: request.project,
                                                baseUrl    : createLink(uri: "/plugin/file/UI", absolute: true),
                                                basei18nUrl: createLink(uri: "/plugin/i18n/UI", absolute: true),
                                                appBaseUrl : createLink(uri: "/", absolute: true),
        ]}"/>
        <g:if test="${uiplugins}">
            <asset:javascript src="global/uiplugins.js"/>
            <g:jsonToken id="uiplugin_tokens" url="${request.forwardURI}"/>
        </g:if>
        <g:each in="${uipluginsorder?:uiplugins?.keySet()?.sort()}" var="pluginname">
            <!-- BEGIN UI plugin scripts for ${pluginname} -->
            <g:each in="${uiplugins[pluginname].scripts}" var="scriptPath">
                <script src="${createLink(
                        controller: 'plugin',
                        action: 'pluginFile',
                        params: [service: 'UI', name: pluginname, path: scriptPath]
                )}" type="text/javascript"></script>
            </g:each>
            <!-- END UI Plugin scripts for ${pluginname} -->
        </g:each>

        <g:each in="${uipluginsorder?:uiplugins?.keySet()?.sort()}" var="pluginname">
            <!-- BEGIN UI plugin css for ${pluginname} -->
            <g:each in="${uiplugins[pluginname].styles}" var="scriptPath">
                <link rel="stylesheet" href="${createLink(
                        controller: 'plugin',
                        action: 'pluginFile',
                        params: [service: 'UI', name: pluginname, path: scriptPath]
                )}"/>
            </g:each>
            <!-- END UI Plugin css for ${pluginname} -->
        </g:each>

    </g:if>
    <g:layoutHead/>
</head>
<body class="${_sidebarClass}">
  <div class="wrapper">
    <div class="sidebar" data-background-color="black" data-active-color="white">

      <div class="logo">
          <a class="home" href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}" title="Home">
              <i class="rdicon app-logo"></i>
              <span class="appTitle"></span>
          </a>
          <%--
            Saved for review should we switch back to another UI for opening
            and closing the sidebar
            <div class="navbar-minimize">
              <button class="btn btn-sm btn-icon">
                <i class="fas fa-sign-out-alt fa-flip-horizontal"></i>
                <i class="fas fa-sign-in-alt"></i>
              </button>
            </div>
          --%>
          <div class="navbar-minimize">
            <a class="triangle">
              <i class="fas fa-chevron-right"></i>
              <i class="fas fa-chevron-left"></i>
            </a>
          </div>
      </div>
      <div class="sidebar-wrapper">
          <g:render template="/common/sidebar"/>
          <div class="sidebar-modal-backdrop"></div>
      </div>
    </div>
    <div class="main-panel" id="main-panel">

        <g:render template="/common/mainbar"/>

        <div class="vue-project-motd container-fluid">
            <motd :event-bus="EventBus" tab-page="${enc(attr:pageProperty(name:'meta.tabpage'))}" style="margin-top:15px"></motd>
        </div>

        <g:ifPageProperty name="page.subtitle">
            <nav id="subtitlebar" class="navbar navbar-default subtitlebar standard">
                <div class="container-fluid">
                    <div class="navbar-header">
                        <ul class="nav navbar-nav">
                            <li class="primarylink">
                                <a href="#">
                                    <g:pageProperty name="page.subtitle"/>
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            </nav>
        </g:ifPageProperty>
        <g:ifPageProperty name="page.subtitlesection">
            <nav id="subtitlebar" class=" subtitlebar has-content ${pageProperty(name: 'page.subtitlecss')}">

                <g:pageProperty name="page.subtitlesection"/>

            </nav>
        </g:ifPageProperty>
      <div class="content">


        <div id="layoutBody">
            <g:layoutBody/>
        </div>
      </div>
      <g:render template="/common/footer"/>
    </div>

  </div>

<g:if test="${uiplugins && uipluginsPath && params.uiplugins!='false'}">
    <script type="text/javascript" defer>
        //call after gsp page has loaded javascript
        jQuery(function(){window.rundeckPage.onPageLoad();});
    </script>
</g:if>

<!-- VUE JS MODULES -->
<asset:javascript src="static/components/motd.js"/>
<asset:javascript src="static/components/tour.js"/>
<g:if test="${grailsApplication.config.rundeck.communityNews.disabled.isEmpty() ||!grailsApplication.config.rundeck.communityNews.disabled in [false,'false']}">
  <asset:javascript src="static/components/community-news-notification.js"/>
</g:if>

<!-- /VUE JS MODULES -->
</body>
</html>
