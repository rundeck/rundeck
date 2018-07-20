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
<g:if test="${session[org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME]?.language=='de'}">
    <g:set var="customCss" value=".navbar-brand,.navbar-default{border-radius: 0 0 10px 10px; }"/>
</g:if>

<style type="text/css">

    /*
    If I turn this off, will it break things? ~ Jesse
    .nodedetail.server .nodedesc, .node_entry.server .nodedesc{

          width: ${enc(rawtext:appLogoW)};
          height: ${enc(rawtext:appLogoH)};
        vertical-align: baseline;
    }

    .nodedetail.server .nodedesc, .node_entry.server .nodedesc {
        background-image: url("${logoResource}");
        background-repeat: no-repeat;
    }

    @media
    only screen and (-webkit-min-device-pixel-ratio: 2),
    only screen and (   min--moz-device-pixel-ratio: 2),
    only screen and (     -o-min-device-pixel-ratio: 2/1),
    only screen and (        min-device-pixel-ratio: 2),
    only screen and (                min-resolution: 192dpi),
    only screen and (                min-resolution: 2dppx) {
    .nodedetail.server .nodedesc, .node_entry.server .nodedesc {
        background-image: url("${logoResourceHires}");
        background-size: ${ enc(rawtext:appLogoW) } ${ enc(rawtext:appLogoH) };
    }
    } */

    ${enc(rawtext:customCss)}
</style>
