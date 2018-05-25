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

<g:set var="appLogo"
       value="${grailsApplication.config.rundeck.gui.logo ?: g.message(code: 'main.app.logo')}"/>
<g:set var="appLogoHires"
       value="${grailsApplication.config.rundeck.gui.logoHires ?: g.message(code: 'main.app.logo.hires')}"/>
<g:set var="appLogoW"
       value="${grailsApplication.config.rundeck.gui.'logo-width' ?: g.message(code: 'main.app.logo.width')}"/>
<g:set var="appLogoH"
       value="${grailsApplication.config.rundeck.gui.'logo-height' ?: g.message(code: 'main.app.logo.height')}"/>
<g:if test="${session[org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME]?.language=='de'}">
    <g:set var="customCss" value=".navbar-brand,.navbar-default{border-radius: 0 0 10px 10px; }"/>
</g:if>

<style type="text/css">

    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc{
          width: ${enc(rawtext:appLogoW)};
          height: ${enc(rawtext:appLogoH)};
        vertical-align: baseline;
    }

    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc {
        background-image: url("${resource(dir: 'images', file: appLogo)}");
        background-repeat: no-repeat;
    }

    @media
    only screen and (-webkit-min-device-pixel-ratio: 2),
    only screen and (   min--moz-device-pixel-ratio: 2),
    only screen and (     -o-min-device-pixel-ratio: 2/1),
    only screen and (        min-device-pixel-ratio: 2),
    only screen and (                min-resolution: 192dpi),
    only screen and (                min-resolution: 2dppx) {
    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc {
        background-image: url("${resource(dir: 'images', file: appLogoHires)}");
        background-size: ${ enc(rawtext:appLogoW) } ${ enc(rawtext:appLogoH) };
    }
    }

    <g:if test="${grailsApplication.config.rundeck.gui.navbar.background}">
    .navbar-overrides {
        background: ${grailsApplication.config.rundeck.gui.navbar.background};
    }
    </g:if>

    ${enc(rawtext:customCss)}
</style>
