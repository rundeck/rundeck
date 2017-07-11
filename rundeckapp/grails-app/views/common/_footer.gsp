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

<%@ page import="com.opensymphony.module.sitemesh.RequestConstants" %>

<g:ifServletContextAttributeExists attribute="CLUSTER_MODE_ENABLED">
    <g:ifServletContextAttribute attribute="CLUSTER_MODE_ENABLED" value="true">
        <g:if test="${grailsApplication.config.rundeck?.gui?.clusterIdentityInFooter in [true, 'true']}">

            <div class="row">
                <div class="col-sm-12">
                    <span class=" rundeck-server-uuid"
                          data-server-uuid="${enc(attr: servletContextAttribute(attribute: 'SERVER_UUID'))}"
                          data-server-name="${enc(attr: servletContextAttribute(attribute: 'FRAMEWORK_NODE'))}">
                    </span>
                </div>
            </div>
        </g:if>
    </g:ifServletContextAttribute>
</g:ifServletContextAttributeExists>


<g:set var="buildIdent" value="${servletContextAttribute(attribute: 'app.ident')}"/>
<g:set var="appId" value="${g.appTitle()}"/>


<div class="row row-space">
    <div class="col-sm-3  ">

    &copy; Copyright 2017 <a href="http://rundeck.com">Rundeck, Inc.</a>

    All rights reserved.
    </div>
    <div class="col-sm-6 text-center ">

        <g:link controller="menu" action="welcome" class="version link-bare">
            <g:appTitle/> ${buildIdent}
        </g:link>

        <span class="rundeck-version-identity"
              data-version-string="${enc(attr: buildIdent)}"
              data-version-date="${enc(attr: servletContextAttribute(attribute: 'version.date_short'))}"
              data-app-id="${enc(attr: appId)}"></span>
    </div>
    <div class="col-sm-3 text-right">

    <g:link controller="menu" action="licenses"><g:message code="licenses"/></g:link>
        &bull;
        <a href="${enc(attr: g.helpLinkUrl())}" class="help ">
            <g:message code="help"/>
        </a>
    </div>
</div>
