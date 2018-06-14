<!DOCTYPE html>
<!--[if lt IE 7 ]> <html class="ie6"> <![endif]-->
<!--[if IE 7 ]>    <html class="ie7"> <![endif]-->
<!--[if IE 8 ]>    <html class="ie8"> <![endif]-->
<!--[if IE 9 ]>    <html class="ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en"><!--<![endif]-->
<head>
    <title>
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

<g:layoutTitle default="${g.appTitle()}"/>
    </title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="SHORTCUT" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="favicon" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="shortcut icon" href="${g.resource(dir: 'images', file: 'favicon.ico')}"/>
    <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>

    <!-- fontawesome -->
    <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.0.10/css/all.css" integrity="sha384-+d0P83n9kaQMCwj8F4RJB66tzIwOKmrdb46+porD/OvrJ+37WqIM7UoBtwHO6Nlg" crossorigin="anonymous">
    <!-- /fontawesome -->
    <!-- themify icons -->
    <!-- <asset:stylesheet  href="themify.css" /> -->
    <!-- /themify icons -->
    <asset:stylesheet href="bootstrap.min.css"/>
    <asset:stylesheet href="app.scss.css"/>
    <asset:stylesheet href="custom.less.css"/>
    <!-- <asset:stylesheet href="app.less.css"/> -->
    <!-- <asset:stylesheet href="rundeck1.css"/> -->
    <asset:stylesheet href="ansicolor.css"/>
    <asset:stylesheet href="github-markdown.css"/>
    <asset:stylesheet href="jquery-ui.css"/>
    <!--[if lt IE 9]>
    <asset:javascript src="respond.min.js"/>
    <![endif]-->
    <asset:javascript src="jquery.js"/>
    <asset:javascript src="jquery-ui.js"/>
    <asset:javascript src="jquery-ui-timepicker-addon.js"/>
    <asset:javascript src="bootstrap-all.js"/>
    <asset:javascript src="prototype-bundle.js"/>
    <asset:javascript src="application.js"/>
    <g:render template="/common/js"/>
    <g:render template="/common/css"/>

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
<body>
  <div class="wrapper">
    <div class="sidebar" data-background-color="black" data-active-color="danger">
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
              <span class="appTitle">
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
              </span>
          </a>
      </div>      
      <div class="sidebar-wrapper">
          <g:render template="/common/sidebar"/>
          <div class="sidebar-modal-backdrop"></div>
      </div>
    </div>
    <div class="main-panel">
      <div>
        <g:render template="/common/mainbar"/>
      </div>
      <div class="content">
          <g:layoutBody/>
      </div>
      <g:render template="/common/footer"/>
    </div>
  </div>
<!--
disable for now because profiler plugin is not compatible with grails 3.x
 < g:profilerOutput />
-->
<miniprofiler:javascript/>

<g:if test="${uiplugins && uipluginsPath && params.uiplugins!='false'}">
    <script type="text/javascript" defer>
        //call after gsp page has loaded javascript
        jQuery(function(){window.rundeckPage.onPageLoad();});
    </script>
</g:if>
</body>
</html>
