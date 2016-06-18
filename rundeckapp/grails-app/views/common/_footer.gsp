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


<g:set var="buildIdent" value="${grailsApplication.metadata['build.ident']}"/>
<g:set var="appId" value="${g.appTitle()}"/>
<g:set var="versionDisplay" value="inline"/>


<g:if test="${request.getAttribute(RequestConstants.PAGE)}">
    <g:ifPageProperty name='meta.tabpage'>
        <g:ifPageProperty name='meta.tabpage' equals='configure'>
            <g:set var="versionDisplay" value="block"/>
        </g:ifPageProperty>
    </g:ifPageProperty>
</g:if>

<div class="row row-space">
    <div class="col-sm-12  ">

        <g:if test="${versionDisplay != 'block'}">
            <g:link controller="menu" action="welcome" class="version link-bare">
                <g:appTitle/> ${buildIdent}
            </g:link>

            <span class="rundeck-version-identity" data-version-string="${enc(attr: buildIdent)}"
                  data-app-id="${enc(attr: appId)}"></span>
        </g:if>
        <g:if test="${versionDisplay == 'block'}">
            <g:link controller="menu" action="welcome"
                    class="rundeck-version-block link-bare"
                    data-version-string="${enc(attr: buildIdent)}"
                    data-app-id="${enc(attr: appId)}">
            </g:link>
        </g:if>
        
    </div>
</div>

<div class="row row-space">
    <div class="col-sm-12">

    &copy; Copyright 2016 <a href="http://simplifyops.com">
        <span style="color:red;">#Simplify</span>Ops</a>.

    All rights reserved.

    <g:link controller="menu" action="licenses">Licenses</g:link>

    </div>
</div>
